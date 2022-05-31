package com.icemelon404.bucket.cluster.election

import com.icemelon404.bucket.cluster.election.listener.ClusterEventListener
import com.icemelon404.bucket.cluster.election.listener.LeaderHeartBeat
import com.icemelon404.bucket.cluster.election.listener.RequestVote
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ClusterStateMachine(
    private val storage: Storage,
    private val executor: ScheduledExecutorService,
    private val term: Term,
    private val logIndex: AppendLogIndex
) : ClusterEventListener, Transition {

    private lateinit var instances: Set<Instance>
    private lateinit var status: InstanceStatus
    private val lock = ReentrantLock()


    fun start(instances: Set<Instance>) {
        this.instances = instances
        changeStatus(Follower(term, storage, this, executor, logIndex))
    }

    override fun onHeartBeat(claim: LeaderHeartBeat) {
        lock.lock()
        status.onHeartBeat(claim)
        lock.unlock()
    }

    override fun onHeartBeatDeny(responseTerm: Long) {
        lock.lock()
        status.onHeartBeatDeny(responseTerm)
        lock.unlock()
    }

    override fun onRequestVote(voteRequest: RequestVote) = lock.withLock {
        status.onRequestVote(voteRequest)
    }


    override fun onVoteReceived(voteTerm: Long) = lock.withLock { status.onVoteReceived(voteTerm) }

    override fun toLeader() {
        changeStatus(Leader(term, instances, this, executor, storage, logIndex))
    }

    override fun toCandidate() {
        changeStatus(Candidate(term, logIndex, instances, this, storage, executor))
    }

    override fun toFollower() {
        changeStatus(Follower(term, storage, this, executor, logIndex))
    }

    private fun changeStatus(status: InstanceStatus) {
        lock.lock()
        this.status = status
        status.onStart()
        lock.unlock()
    }
}