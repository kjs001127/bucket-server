package com.icemelon404.bucket.network.cluster.election.handler

import com.icemelon404.bucket.cluster.ClusterLog
import com.icemelon404.bucket.cluster.TermAndOffset
import com.icemelon404.bucket.cluster.ConsensusService
import com.icemelon404.bucket.cluster.VoteRequest
import com.icemelon404.bucket.network.cluster.election.Vote
import com.icemelon404.bucket.network.cluster.election.VoteRequest
import com.icemelon404.bucket.network.common.MessageHandler
import io.netty.channel.ChannelHandlerContext

class VoteRequestHandler(private val listener : ConsensusService) : MessageHandler<VoteRequest>(VoteRequest::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: VoteRequest) {
        listener.onRequestVote(object : com.icemelon404.bucket.cluster.VoteRequest {
            override val term: Long
                get() = msg.term
            override val log: ClusterLog
                get() = object: ClusterLog {
                    override val termAndOffset: TermAndOffset
                        get() = TermAndOffset(msg.logId, msg.logOffset)
                }

            override fun vote() {
                ctx?.writeAndFlush(Vote(term))
            }
        })
    }
}
