package com.icemelon404.bucket.cluster.replication

import com.icemelon404.bucket.cluster.election.Storage
import com.icemelon404.bucket.cluster.election.Term
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.listener.*
import com.icemelon404.bucket.replication.ReplicationStatus
import com.icemelon404.bucket.storage.KeyValue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ReplicationStatusMachine(
    private val term: Term,
    private val makeFollower: (address: InstanceAddress) -> ReplicationStatus,
    private val makeLeader: () -> ReplicationStatus
) : Storage, ReplicationListener {

    private val lock = ReentrantLock()
    private lateinit var status: ReplicationStatus

    override fun disable() {
        lock.withLock {
            closeCurrentStatus()
            status = EmptyStatus()
            status.onStart()
        }
    }

    override fun setLeader() = lock.withLock {
        newStatus(makeLeader())
    }

    override fun setFollowerOf(masterAddress: InstanceAddress?) = lock.withLock {
        if (masterAddress == null)
            newStatus(EmptyStatus())
        else
            newStatus(makeFollower(masterAddress))
    }

    private fun newStatus(new: ReplicationStatus) {
        closeCurrentStatus()
        status = new
        status.onStart()
    }

    private fun closeCurrentStatus() {
        if (this::status.isInitialized)
            this.status.close()
    }

    override fun onData(replication: DataReplication) {
        replication as ClusterDataReplication
        lock.withLock {
            if (this.term.value != replication.term)
                return
            status.onData(replication)
        }
    }

    override fun onReplicationRequest(request: ReplicationRequest) {
        request as ClusterReplicationRequest
        lock.withLock {
            request.currentTerm = term.value
            if (this.term.value != request.term)
                return
            status.onReplicationRequest(request)
        }
    }

    override fun onReplicationAccept(accept: ReplicationAccept) {
        accept as ClusterReplicationAccept
        lock.withLock {
            if (this.term.value != accept.term)
                return
            status.onReplicationAccept(accept)
        }
    }

    override fun write(keyValue: KeyValue) = status.write(keyValue)

    override fun read(key: String): ByteArray? = status.read(key)
}

class ClusterReplicationAccept(
    val term: Long,
    masterAddr: InstanceAddress,
    replicationId: Long,
    masterInfo: IdAndOffset
): ReplicationAccept(replicationId, masterAddr, masterInfo)

class ClusterDataReplication(
    val term: Long,
    content: List<KeyValue>,
    replicationId: Long
) : DataReplication(content, replicationId)

interface ClusterReplicationRequest : ReplicationRequest {
    val term: Long
    var currentTerm: Long
}

class EmptyStatus : ReplicationStatus {
    override fun write(keyValue: KeyValue) = error("empty status")
    override fun read(key: String) = error("empty status")
}
