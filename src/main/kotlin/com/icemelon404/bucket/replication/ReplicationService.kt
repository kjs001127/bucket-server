package com.icemelon404.bucket.replication

interface ReplicationService {
    fun onData(replication: DataReplication) {}
    fun onRequest(request: ReplicationContext, accept: ReplicationAcceptor) {}
    fun onAccept(accept: ReplicationAccept) {}
}

class ReplicationAccept(val replicationId: Long, val dataInfo: VersionAndOffset)

class ReplicationContext(
    val replicationId: Long,
    val instanceId: String,
    val lastMaster: VersionAndOffset,
)

interface ReplicationAcceptor {
    fun accept(masterInfo: VersionAndOffset): ReplicationDataSender
}

interface ReplicationDataSender {
    fun sendData(data: ByteArray)
}

class DataReplication(
    val content: ByteArray,
    val seqNo: Long,
    val replicationId: Long
)

data class VersionAndOffset(val id: String, val offset: Long)




