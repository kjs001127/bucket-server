package com.icemelon404.bucket.cluster.election

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.cluster.election.listener.LeaderHeartBeat
import com.icemelon404.bucket.cluster.election.listener.RequestVote
import com.icemelon404.bucket.common.logger
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Leader(
    private val term: Term,
    private val instances: Set<Instance>,
    private val transition: Transition,
    private val executor: ScheduledExecutorService,
    private val storage: Storage
) : InstanceStatus {

    private val health = ConcurrentHashMap<InstanceAddress, Health>()

    @Volatile
    private lateinit var heartBeatJob: List<Future<*>>

    @Volatile
    private lateinit var watchJob: Future<*>

    private val lock = ReentrantLock()
    private val majority: Int
        get() = ((instances.size + 1) / 2) + 1

    override fun onStart() {
        logger().info { "Transition to leader state" }
        storage.setLeader()
        instances.forEach { health[it.address] = Health.PENDING }
        runHeartBeat(term.value)
    }

    private fun runHeartBeat(term: Long) {
        val heartBeatLatch = CountDownLatch(1)
        heartBeatJob = instances.map {
            executor.scheduleWithFixedDelay({
                heartBeatLatch.await()
                try {
                    it.heartBeat(term)
                    health[it.address] = Health.ONLINE
                } catch (e: Exception) {
                    health[it.address] = Health.OFFLINE
                }

            }, 0, 200, TimeUnit.MILLISECONDS)
        }
        heartBeatLatch.countDown()

        val watchLatch = CountDownLatch(1)
        watchJob = executor.scheduleWithFixedDelay({
            watchLatch.await()
            if (isMajorityOffline()) {
                logger().warn { "Majority of node offline" }
                cancelHeartBeat()
                lock.withLock {
                    if (!watchJob.isCancelled)
                        transition.toFollower()
                }
                watchJob.cancel(true)
            }
        }, 0, 100, TimeUnit.MILLISECONDS)
        watchLatch.countDown()
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

    override fun onRequestVote(voteRequest: RequestVote) {
        if (voteRequest.term > term.value && toFollower(voteRequest.term))
            voteRequest.vote()
    }

    override fun onHeartBeatDeny(responseTerm: Long) {
        if (responseTerm > term.value)
            toFollower(responseTerm)
    }

    private fun toFollower(responseTerm: Long): Boolean {
        if (!lock.tryLock())
            return false
        logger().warn { "Leader with higher term: $responseTerm detected" }
        term.value = responseTerm
        cancelBackgroundJobs()
        transition.toFollower()
        lock.unlock()
        return true
    }
}

enum class Health {
    ONLINE, OFFLINE, PENDING
}