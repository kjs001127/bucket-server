package com.icemelon404.bucket.cluster.core

import com.icemelon404.bucket.cluster.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ConsensusStateHandler (
    private val clusterEventListener: ClusterEventListener,
    private val executor: ScheduledExecutorService,
    private val term: Term,
    private val logIndex: ClusterLog,
) : ConsensusService {

    private lateinit var peers: Set<Peer>
    private lateinit var status: ConsensusState
    private val lock = ReentrantLock()


    fun start(peers: Set<Peer>) {
        this.peers = peers
        changeStatus(Follower(term, clusterEventListener, this, executor, logIndex))
    }

    override fun onHeartBeat(claim: LeaderHeartBeat) = lock.withLock {
        status.onHeartBeat(claim)
    }
    override fun onHeartBeatDeny(responseTerm: Long) = lock.withLock {
        status.onHeartBeatDeny(responseTerm)
    }

    override fun onRequestVote(voteRequest: VoteRequest) = lock.withLock {
        status.onRequestVote(voteRequest)
    }

    override fun onVoteReceived(voteTerm: Long) = lock.withLock { status.onVoteReceived(voteTerm) }

    fun toLeader() {
        changeStatus(Leader(term, peers, this, executor, clusterEventListener, logIndex))
    }

    fun toCandidate() {
        changeStatus(Candidate(term, logIndex, peers, this, clusterEventListener, executor))
    }

    fun toFollower() {
        changeStatus(Follower(term, clusterEventListener, this, executor, logIndex))
    }

    private fun changeStatus(status: ConsensusState) = lock.withLock {
        this.status = status
        status.onStart()
    }
}