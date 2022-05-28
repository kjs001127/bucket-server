package com.icemelon404.bucket.network.cluster.election.handler

import com.icemelon404.bucket.cluster.election.listener.ClusterEventListener
import com.icemelon404.bucket.network.cluster.election.Vote
import com.icemelon404.bucket.network.common.MessageHandler
import io.netty.channel.ChannelHandlerContext

class VoteMessageHandler(private val listener : ClusterEventListener): MessageHandler<Vote>(Vote::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: Vote) {
        listener.onVoteReceived(msg.term)
    }
}



