package com.icemelon404.bucket.replication.listener

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.KeyValueStorage

interface ReplicationListener: KeyValueStorage {
    fun onData(replication: DataReplication) {}
    fun onReplicationRequest(request: ReplicationRequest) {}
    fun onReplicationAccept(accept: ReplicationAccept) {}
}

open class ReplicationAccept(val replicationId: Long, val masterAddress: InstanceAddress, val dataInfo: IdAndOffset)

interface ReplicationRequest {
    val replicationId: Long
    val instanceId: String
    val lastMaster: IdAndOffset
    fun sendData(data: List<KeyValue>)
    fun accept(masterInfo: IdAndOffset)
}

open class DataReplication(
    val content: List<KeyValue>,
    val replicationId: Long
)

data class IdAndOffset(val id: Long, val offset: Long)




