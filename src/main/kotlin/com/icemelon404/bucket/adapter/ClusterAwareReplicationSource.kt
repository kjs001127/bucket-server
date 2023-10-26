package com.icemelon404.bucket.adapter

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.FollowerInfo

interface ClusterAwareReplicationSourceConnector {
    fun connect(address: InstanceAddress): ClusterAwareReplicationSource
}

interface ClusterAwareReplicationSource {
    val address: InstanceAddress
    fun requestReplication(info: ClusterFollowerInfo)
}

class ClusterFollowerInfo(val term: Long, val info: FollowerInfo)