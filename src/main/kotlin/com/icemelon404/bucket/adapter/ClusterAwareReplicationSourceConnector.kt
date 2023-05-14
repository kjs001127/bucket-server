package com.icemelon404.bucket.adapter

import com.icemelon404.bucket.common.InstanceAddress

interface ClusterAwareReplicationSourceConnector {
    fun connect(address: InstanceAddress): ClusterAwareReplicationSource
}