package com.icemelon404.bucket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.icemelon404.bucket.adapter.source.ReplicationSourceConnectorAdapter
import com.icemelon404.bucket.adapter.core.ReplicationStateHandler
import com.icemelon404.bucket.adapter.aof.AppendOnlyFile
import com.icemelon404.bucket.adapter.storage.FollowerStorage
import com.icemelon404.bucket.adapter.storage.*
import com.icemelon404.bucket.cluster.core.ConsensusStateHandler
import com.icemelon404.bucket.cluster.core.Term
import com.icemelon404.bucket.codec.SimpleKeyValueCodec
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.election.codec.DenyHeartBeatCodec
import com.icemelon404.bucket.network.cluster.election.codec.HeartBeatCodec
import com.icemelon404.bucket.network.cluster.election.codec.VoteCodec
import com.icemelon404.bucket.network.cluster.election.codec.VoteRequestCodec
import com.icemelon404.bucket.network.cluster.connection.ClusterNode
import com.icemelon404.bucket.network.cluster.replication.request.ClusterNodeMatchingConnector
import com.icemelon404.bucket.network.cluster.connection.ClusterServer
import com.icemelon404.bucket.network.cluster.election.handler.DenyHeartBeatHandler
import com.icemelon404.bucket.network.cluster.election.handler.HeartBeatHandler
import com.icemelon404.bucket.network.cluster.election.handler.VoteMessageHandler
import com.icemelon404.bucket.network.cluster.election.handler.VoteRequestHandler
import com.icemelon404.bucket.network.cluster.election.request.PeerRequester
import com.icemelon404.bucket.network.cluster.replication.codec.RedirectCodec
import com.icemelon404.bucket.network.cluster.replication.codec.ReplicationAcceptCodec
import com.icemelon404.bucket.network.cluster.replication.codec.ReplicationDataCodec
import com.icemelon404.bucket.network.cluster.replication.codec.ReplicationRequestCodec
import com.icemelon404.bucket.network.cluster.replication.handler.*
import com.icemelon404.bucket.network.storage.codec.*
import com.icemelon404.bucket.replication.core.Master
import com.icemelon404.bucket.replication.core.Slave
import com.icemelon404.bucket.replication.core.VersionOffsetManager
import com.icemelon404.bucket.storage.MemoryStorage
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.Executors
import kotlin.io.path.createDirectories


fun main(arr: Array<String>) {

    val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())
    val config = mapper.readValue(File(arr[0]), Config::class.java)

    val host = config.host
    val port = config.port


    val scheduler = Executors.newScheduledThreadPool(20)
    val clusterIp = InstanceAddress(host, config.clusterPort)
    val storageIp = InstanceAddress(host, port)

    Paths.get(config.dataPath).createDirectories()
    val aof = AppendOnlyFile(config.dataPath + File.separator + "aof")
    val storage = MemoryStorage()

    aof.load().forEach {
        storage.write(it.keyValue)
    }

    val followerStorage = FollowerStorage(aof, storage)
    val leaderStorage = LeaderStorage(aof, storage)

    val followerLeaderStorage = FollowerLeaderStorage(leaderStorage, followerStorage)

    val term = Term(0)
    val realConnector = ClusterNodeMatchingConnector(mutableSetOf())
    val connector = ReplicationSourceConnectorAdapter(realConnector, term)
    val versionManager = VersionOffsetManager()
    val followerBuilder = { masterAddress: InstanceAddress ->
        Slave(clusterIp.toString(), scheduler, versionManager, followerStorage, connector.connect(masterAddress))
    }
    val leaderBuilder = { logId: Long -> Master(scheduler, leaderStorage, leaderStorage, versionManager) }
    val replication = ReplicationStateHandler(followerLeaderStorage, followerBuilder, leaderBuilder)
    val cluster = ConsensusStateHandler(replication, scheduler, term, aof)

    val codecs = listOf(
        DenyHeartBeatCodec(1),
        HeartBeatCodec(2),
        VoteCodec(3),
        VoteRequestCodec(4),
        ReplicationDataCodec(5),
        ReplicationRequestCodec(6),
        ReplicationAcceptCodec(7)
    )

    val handlers = listOf(
        DenyHeartBeatHandler(cluster),
        HeartBeatHandler(cluster),
        VoteMessageHandler(cluster),
        VoteRequestHandler(cluster),
        ReplicationDataHandler(replication),
        ReplicationRequestHandler(replication, storageIp),
        ReplicationAcceptHandler(replication)
    )

    val nodes = config.clusterNodes.map { str ->
        str.split(":").let {
            InstanceAddress(it[0], it[1].toInt())
        }
    }.map {
        ClusterNode(it, codecs, handlers)
    }.toSet()

    val server = ClusterServer(codecs, handlers, clusterIp.port)

    val storageCodec = listOf(
        AckCodec(1),
        GetCodec(2),
        NackCodec(3),
        SetCodec(4, SimpleKeyValueCodec()),
        ValueCodec(5),
        RedirectCodec(6)
    )

    val storageHandler = listOf(
        RedirectSetHandler(replication),
        RedirectGetHandler(replication)
    )

    val storageServer = ClusterServer(storageCodec, storageHandler, storageIp.port)
    storageServer.start()
    nodes.forEach { realConnector.addInstance(it); it.connect() }
    cluster.start(nodes.map{ PeerRequester(it, clusterIp) }.toSet())
    server.start()
}