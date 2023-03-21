package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.network.cluster.replication.Redirect
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.network.storage.message.*
import com.icemelon404.bucket.network.storage.message.Set
import com.icemelon404.bucket.replication.RedirectException
import com.icemelon404.bucket.replication.listener.ReplicationListener
import com.icemelon404.bucket.replication.listener.StorageStatus
import com.icemelon404.bucket.storage.KeyValueStorage
import io.netty.channel.ChannelHandlerContext

class RedirectGetHandler(
    private val storage: KeyValueStorage,
    private val status: StorageStatus,
) : MessageHandler<Get>(Get::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: Get) {
        val currentStatus = this.status.check()

        if (currentStatus.readable) {
            ctx?.writeAndFlush(Value(msg.requestId, storage.read(msg.key)))
        } else if (currentStatus.shouldRedirect) {
            ctx?.writeAndFlush(Redirect(currentStatus.redirectAddress))
        } else {
            ctx?.writeAndFlush(Nack(msg.requestId))
        }
    }
}

class RedirectSetHandler(
    private val storage: KeyValueStorage,
    private val status: StorageStatus,
) : MessageHandler<Set>(Set::class) {
    override fun onMessage(ctx: ChannelHandlerContext?, msg: Set) {
        val currentStatus = this.status.check()

        if (currentStatus.writable) {
            storage.write(msg.keyValue)
            ctx?.writeAndFlush(Ack(msg.requestId))
        } else if (currentStatus.shouldRedirect) {
            ctx?.writeAndFlush(Redirect(currentStatus.redirectAddress))
        } else {
            ctx?.writeAndFlush(Nack(msg.requestId))
        }
    }
}
