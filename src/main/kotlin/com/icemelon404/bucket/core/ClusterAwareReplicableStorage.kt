package com.icemelon404.bucket.synchornize

import com.icemelon404.bucket.core.aof.withTry
import com.icemelon404.bucket.core.FollowerLeaderStorage
import com.icemelon404.bucket.cluster.ElectionEventListener
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.util.logger
import com.icemelon404.bucket.replication.*
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.KeyValueStorage
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

class ClusterAwareReplicableStorage(
    private val storage: FollowerLeaderStorage,
    private val newFollower: (masterAddr: InstanceAddress) -> ReplicationStatus,
    private val newLeader: (masterId: Long) -> ReplicationStatus,
) : ElectionEventListener, ReplicationStrategy, KeyValueStorage {

    private val storageLock = ReentrantReadWriteLock()
    private val serializer: Executor = Executors.newSingleThreadExecutor()
    private lateinit var status: ReplicationStatus

    override fun onVotePending() = serializer.execute {
        logger().info { "replication state: idle" }
        storageLock.write {
            newReplicationStatus(EmptyStatus())
            storage.setIdle()
        }
    }

    override fun onElectedAsLeader(term: Long) = serializer.execute {
        logger().info { "replication state: leader" }

        storageLock.write {
            newReplicationStatus(newLeader(term))
            storage.toLeader(term)
        }
    }

    override fun onLeaderFound(term: Long, leaderAddress: InstanceAddress) = serializer.execute {
        logger().info { "replication state: follower" }

        storageLock.write {
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

    override fun onData(replication: DataReplication) =
        serializer.execute {
            status.onData(replication)
        }


    override fun onRequest(request: ReplicationContext, accept: ReplicationAcceptor) =
        serializer.execute {
            status.onRequest(request, accept)
        }

    override fun onAck(ack: Ack, dataSender: ReplicationDataSender) =
        serializer.execute {
            status.onAck(ack, dataSender)
        }


    override fun onAccept(accept: ReplicationAccept, ack: ReplicationAckSender) =
        serializer.execute {
            status.onAccept(accept, ack)
        }

    override fun write(keyValue: KeyValue) =
        storageLock.readLock().withTry {
            storage.write(keyValue)
        }

    override fun read(key: String) =
        storageLock.readLock().withTry {
            storage.read(key)
        }

    override fun clear() = storageLock.readLock().withTry {
        storage.clear()
    }
}

class EmptyStatus() : ReplicationStatus {

}
