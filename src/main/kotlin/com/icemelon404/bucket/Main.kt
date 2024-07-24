package com.icemelon404.bucket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.icemelon404.bucket.core.FollowerLeaderStorage
import com.icemelon404.bucket.core.aof.AppendOnlyFile
import com.icemelon404.bucket.core.storage.FollowerStorage
import com.icemelon404.bucket.core.storage.LeaderStorage
import com.icemelon404.bucket.cluster.core.ElectionStateHandler
import com.icemelon404.bucket.cluster.core.Term
import com.icemelon404.bucket.storage.codec.SimpleKeyValueCodec
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.election.codec.DenyHeartBeatCodec
import com.icemelon404.bucket.network.election.codec.HeartBeatCodec
import com.icemelon404.bucket.network.election.codec.VoteCodec
import com.icemelon404.bucket.network.election.codec.VoteRequestCodec
import com.icemelon404.bucket.network.connection.ClusterNode
import com.icemelon404.bucket.network.replication.requester.ClusterNodeMatchingConnector
import com.icemelon404.bucket.network.connection.ClusterServer
import com.icemelon404.bucket.network.election.handler.DenyHeartBeatHandler
import com.icemelon404.bucket.network.election.handler.HeartBeatHandler
import com.icemelon404.bucket.network.election.handler.VoteMessageHandler
import com.icemelon404.bucket.network.election.handler.VoteRequestHandler
import com.icemelon404.bucket.network.election.requester.PeerRequester
import com.icemelon404.bucket.network.replication.codec.*
import com.icemelon404.bucket.network.replication.handler.*
import com.icemelon404.bucket.network.storage.codec.*
import com.icemelon404.bucket.replication.core.Master
import com.icemelon404.bucket.replication.core.Slave
import com.icemelon404.bucket.replication.core.VersionOffsetRecorder
import com.icemelon404.bucket.storage.MemoryStorage
import com.icemelon404.bucket.synchornize.ClusterAwareReplicableStorage
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


    aof.loadAndFix { storage.write(it.keyValue) }

    val followerStorage = FollowerStorage(aof, storage)
    val leaderStorage = LeaderStorage(aof, storage)

    val followerLeaderStorage = FollowerLeaderStorage(leaderStorage, followerStorage)

    val term = Term(0)
    val connector = ClusterNodeMatchingConnector(mutableSetOf())
    val versionManager = VersionOffsetRecorder()
    val followerBuilder = { masterAddress: InstanceAddress ->
        Slave(clusterIp.toString(), scheduler, versionManager, followerStorage, connector.connect(masterAddress))
    }
    val leaderBuilder = { logId: Long -> Master(scheduler, leaderStorage, leaderStorage, versionManager) }
    val replication = ClusterAwareReplicableStorage(followerLeaderStorage, followerBuilder, leaderBuilder)
    val electionHandler = ElectionStateHandler(replication, scheduler, term, aof)

    val codecs = listOf(
        DenyHeartBeatCodec(1),
        HeartBeatCodec(2),
        VoteCodec(3),
        VoteRequestCodec(4),
        ReplicationDataCodec(5),
        ReplicationRequestCodec(6),
        ReplicationAcceptCodec(7),
        ReplicationAckCodec(8),
    )

    val handlers = listOf(
        DenyHeartBeatHandler(electionHandler),
        HeartBeatHandler(electionHandler),
        VoteMessageHandler(electionHandler),
        VoteRequestHandler(electionHandler),
        ReplicationDataHandler(replication),
        ReplicationRequestHandler(replication, storageIp),
        ReplicationAcceptHandler(replication),
        ReplicationAckHandler(replication)
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
    nodes.forEach { connector.addInstance(it); it.connect() }
    electionHandler.start(nodes.map{ PeerRequester(it, clusterIp) }.toSet())
    server.start()
}
