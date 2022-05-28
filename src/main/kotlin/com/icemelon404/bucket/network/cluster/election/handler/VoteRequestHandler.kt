package com.icemelon404.bucket.network.cluster.election.handler

import com.icemelon404.bucket.cluster.election.listener.ClusterEventListener
import com.icemelon404.bucket.cluster.election.listener.RequestVote
import com.icemelon404.bucket.network.cluster.election.Vote
import com.icemelon404.bucket.network.cluster.election.VoteRequest
import com.icemelon404.bucket.network.common.MessageHandler
import io.netty.channel.ChannelHandlerContext

class VoteRequestHandler(private val listener : ClusterEventListener) : MessageHandler<VoteRequest>(VoteRequest::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: VoteRequest) {
        listener.onRequestVote(object : RequestVote {
            override val term: Long
                get() = msg.term

            override fun vote() {
                ctx?.writeAndFlush(Vote(term))
            }
        })
    }
}
