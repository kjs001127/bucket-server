package com.icemelon404.bucket.adapter.core

import com.icemelon404.bucket.adapter.*
import com.icemelon404.bucket.adapter.core.storage.aof.withTry
import com.icemelon404.bucket.adapter.core.storage.FollowerLeaderStorage
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
) : ClusterEventListener, ClusterAwareReplicationService, KeyValueStorage {

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

    override fun onData(data: ClusterDataReplication) = executor.execute {
        if (this.term.value != data.term)
            return@execute
        status.onData(data.data)
    }


    override fun onRequest(context: ClusterReplicationContext, acceptor: ClusterReplicationAcceptor) =
        executor.execute {
            if (this.term.value != context.term)
                return@execute
            status.onRequest(context.context, ReplicationAcceptorAdapter(this.term.value, acceptor))
        }


    override fun onAccept(accept: ClusterReplicationAccept) =
        executor.execute {
            if (this.term.value != accept.term)
                return@execute
            status.onAccept(accept.data)
        }

    override fun write(keyValue: KeyValue) = storageLock.withTry {
        storage.write(keyValue)
    }

    override fun read(key: String) = storageLock.withTry { storage.read(key) }

    override fun clear() = lock.withLock {
        storage.clear()
    }
}

class EmptyStatus() : ReplicationStatus {

}

class ReplicationAcceptorAdapter(
    val term: Long,
    private val delegate: ClusterReplicationAcceptor
) : ReplicationAcceptor {
    override fun accept(masterInfo: VersionAndOffset): ReplicationDataSender {
        return ReplicationDataSenderAdaptor(term, delegate.accept(term, masterInfo))
    }
}

class ReplicationDataSenderAdaptor(
    private val term: Long,
    private val delegate: ClusterReplicationDataSender
) : ReplicationDataSender {
    override fun sendData(data: ByteArray) {
        delegate.sendData(term, data)
    }
}