package com.icemelon404.bucket.cluster.replication

import com.icemelon404.bucket.common.InstanceAddress

interface ClusterAwareReplicationSourceConnector {
    fun connect(address: InstanceAddress): ClusterAwareReplicationSource
}