package com.icemelon404.bucket.network.election.handler

import com.icemelon404.bucket.cluster.ElectionService
import com.icemelon404.bucket.network.election.Vote
import com.icemelon404.bucket.network.common.MessageHandler
import io.netty.channel.ChannelHandlerContext

class VoteMessageHandler(private val listener : ElectionService): MessageHandler<Vote>(Vote::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: Vote) {
        listener.onVoteReceived(msg.term)
    }
}



