package com.icemelon404.bucket.network.connection

import com.icemelon404.bucket.util.logger
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext

class ClusterExceptionHandler: ChannelHandler {
    override fun handlerAdded(ctx: ChannelHandlerContext?) {
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext?) {
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger().warn { "Channel Error on ${ctx?.channel()?.remoteAddress()}: ${cause?.message}" }
    }
}
