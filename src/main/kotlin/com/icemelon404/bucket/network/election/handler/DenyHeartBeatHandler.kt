package com.icemelon404.bucket.network.election.handler

import com.icemelon404.bucket.cluster.PeerEventListener
import com.icemelon404.bucket.network.election.HeartBeatDeny
import com.icemelon404.bucket.network.common.MessageHandler
import io.netty.channel.ChannelHandlerContext

class DenyHeartBeatHandler(private val listener: PeerEventListener) : MessageHandler<HeartBeatDeny>(HeartBeatDeny::class) {
    override fun onMessage(ctx: ChannelHandlerContext?, msg: HeartBeatDeny) {
        listener.onHeartBeatDeny(msg.term)
    }
}
