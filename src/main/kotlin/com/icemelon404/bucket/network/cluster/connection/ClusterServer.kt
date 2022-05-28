package com.icemelon404.bucket.network.cluster.connection

import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.network.common.ByteToPacketCodec
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.PacketToMessageCodec
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel

class ClusterServer(
    private val codecs: List<MessageCodec<*>>,
    private val handlers : List<MessageHandler<*>>,
    private val port : Int
) {

    fun start() {
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup(1)
        ServerBootstrap().apply {
            group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(initializer())
                .childOption(ChannelOption.SO_KEEPALIVE, true)
            bind(port).sync()
        }
    }

    private fun initializer() : ChannelInitializer<SocketChannel> =
        object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel?) {
                ch?.pipeline()?.apply {
                    addLast(ClusterExceptionHandler())
                    addLast(ByteToPacketCodec())
                    addLast(PacketToMessageCodec(codecs))
                    handlers.forEach { addLast(it) }
                }
            }
        }

}