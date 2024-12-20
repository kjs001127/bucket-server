package com.icemelon404.bucket.network.election.handler

import com.icemelon404.bucket.cluster.PeerEventListener
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.cluster.LeaderHeartBeat
import com.icemelon404.bucket.network.election.HeartBeat
import com.icemelon404.bucket.network.election.HeartBeatDeny
import com.icemelon404.bucket.network.common.MessageHandler
import io.netty.channel.ChannelHandlerContext

class HeartBeatHandler(
    val listener: PeerEventListener
) : MessageHandler<HeartBeat>(HeartBeat::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: HeartBeat) {
        listener.onHeartBeat(object : LeaderHeartBeat {
            override val instanceId : InstanceAddress
                get() = msg.address
            override val term: Long
                get() = msg.term

            override fun deny(term: Long) {
                ctx?.writeAndFlush(HeartBeatDeny(term))
            }
        })
    }
}

