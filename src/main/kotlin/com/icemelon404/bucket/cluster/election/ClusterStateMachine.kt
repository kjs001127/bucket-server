package com.icemelon404.bucket.cluster.election

import com.icemelon404.bucket.cluster.election.listener.ClusterEventListener
import com.icemelon404.bucket.cluster.election.listener.LeaderHeartBeat
import com.icemelon404.bucket.cluster.election.listener.RequestVote
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.locks.ReentrantLock

class ClusterStateMachine(
    private val storage : Storage,
    private val executor : ScheduledExecutorService,
    private val term : Term
) : ClusterEventListener, Transition {

    private lateinit var instances : Set<Instance>
    private lateinit var status : InstanceStatus
    private val lock = ReentrantLock()


    fun start(instances : Set<Instance>) {
        this.instances = instances
        changeStatus(Follower(term, storage, this, executor))
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

    override fun onRequestVote(voteRequest: RequestVote) {
        lock.lock()
        status.onRequestVote(voteRequest)
        lock.unlock()
    }

    override fun onVoteReceived(voteTerm: Long) {
        lock.lock()
        status.onVoteReceived(voteTerm)
        lock.unlock()
    }

    override fun toLeader() {
        changeStatus(Leader(term, instances, this, executor, storage))
    }

    override fun toCandidate() {
        changeStatus(Candidate(term, instances, this, storage, executor))
    }

    override fun toFollower() {
       changeStatus(Follower(term, storage, this, executor))
    }

    private fun changeStatus(status : InstanceStatus) {
        lock.lock()
        this.status = status
        status.onStart()
        lock.unlock()
    }
}