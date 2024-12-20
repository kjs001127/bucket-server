package com.icemelon404.bucket.core

import com.icemelon404.bucket.common.withTry
import com.icemelon404.bucket.cluster.ElectionEventListener
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.util.logger
import com.icemelon404.bucket.replication.*
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.concurrent.write

class ClusterAwareReplicableStorage(
    private val storage: FollowerLeaderStorage,
    private val newFollower: (masterAddr: InstanceAddress) -> ReplicationStatus,
    private val newLeader: (masterId: Long) -> ReplicationStatus,
) : ElectionEventListener, ReplicationStrategy, KeyValueStorage {

    private val statusLock = ReentrantLock()

    private lateinit var status: ReplicationStatus

    override fun onVotePending() {
        logger().info { "replication state: idle" }

        statusLock.withLock {
                newReplicationStatus(EmptyStatus())
                storage.setIdle()
        }
    }

    override fun onElectedAsLeader(term: Long) {
        logger().info { "replication state: leader" }

        statusLock.withLock {
                newReplicationStatus(newLeader(term))
                storage.toLeader(term)
        }
    }

    override fun onLeaderFound(term: Long, leaderAddress: InstanceAddress) {
        logger().info { "replication state: follower" }

        statusLock.withLock {

                newReplicationStatus(newFollower(leaderAddress))
                storage.toFollower(leaderAddress)

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

    override fun onData(replication: DataReplication) = statusLock.withTry {
        status.onData(replication)
    }

    override fun onRequest(request: ReplicationContext, accept: ReplicationAcceptor) = statusLock.withTry {
        status.onRequest(request, accept)
    }

    override fun onAck(ack: Ack, dataSender: ReplicationDataSender) = statusLock.withTry {
        status.onAck(ack, dataSender)
    }

    override fun onAccept(accept: ReplicationAccept, ack: ReplicationAckSender) = statusLock.withTry {
            status.onAccept(accept, ack)
    }

    override fun write(keyValue: KeyValue) = statusLock.withTry {
        storage.write(keyValue)
    }

    override fun read(key: String) = statusLock.withTry {
        storage.read(key)
    }

    override fun clear() = statusLock.withTry {
        storage.clear()
    }
}

class EmptyStatus() : ReplicationStatus {

}
