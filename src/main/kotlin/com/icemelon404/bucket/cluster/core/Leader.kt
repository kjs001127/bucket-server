package com.icemelon404.bucket.cluster.core

import com.icemelon404.bucket.cluster.*
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.common.withTry
import com.icemelon404.bucket.util.logger
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock

class Leader(
    private val term: Term,
    private val cluster: Set<Peer>,
    private val transition: ElectionStateHandler,
    private val executor: ScheduledExecutorService,
    private val electionEventListener: ElectionEventListener,
    private val log: Log
) : ElectionState {

    private val health = ConcurrentHashMap<InstanceAddress, Health>()

    @Volatile
    private lateinit var heartBeatJob: List<Future<*>>
    private var once: Boolean = false

    @Volatile
    private lateinit var watchJob: Future<*>

    private val lock = ReentrantLock()
    private val majority: Int
        get() = ((cluster.size + 1) / 2) + 1

    override fun onStart() {
        logger().info { "Transition to leader state" }
        electionEventListener.onElectedAsLeader(term.value)
        cluster.forEach { health[it.address] = Health.PENDING }
        runHeartBeat(term.value)
    }

    private fun runHeartBeat(term: Long) {
        heartBeatJob = cluster.map {
            executor.scheduleWithFixedDelay({
                try {
                    it.heartBeat(term)
                    health[it.address] = Health.ONLINE
                } catch (e: Exception) {
                    health[it.address] = Health.OFFLINE
                }

            }, 0, 200, TimeUnit.MILLISECONDS)
        }

        val latch = CountDownLatch(1)
        watchJob = this.executor.scheduleWithFixedDelay({
            latch.await()

            if (isMajorityOffline()) {
                logger().warn { "Majority of node offline" }
                cancelHeartBeat()
                if (!watchJob.isCancelled)
                    watchJob.cancel(true)
                toFollower(term)
            }
        }, 0, 100, TimeUnit.MILLISECONDS)
        latch.countDown()

    }

    private fun isMajorityOffline() = health.count { (_, status) -> status == Health.OFFLINE } >= majority

    private fun cancelBackgroundJobs() {
        cancelHeartBeat()
        cancelWatchJob()
    }

    private fun cancelWatchJob() {
        watchJob.cancel(true)
    }

    private fun cancelHeartBeat() {
        heartBeatJob.forEach { it.cancel(true) }
    }

    override fun onHeartBeat(claim: LeaderHeartBeat) {
        if (claim.term > term.value)
            toFollower(claim.term)
        else if (claim.term < term.value)
            claim.deny(term.value)
        else
            error("Term 이 같은 두 리더가 존재할 수 없습니다")
    }

    override fun onRequestVote(voteRequest: VoteRequest) {
        if (voteRequest.term > term.value) {
            term.value = voteRequest.term
            if (log > voteRequest.log)
                return
            if (toFollower(voteRequest.term))
                voteRequest.vote()
        }
    }

    override fun onHeartBeatDeny(responseTerm: Long) {
        if (responseTerm > term.value)
            toFollower(responseTerm)
    }

    private fun toFollower(responseTerm: Long): Boolean = lock.withTry {
        if (once) {
            return false
        }
        once = true
        term.value = responseTerm
        cancelBackgroundJobs()
        transition.toFollower()
        return true
    }
}

enum class Health {
    ONLINE, OFFLINE, PENDING
}
