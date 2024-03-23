package com.icemelon404.bucket.synchornize

import com.icemelon404.bucket.adaptable.aof.withTry
import com.icemelon404.bucket.adaptable.storage.FollowerLeaderStorage
import com.icemelon404.bucket.cluster.ClusterEventListener
import com.icemelon404.bucket.cluster.core.Term
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.common.logger
import com.icemelon404.bucket.replication.*
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.KeyValueStorage
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class EventSynchronizer(
    private val storage: FollowerLeaderStorage,
    private val newFollower: (masterAddr: InstanceAddress) -> ReplicationStatus,
    private val newLeader: (masterId: Long) -> ReplicationStatus,
) : ClusterEventListener, ReplicationService, KeyValueStorage {

    private val lock = ReentrantLock()
    private val storageLock = ReentrantLock()
    private val term: Term = Term(0)
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private lateinit var status: ReplicationStatus

    override fun onVotePending() = executor.execute {
        logger().info { "replication state: idle" }
        storageLock.withLock {
            newReplicationStatus(EmptyStatus())
            storage.setIdle()
        }
    }

    override fun onElectedAsLeader(term: Long) = executor.execute {
        logger().info { "replication state: leader" }

        storageLock.withLock {
            newReplicationStatus(newLeader(term))
            storage.toLeader(term)
            this.term.value = term
        }
    }

    override fun onLeaderFound(term: Long, leaderAddress: InstanceAddress) = executor.execute {
        logger().info { "replication state: follower" }

        storageLock.withLock {
            newReplicationStatus(newFollower(leaderAddress))
            storage.toFollower(leaderAddress)
            this.term.value = term
        }
    }

    private fun newReplicationStatus(new: ReplicationStatus) {
        closeCurrentStatus()
        status = new
        status.start()
    }

    private fun closeCurrentStatus() {
        if (this::status.isInitialized)
            this.status.close()
    }

    override fun onData(replication: DataReplication) = executor.execute {
        status.onData(replication)
    }


    override fun onRequest(request: ReplicationContext, accept: ReplicationAcceptor) =
        executor.execute {
            status.onRequest(request, accept)
        }


    override fun onAccept(accept: ReplicationAccept) =
        executor.execute {
            status.onAccept(accept)
        }

    override fun write(keyValue: KeyValue) = storageLock.withTry {
        storage.write(keyValue)
    }

    override fun read(key: String) = storageLock.withTry { storage.read(key) }

    override fun clear() = lock.withTry {
        storage.clear()
    }
}

class EmptyStatus() : ReplicationStatus {

}
