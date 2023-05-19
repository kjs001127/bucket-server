package com.icemelon404.bucket.adapter.core

import com.icemelon404.bucket.adapter.*
import com.icemelon404.bucket.adapter.storage.FollowerLeaderStorage
import com.icemelon404.bucket.cluster.ClusterEventListener
import com.icemelon404.bucket.cluster.core.Term
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.common.logger
import com.icemelon404.bucket.replication.*
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.KeyValueStorage
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ReplicationStateHandler(
    private val storage: FollowerLeaderStorage,
    private val followerFactory: (masterAddr: InstanceAddress) -> ReplicationStatus,
    private val leaderFactory: (masterId: Long) -> ReplicationStatus
) : ClusterEventListener, ClusterAwareReplicationService, KeyValueStorage {

    private lateinit var term: Term
    private val lock = ReentrantLock()
    private lateinit var status: ReplicationStatus

    override fun onVotePending() = lock.withLock {
        logger().info { "replication state: idle" }

        newReplicationStatus(EmptyStatus())
        storage.setIdle()
    }

    override fun onElectedAsLeader(term: Long) = lock.withLock {
        logger().info { "replication state: leader" }

        newReplicationStatus(leaderFactory(term))
        storage.setLeader(term)
        this.term.value = term
    }

    override fun onLeaderFound(term: Long, masterAddress: InstanceAddress) = lock.withLock {
        logger().info { "replication state: follower" }

        newReplicationStatus(followerFactory(masterAddress))
        storage.setFollower(masterAddress)
        this.term.value = term
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

    override fun onData(replication: ClusterDataReplication) =
        lock.withLock {
            if (this.term.value != replication.term)
                return
            status.onData(replication.data)
        }


    override fun onRequest(context: ClusterReplicationContext, acceptor: ClusterReplicationAcceptor) =
        lock.withLock {
            if (this.term.value != context.term)
                return
            status.onRequest(context.context, ReplicationAcceptorAdapter(this.term.value, acceptor))
        }


    override fun onAccept(accept: ClusterReplicationAccept) =
        lock.withLock {
            if (this.term.value != accept.term)
                return
            status.onAccept(accept.data)
        }

    override fun write(keyValue: KeyValue) = lock.withLock {
        storage.write(keyValue)
    }

    override fun read(key: String) = storage.read(key)

    override fun clear() = lock.withLock {
        storage.clear()
    }
}

class EmptyStatus() : ReplicationStatus {

}