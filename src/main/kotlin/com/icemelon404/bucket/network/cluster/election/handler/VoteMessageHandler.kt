package com.icemelon404.bucket.network.cluster.election.handler

import com.icemelon404.bucket.cluster.ConsensusService
import com.icemelon404.bucket.network.cluster.election.Vote
import com.icemelon404.bucket.network.common.MessageHandler
import io.netty.channel.ChannelHandlerContext

class VoteMessageHandler(private val listener : ConsensusService): MessageHandler<Vote>(Vote::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: Vote) {
        listener.onVoteReceived(msg.term)
    }
}



