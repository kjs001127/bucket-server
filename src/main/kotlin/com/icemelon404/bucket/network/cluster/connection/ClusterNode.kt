package com.icemelon404.bucket.network.cluster.connection

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.network.common.ByteToPacketCodec
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.PacketToMessageCodec
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.util.concurrent.TimeUnit

class ClusterNode(
    val address: InstanceAddress,
    private val codecs: List<MessageCodec<*>>,
    private val handlers: List<MessageHandler<*>>,
) {

    @Volatile
    private lateinit var channel: Channel
    private val bootstrap: Bootstrap

    init {
        val loopGroup = NioEventLoopGroup()
        bootstrap = Bootstrap().apply {
            group(loopGroup)
            channel(NioSocketChannel::class.java)
            option(ChannelOption.SO_KEEPALIVE, true)
            handler(initializer())
        }
    }


    fun connect() {
        channel = bootstrap.connect(address.dest, address.port).channel()
        channel.closeFuture().addListener {
            channel.eventLoop().schedule(::connect, 1000, TimeUnit.MILLISECONDS)
        }
    }

    private fun initializer(): ChannelInitializer<SocketChannel> =
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

    fun write(obj: Any) {
        channel.writeAndFlush(obj)
    }

}