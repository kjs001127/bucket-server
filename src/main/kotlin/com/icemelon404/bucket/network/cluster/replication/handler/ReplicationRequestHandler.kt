package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.cluster.replication.ClusterAwareReplicationListener
import com.icemelon404.bucket.cluster.replication.ClusterReplicationAcceptor
import com.icemelon404.bucket.cluster.replication.ClusterReplicationContext
import com.icemelon404.bucket.cluster.replication.ClusterReplicationDataSender
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.replication.ReplicationAcceptRequest
import com.icemelon404.bucket.network.cluster.replication.ReplicationData
import com.icemelon404.bucket.network.cluster.replication.ReplicationRequest
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.api.IdAndOffset
import com.icemelon404.bucket.replication.api.ReplicationContext
import com.icemelon404.bucket.storage.KeyValue
import io.netty.channel.ChannelHandlerContext

class ReplicationRequestHandler(
    private val listener: ClusterAwareReplicationListener,
    private val storageAddress: InstanceAddress
) : MessageHandler<ReplicationRequest>(ReplicationRequest::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationRequest) {

        val sender = object : ClusterReplicationDataSender {
            override fun sendData(currentTerm: Long, keyValues: List<KeyValue>) {
                ctx?.writeAndFlush(ReplicationData(currentTerm, msg.replicationId, keyValues))
            }

        }
        val request = object : ClusterReplicationAcceptor {
            override fun accept(currentTerm: Long, masterInfo: IdAndOffset): ClusterReplicationDataSender {
                ctx?.writeAndFlush(ReplicationAcceptRequest(currentTerm, msg.replicationId, storageAddress, masterInfo))
                return sender;
            }
        }

        val rawContext = ReplicationContext(
            msg.replicationId,
            msg.instanceId,
            msg.lastMaster
        )

        listener.onRequest(ClusterReplicationContext(rawContext, msg.term), request)
    }
}