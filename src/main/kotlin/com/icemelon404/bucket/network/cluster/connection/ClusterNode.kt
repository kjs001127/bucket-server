package com.icemelon404.bucket.network.cluster.connection

import com.icemelon404.bucket.adapter.ClusterAwareReplicationSource
import com.icemelon404.bucket.adapter.ClusterFollowerInfo
import com.icemelon404.bucket.cluster.election.Instance
import com.icemelon404.bucket.cluster.election.LogIndex
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.election.HeartBeat
import com.icemelon404.bucket.network.cluster.election.VoteRequest
import com.icemelon404.bucket.network.cluster.replication.ReplicationRequest
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
    override val address: InstanceAddress,
    private val codecs: List<MessageCodec<*>>,
    private val handlers: List<MessageHandler<*>>,
    private val serverAddress: InstanceAddress
) : Instance, ClusterAwareReplicationSource {

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
            channel.eventLoop().schedule(::connect, 2000, TimeUnit.MILLISECONDS)
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

    override fun heartBeat(term: Long) {
        channel.writeAndFlush(HeartBeat(term, serverAddress)).syncUninterruptibly()
    }

    override fun requestVote(term: Long, index: LogIndex) {
        var temAndOffset = index.termAndOffset
        channel.writeAndFlush(VoteRequest(term, temAndOffset.term, temAndOffset.offset))
    }

    override fun requestReplication(request: ClusterFollowerInfo) {
        channel.writeAndFlush(
            ReplicationRequest(
                request.term,
                request.info.instanceId,
                request.info.replicationId,
                request.info.lastMaster
            )
        )
    }
}