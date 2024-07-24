package com.icemelon404.bucket.network.replication

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.VersionAndOffset

class ReplicationRequest(
    val instanceId: String,
    val replicationId: Long,
    val lastMaster: VersionAndOffset
)

class ReplicationData(
    val replicationId: Long,
    val seqNo: Long,
    val data: ByteArray,
)

class ReplicationAcceptRequest(
    val replicationId: Long,
    val masterAddress: InstanceAddress,
    val masterInfo: VersionAndOffset
)

class ReplicationAck(
    val instanceId: String,
    val replicationId: Long,
)

class Redirect(
    val address: InstanceAddress
)
