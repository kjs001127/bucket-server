package com.icemelon404.bucket.replication

interface ReplicationStrategy {
    fun onData(replication: DataReplication) {}
    fun onRequest(request: ReplicationContext, accept: ReplicationAcceptor) {}
    fun onAccept(accept: ReplicationAccept, ack: ReplicationAckSender) {}
    fun onAck(ack: Ack, dataSender: ReplicationDataSender) {}
}

interface ReplicationAckSender {
    fun ack(replicationId: Long, instanceId: String)
}

interface ReplicationAcceptor {
    fun accept(masterInfo: VersionAndOffset)
}

interface ReplicationDataSender {
    fun sendData(seqNo: Long, data: ByteArray)
}



class Ack(val instanceId: String, val replicationId: Long)

class ReplicationAccept(val replicationId: Long, val dataInfo: VersionAndOffset)

class ReplicationContext(
    val replicationId: Long,
    val instanceId: String,
    val lastMaster: VersionAndOffset,
)

class DataReplication(
    val content: ByteArray,
    val seqNo: Long,
    val replicationId: Long
)

data class VersionAndOffset(val id: String, val offset: Long)




