package com.icemelon404.bucket.network.cluster.election.handler

import com.icemelon404.bucket.cluster.ConsensusService
import com.icemelon404.bucket.network.cluster.election.HeartBeatDeny
import com.icemelon404.bucket.network.common.MessageHandler
import io.netty.channel.ChannelHandlerContext

class DenyHeartBeatHandler(private val listener: ConsensusService) : MessageHandler<HeartBeatDeny>(HeartBeatDeny::class) {
    override fun onMessage(ctx: ChannelHandlerContext?, msg: HeartBeatDeny) {
        listener.onHeartBeatDeny(msg.term)
    }
}
