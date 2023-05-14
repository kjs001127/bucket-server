package com.icemelon404.bucket.replication.api

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.storage.KeyValue

data class Status(
    val readable: Boolean,
    val writable: Boolean,
    val redirectAddress: InstanceAddress?,
)

interface StorageStatus {
    fun check(): Status
}

interface ReplicationListener {
    fun onData(replication: DataReplication) {}
    fun onRequest(request: ReplicationContext, accept: ReplicationAcceptor) {}
    fun onAccept(accept: ReplicationAccept) {}
}

class ReplicationAccept(val replicationId: Long, val dataInfo: IdAndOffset)

class ReplicationContext(
    val replicationId: Long,
    val instanceId: String,
    val lastMaster: IdAndOffset,
)

interface ReplicationAcceptor {
    fun accept(masterInfo: IdAndOffset): ReplicationDataSender
}

interface ReplicationDataSender {
    fun sendData(data: List<KeyValue>)
}

class DataReplication(
    val content: List<KeyValue>,
    val seqNo: Long,
    val replicationId: Long
)

data class IdAndOffset(val id: Long, val offset: Long)




