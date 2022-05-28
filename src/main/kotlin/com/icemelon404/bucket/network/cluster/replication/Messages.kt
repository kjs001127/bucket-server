package com.icemelon404.bucket.network.cluster.replication

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.listener.IdAndOffset
import com.icemelon404.bucket.storage.KeyValue

class ReplicationRequest(
    val term: Long,
    val instanceId: String,
    val replicationId: Long,
    val lastMaster: IdAndOffset
)

class ReplicationData(
    val term: Long,
    val replicationId: Long,
    val data: List<KeyValue>
)

class ReplicationAccept(
    val term: Long,
    val replicationId: Long,
    val masterAddress: InstanceAddress,
    val masterInfo: IdAndOffset
)

class Redirect(
    val address: InstanceAddress
)