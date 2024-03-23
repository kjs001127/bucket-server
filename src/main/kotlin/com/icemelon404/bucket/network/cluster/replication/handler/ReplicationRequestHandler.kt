package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.replication.ReplicationAcceptRequest
import com.icemelon404.bucket.network.cluster.replication.ReplicationData
import com.icemelon404.bucket.network.cluster.replication.ReplicationRequest
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.*
import io.netty.channel.ChannelHandlerContext

class ReplicationRequestHandler(
    private val listener: ReplicationService,
    private val storageAddress: InstanceAddress
) : MessageHandler<ReplicationRequest>(ReplicationRequest::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationRequest) {

        val sender = object : ReplicationDataSender {
            var seqNo: Long = 0
            override fun sendData(data: ByteArray) {
                ctx?.writeAndFlush(ReplicationData(msg.replicationId, seqNo, data))
                seqNo++
            }
        }
        val request = object : ReplicationAcceptor {
            override fun accept(masterInfo: VersionAndOffset): ReplicationDataSender {
                ctx?.writeAndFlush(ReplicationAcceptRequest(msg.replicationId, storageAddress, masterInfo))
                return sender;
            }
        }

        val context = ReplicationContext(
            msg.replicationId,
            msg.instanceId,
            msg.lastMaster
        )

        listener.onRequest(context, request)
    }
}
