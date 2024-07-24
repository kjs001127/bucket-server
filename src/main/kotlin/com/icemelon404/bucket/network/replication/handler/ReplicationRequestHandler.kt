package com.icemelon404.bucket.network.replication.handler

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.replication.ReplicationAcceptRequest
import com.icemelon404.bucket.network.replication.ReplicationRequest
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.*
import io.netty.channel.ChannelHandlerContext

class ReplicationRequestHandler(
    private val listener: ReplicationStrategy,
    private val storageAddress: InstanceAddress
) : MessageHandler<ReplicationRequest>(ReplicationRequest::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationRequest) {

        val request = object : ReplicationAcceptor {
            override fun accept(masterInfo: VersionAndOffset) {
                ctx?.writeAndFlush(ReplicationAcceptRequest(msg.replicationId, storageAddress, masterInfo))
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
