package com.icemelon404.bucket.network.cluster.election.handler

import com.icemelon404.bucket.cluster.election.LogIndex
import com.icemelon404.bucket.cluster.election.TermAndOffset
import com.icemelon404.bucket.cluster.election.api.ClusterEventListener
import com.icemelon404.bucket.cluster.election.api.RequestVote
import com.icemelon404.bucket.network.cluster.election.Vote
import com.icemelon404.bucket.network.cluster.election.VoteRequest
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.api.IdAndOffset
import io.netty.channel.ChannelHandlerContext

class VoteRequestHandler(private val listener : ClusterEventListener) : MessageHandler<VoteRequest>(VoteRequest::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: VoteRequest) {
        listener.onRequestVote(object : RequestVote {
            override val term: Long
                get() = msg.term
            override val logIndex: LogIndex
                get() = object: LogIndex {
                    override val termAndOffset: TermAndOffset
                        get() = TermAndOffset(msg.logId, msg.logOffset)
                }

            override fun vote() {
                ctx?.writeAndFlush(Vote(term))
            }
        })
    }
}
