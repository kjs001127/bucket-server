package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.adaptable.storage.RedirectException
import com.icemelon404.bucket.network.cluster.replication.Redirect
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.network.storage.message.*
import com.icemelon404.bucket.network.storage.message.Set
import com.icemelon404.bucket.storage.KeyValueStorage
import io.netty.channel.ChannelHandlerContext

class RedirectGetHandler(
    private val storage: KeyValueStorage,
) : MessageHandler<Get>(Get::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: Get) {

        try {
            ctx?.writeAndFlush(Value(msg.requestId, storage.read(msg.key)))
        } catch (e : RedirectException) {
            ctx?.writeAndFlush(Redirect(e.to))
        } catch (e : Exception) {
            ctx?.writeAndFlush(Nack(msg.requestId))
        }
    }
}

class RedirectSetHandler(
    private val storage: KeyValueStorage,
) : MessageHandler<Set>(Set::class) {
    override fun onMessage(ctx: ChannelHandlerContext?, msg: Set) {
        try {
            storage.write(msg.keyValue)
            ctx?.writeAndFlush(Ack(msg.requestId))
        } catch (e : RedirectException) {
            ctx?.writeAndFlush(Redirect(e.to))
        } catch (e : Exception) {
            ctx?.writeAndFlush(Nack(msg.requestId))
        }
    }
}
