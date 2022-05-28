package com.icemelon404.bucket.network.storage.handler

import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.network.storage.message.Ack
import com.icemelon404.bucket.network.storage.message.Get
import com.icemelon404.bucket.network.storage.message.Set
import com.icemelon404.bucket.network.storage.message.Value
import com.icemelon404.bucket.storage.KeyValueStorage
import io.netty.channel.ChannelHandlerContext

class GetHandler (
    private val storage: KeyValueStorage
): MessageHandler<Get>(Get::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: Get) {
        ctx?.writeAndFlush(Value(msg.requestId, storage.read(msg.key)))
    }
}

class SetHandler (
    private val storage: KeyValueStorage
): MessageHandler<Set>(Set::class) {
    override fun onMessage(ctx: ChannelHandlerContext?, msg: Set) {
        storage.write(msg.keyValue)
        ctx?.writeAndFlush(Ack(msg.requestId))
    }
}