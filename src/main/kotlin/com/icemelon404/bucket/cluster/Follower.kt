package com.icemelon404.bucket.cluster

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.cluster.api.LeaderHeartBeat
import com.icemelon404.bucket.cluster.api.RequestVote
import com.icemelon404.bucket.common.logger
import java.util.concurrent.*

class Follower(
    private val term: Term,
    private val voteListener: VoteListener,
    private val transition: ClusterStateMachine,
    private val executor: ScheduledExecutorService,
    private val logIndex: LogIndex
) : InstanceStatus {

    @Volatile
    private var electionTimeout = System.currentTimeMillis()
    private lateinit var checkTimeoutJob: Future<*>
    private var leaderAddress: InstanceAddress? = null


    override fun onStart() {
        logger().info { "Transition to follower state" }
        voteListener.onVotePending()
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
        claim.instanceId.let {
            if (leaderAddress != it) {
                logger().info { "Leader changed to $it" }
                leaderAddress = it
                voteListener.onLeaderFound(term.value, it)
            }
        }
        refreshElectionTimeout()
    }

    override fun onRequestVote(request: RequestVote) {
        if (request.term > term.value) {
            term.value = request.term
            if (logIndex > request.logIndex)
                return
            logger().info { "Voted incoming request" }
            refreshElectionTimeout()
            voteListener.onVotePending()
            request.vote()
        }
    }

    private fun refreshElectionTimeout() {
        electionTimeout = (System.currentTimeMillis() + 5000 + ThreadLocalRandom.current().nextInt(1000))
    }

    private fun checkElectionTimeout() {
        val latch = CountDownLatch(1)
        checkTimeoutJob = executor.scheduleWithFixedDelay({
            latch.await()
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