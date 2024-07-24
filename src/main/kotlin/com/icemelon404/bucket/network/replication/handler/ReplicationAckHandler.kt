package com.icemelon404.bucket.network.replication.handler

import com.icemelon404.bucket.network.replication.ReplicationAck
import com.icemelon404.bucket.network.replication.ReplicationData
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.Ack
import com.icemelon404.bucket.replication.ReplicationDataSender
import com.icemelon404.bucket.replication.ReplicationStrategy
import io.netty.channel.ChannelHandlerContext

class ReplicationAckHandler(
    val service: ReplicationStrategy
):  MessageHandler<ReplicationAck>(ReplicationAck::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationAck) {
        val sender = object : ReplicationDataSender {
            override fun sendData(seqNo: Long, data: ByteArray) {
                ctx?.writeAndFlush(ReplicationData(msg.replicationId, seqNo, data))
            }
        }
        service.onAck(Ack(instanceId = msg.instanceId, replicationId = msg.replicationId), sender)
    }

}

