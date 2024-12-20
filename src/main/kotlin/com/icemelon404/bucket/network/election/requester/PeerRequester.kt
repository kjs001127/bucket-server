package com.icemelon404.bucket.network.election.requester

import com.icemelon404.bucket.cluster.Log
import com.icemelon404.bucket.cluster.Peer
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.connection.ClusterNode
import com.icemelon404.bucket.network.election.HeartBeat
import com.icemelon404.bucket.network.election.VoteRequest

class PeerRequester(
    private val node: ClusterNode,
    private val serverAddress: InstanceAddress
): Peer {
    override val address: InstanceAddress
        get() = node.address

    override fun heartBeat(term: Long) {
        node.write(HeartBeat(term, serverAddress))
    }

    override fun requestVote(term: Long, index: Log) {
        val temAndOffset = index.termAndOffset
        node.write(VoteRequest(term, temAndOffset.term, temAndOffset.offset))
    }

}
