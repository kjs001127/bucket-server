package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.network.cluster.replication.Redirect
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.network.storage.message.*
import com.icemelon404.bucket.network.storage.message.Set
import com.icemelon404.bucket.replication.RedirectException
import com.icemelon404.bucket.replication.listener.ReplicationListener
import io.netty.channel.ChannelHandlerContext

class RedirectGetHandler(
    private val listener: ReplicationListener
) : MessageHandler<Get>(Get::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: Get) {
        try {
            ctx?.writeAndFlush(Value(msg.requestId, listener.read(msg.key)))
        } catch (e: RedirectException) {
            ctx?.writeAndFlush(Redirect(e.dest))
        } catch (e: Exception) {
            ctx?.writeAndFlush(Nack(msg.requestId))
        }
    }
}

class RedirectSetHandler(
    private val listener: ReplicationListener
) : MessageHandler<Set>(Set::class) {
    override fun onMessage(ctx: ChannelHandlerContext?, msg: Set) {
        try {
            listener.write(msg.keyValue)
            ctx?.writeAndFlush(Ack(msg.requestId))
        } catch (e: RedirectException) {
            ctx?.writeAndFlush(Redirect(e.dest))
        } catch (e: Exception) {
            ctx?.writeAndFlush(Nack(msg.requestId))
        }
    }
}
