package com.icemelon404.bucket.network.common

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.lang.ClassCastException
import kotlin.reflect.KClass
@ChannelHandler.Sharable
abstract class MessageHandler<in T : Any>(private val clazz : KClass<T>): ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        try {
            onMessage(ctx, clazz.java.cast(msg))
        } catch (e : ClassCastException) {
            ctx?.fireChannelRead(msg)
        }
    }

    abstract fun onMessage(ctx: ChannelHandlerContext?, msg :T)
}