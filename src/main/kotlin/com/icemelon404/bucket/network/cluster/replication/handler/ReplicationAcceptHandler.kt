package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.network.cluster.replication.ReplicationAcceptRequest
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.ReplicationAccept
import com.icemelon404.bucket.replication.ReplicationService
import io.netty.channel.ChannelHandlerContext

class ReplicationAcceptHandler(
    private val listener: ReplicationService
): MessageHandler<ReplicationAcceptRequest>(ReplicationAcceptRequest::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationAcceptRequest) {
        listener.onAccept(ReplicationAccept(msg.replicationId, msg.masterInfo))
    }
}
