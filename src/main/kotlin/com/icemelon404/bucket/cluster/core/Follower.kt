package com.icemelon404.bucket.cluster.core

import com.icemelon404.bucket.cluster.ElectionEventListener
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.cluster.LeaderHeartBeat
import com.icemelon404.bucket.cluster.Log
import com.icemelon404.bucket.cluster.VoteRequest
import com.icemelon404.bucket.common.withTry
import com.icemelon404.bucket.util.logger
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock

class Follower(
    private val term: Term,
    private val electionEventListener: ElectionEventListener,
    private val transition: ElectionStateHandler,
    private val executor: ScheduledExecutorService,
    private val logIndex: Log
) : ElectionState {

    @Volatile
    private var electionTimeout = System.currentTimeMillis()
    private lateinit var checkTimeoutJob: Future<*>
    private var leaderAddress: InstanceAddress? = null

    override fun onStart() {
        logger().info { "Transition to follower state" }
        electionEventListener.onVotePending()
        refreshElectionTimeout()
        checkElectionTimeout()
    }

    override fun onHeartBeat(claim: LeaderHeartBeat) {
        if (term.value > claim.term) {
            logger().warn { "Leader with lower term: ${claim.term} detected. Denied leader" }
            claim.deny(term.value)
            return
        }

        term.value = claim.term
        refreshElectionTimeout()

        claim.instanceId.let {
            if (leaderAddress != it) {
                logger().info { "Leader changed to $it" }
                leaderAddress = it
                electionEventListener.onLeaderFound(term.value, it)
            }
        }
    }

    override fun onRequestVote(voteRequest: VoteRequest) {
        if (voteRequest.term > term.value) {
            term.value = voteRequest.term
            if (logIndex > voteRequest.log)
                return
            logger().info { "Voted incoming request" }
            refreshElectionTimeout()
            voteRequest.vote()
        }
    }

    private fun refreshElectionTimeout() {
        electionTimeout = (System.currentTimeMillis() + 5000 + ThreadLocalRandom.current().nextInt(1000))
    }

    private fun checkElectionTimeout() {
        val latch = CountDownLatch(1)
        checkTimeoutJob = executor.scheduleWithFixedDelay({
            latch.await()

                if (checkTimeoutJob.isCancelled) {
                    checkTimeoutJob.cancel(true)
                    return@scheduleWithFixedDelay
                }
                if (electionTimeout < System.currentTimeMillis()) {
                    logger().warn { "Election timeout" }
                    transition.toCandidate()
                    checkTimeoutJob.cancel(true)
                    return@scheduleWithFixedDelay
            }
        }, 200, 200, TimeUnit.MILLISECONDS)
        latch.countDown()
    }
}
