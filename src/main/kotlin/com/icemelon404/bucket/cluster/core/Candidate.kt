package com.icemelon404.bucket.cluster.core

import com.icemelon404.bucket.cluster.*
import com.icemelon404.bucket.common.logger
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Candidate(
    private val term: Term,
    private val logIndex: ClusterLog,
    private val peers: Set<Peer>,
    private val transition: ConsensusStateHandler,
    private val clusterEventListener: ClusterEventListener,
    private val executor: ScheduledExecutorService
) : ConsensusState {
    private var receivedVoteCnt: Int = 0
    private lateinit var requestVoteJob: Future<*>
    private val majority: Int
        get() = ((peers.size+1)/2) + 1
    private val lock = ReentrantLock()

    override fun onStart() {
        logger().info { "Transition to candidate state" }
        clusterEventListener.onVotePending()
        startPeriodicVoteRequest()
    }

    private fun startPeriodicVoteRequest() {
        val latch = CountDownLatch(1)
        requestVoteJob = executor.scheduleWithFixedDelay({
            latch.await()
            lock.withLock {
                if (requestVoteJob.isCancelled)
                    return@scheduleWithFixedDelay
                term.value++
                receivedVoteCnt = 1
                peers.forEach {
                    try {
                        it.requestVote(term.value, logIndex)
                    } catch (e: Exception) {
                        logger().warn { "Vote request failed to ${it.address}" }
                    }
                }
            }

        }, 0, 500 + ThreadLocalRandom.current().nextLong(500), TimeUnit.MILLISECONDS)
        latch.countDown()
    }

    override fun onVoteReceived(voteTerm: Long) {
        lock.withLock {
            if (voteTerm == term.value) {
                receivedVoteCnt++
                logger().info { "Vote received: $receivedVoteCnt" }
            }
            if (receivedVoteCnt >= majority) {
                requestVoteJob.cancel(true)
                logger().info { "Majority vote received" }
                transition.toLeader()
            }
        }
    }


    override fun onRequestVote(voteRequest: VoteRequest) {
        lock.withLock {
            if (term.value < voteRequest.term) {
                term.value = voteRequest.term
                if (voteRequest.log < logIndex)
                    return
                voteRequest.vote()
                requestVoteJob.cancel(true)
                transition.toFollower()
                logger().info { "Voted incoming request" }
            }
        }
    }

    override fun onHeartBeat(claim: LeaderHeartBeat) {
        lock.withLock {
            if (term.value > claim.term) {
                logger().warn { "Leader with lower term: ${claim.term} detected. Denied leader" }
                claim.deny(term.value)
                return
            }
            logger().info { "Leader with term: ${claim.term} detected" }
            term.value = claim.term
            requestVoteJob.cancel(true)
            transition.toFollower()
        }
    }
}