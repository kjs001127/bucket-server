package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.adapter.ClusterAwareReplicationService
import com.icemelon404.bucket.adapter.ClusterReplicationAcceptor
import com.icemelon404.bucket.adapter.ClusterReplicationContext
import com.icemelon404.bucket.adapter.ClusterReplicationDataSender
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.replication.ReplicationAcceptRequest
import com.icemelon404.bucket.network.cluster.replication.ReplicationData
import com.icemelon404.bucket.network.cluster.replication.ReplicationRequest
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.VersionAndOffset
import com.icemelon404.bucket.replication.ReplicationContext
import io.netty.channel.ChannelHandlerContext

class ReplicationRequestHandler(
    private val listener: ClusterAwareReplicationService,
    private val storageAddress: InstanceAddress
) : MessageHandler<ReplicationRequest>(ReplicationRequest::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationRequest) {

        val sender = object : ClusterReplicationDataSender {
            var seqNo: Long = 0
            override fun sendData(currentTerm: Long, data: ByteArray) {
                ctx?.writeAndFlush(ReplicationData(currentTerm, msg.replicationId, seqNo, data))
                seqNo++
            }
        }
        val request = object : ClusterReplicationAcceptor {
            override fun accept(currentTerm: Long, masterInfo: VersionAndOffset): ClusterReplicationDataSender {
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