package com.icemelon404.bucket.replication

import com.icemelon404.bucket.common.InstanceAddress

interface ReplicationSource {
    val address: InstanceAddress
    fun requestReplication(info: FollowerInfo)
}

interface ReplicationSourceConnector {
    fun connect(address: InstanceAddress): ReplicationSource
}

data class FollowerInfo(val instanceId: String, val replicationId: Long, val lastMaster: VersionAndOffset)
