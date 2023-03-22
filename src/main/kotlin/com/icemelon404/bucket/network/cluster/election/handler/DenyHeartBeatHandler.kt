package com.icemelon404.bucket.network.cluster.election.handler

import com.icemelon404.bucket.cluster.election.api.ClusterEventListener
import com.icemelon404.bucket.network.cluster.election.HeartBeatDeny
import com.icemelon404.bucket.network.common.MessageHandler
import io.netty.channel.ChannelHandlerContext

class DenyHeartBeatHandler(private val listener: ClusterEventListener) : MessageHandler<HeartBeatDeny>(HeartBeatDeny::class) {
    override fun onMessage(ctx: ChannelHandlerContext?, msg: HeartBeatDeny) {
        listener.onHeartBeatDeny(msg.term)
    }
}
