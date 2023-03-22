package com.icemelon404.bucket.cluster.replication

import com.icemelon404.bucket.cluster.election.VoteListener
import com.icemelon404.bucket.cluster.election.Term
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.api.*
import com.icemelon404.bucket.replication.ReplicationLifecycle
import com.icemelon404.bucket.storage.KeyValue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ReplicationStatusMachine(
    private val term: Term,
    private val makeFollower: (masterAddr: InstanceAddress) -> ReplicationLifecycle,
    private val makeLeader: (masterId: Long) -> ReplicationLifecycle
) : VoteListener, ClusterAwareReplicationListener, StorageStatus {

    private val lock = ReentrantLock()
    private lateinit var status: ReplicationLifecycle

    override fun onVotePending() = lock.withLock {
        closeCurrentStatus()
        status = EmptyStatus()
        status.start()
    }


    override fun onElectedAsLeader(term: Long) = lock.withLock {
        newStatus(makeLeader(term))
    }

    override fun onLeaderFound(term: Long, masterAddress: InstanceAddress) = lock.withLock {
        newStatus(makeFollower(masterAddress))
    }

    private fun newStatus(new: ReplicationLifecycle) {
        closeCurrentStatus()
        status = new
        status.start()
    }

    private fun closeCurrentStatus() {
        if (this::status.isInitialized)
            this.status.close()
    }

    override fun onData(replication: ClusterDataReplication) {
        lock.withLock {
            if (this.term.value != replication.term)
                return
            status.onData(replication.data)
        }
    }

    override fun onRequest(context: ClusterReplicationContext, acceptor: ClusterReplicationAcceptor) {
        lock.withLock {
            if (this.term.value != context.term)
                return
            status.onRequest(context.context, ReplicationAcceptorAdapter(this.term.value, acceptor))
        }
    }

    override fun onAccept(accept: ClusterReplicationAccept) {
        lock.withLock {
            if (this.term.value != accept.term)
                return
            status.onAccept(accept.data)
        }
    }

    override fun check(): Status = lock.withLock {
        return this.status.check()
    }
}

interface ClusterAwareReplicationListener {
    fun onAccept(accept: ClusterReplicationAccept)
    fun onRequest(context: ClusterReplicationContext, acceptor: ClusterReplicationAcceptor)
    fun onData(data: ClusterDataReplication)
}

class ClusterReplicationAccept(
    val term: Long,
    val data: ReplicationAccept
)

class ClusterDataReplication(
    val term: Long,
    val data: DataReplication
)

class ClusterReplicationContext(
    val context: ReplicationContext,
    val term: Long
)

interface ClusterReplicationAcceptor {
    fun accept(currentTerm: Long, masterInfo: IdAndOffset): ClusterReplicationDataSender
}

interface ClusterReplicationDataSender {
    fun sendData(currentTerm: Long, keyValues: List<KeyValue>)
}

class ReplicationAcceptorAdapter(
    val term: Long,
    private val delegate: ClusterReplicationAcceptor
) : ReplicationAcceptor {
    override fun accept(masterInfo: IdAndOffset): ReplicationDataSender {
        return ReplicationDataSenderAdaptor(term, delegate.accept(term, masterInfo))
    }
}

class ReplicationDataSenderAdaptor(
    private val term: Long,
    private val delegate: ClusterReplicationDataSender
) : ReplicationDataSender {
    override fun sendData(data: List<KeyValue>) {
        delegate.sendData(term, data)
    }
}

class EmptyStatus() : ReplicationLifecycle {
    override fun check(): Status {
        return Status(readable = false, writable = false, null)
    }

}