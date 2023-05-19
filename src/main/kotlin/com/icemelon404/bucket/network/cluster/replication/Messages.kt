package com.icemelon404.bucket.network.cluster.replication

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.VersionAndOffset
import com.icemelon404.bucket.storage.KeyValue

class ReplicationRequest(
    val term: Long,
    val instanceId: String,
    val replicationId: Long,
    val lastMaster: VersionAndOffset
)

class ReplicationData(
    val term: Long,
    val replicationId: Long,
    val seqNo: Long,
    val data: ByteArray,
)

class ReplicationAcceptRequest(
    val term: Long,
    val replicationId: Long,
    val masterAddress: InstanceAddress,
    val masterInfo: VersionAndOffset
)

class Redirect(
    val address: InstanceAddress
)