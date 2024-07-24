package com.icemelon404.bucket.network.replication.handler

import com.icemelon404.bucket.network.replication.ReplicationAcceptRequest
import com.icemelon404.bucket.network.replication.ReplicationAck
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.ReplicationAccept
import com.icemelon404.bucket.replication.ReplicationAckSender
import com.icemelon404.bucket.replication.ReplicationStrategy
import io.netty.channel.ChannelHandlerContext

class ReplicationAcceptHandler(
    private val listener: ReplicationStrategy
): MessageHandler<ReplicationAcceptRequest>(ReplicationAcceptRequest::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationAcceptRequest) {
        val sender = object : ReplicationAckSender {
            override fun ack(replicationId: Long, instanceId: String) {
               ctx?.writeAndFlush(ReplicationAck(replicationId = replicationId, instanceId = instanceId))
            }
        }
        listener.onAccept(ReplicationAccept(msg.replicationId, msg.masterInfo), sender)
    }
}
