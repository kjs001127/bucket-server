package com.icemelon404.bucket.network.cluster.election.request

import com.icemelon404.bucket.cluster.ClusterLog
import com.icemelon404.bucket.cluster.Peer
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.connection.ClusterNode
import com.icemelon404.bucket.network.cluster.election.HeartBeat
import com.icemelon404.bucket.network.cluster.election.VoteRequest

class PeerRequester(
    private val node: ClusterNode,
    private val serverAddress: InstanceAddress
): Peer {
    override val address: InstanceAddress
        get() = node.address

    override fun heartBeat(term: Long) {
        node.write(HeartBeat(term, serverAddress))
    }

    override fun requestVote(term: Long, index: ClusterLog) {
        val temAndOffset = index.termAndOffset
        node.write(VoteRequest(term, temAndOffset.term, temAndOffset.offset))
    }

}