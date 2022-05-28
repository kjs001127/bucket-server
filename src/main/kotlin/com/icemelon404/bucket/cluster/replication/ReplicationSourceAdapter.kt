package com.icemelon404.bucket.cluster.replication

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.FollowerInfo
import com.icemelon404.bucket.replication.ReplicationSource

class ReplicationSourceAdapter(
    private val term: Long,
    private val delegate: ClusterAwareReplicationSource
) : ReplicationSource {

    override fun requestReplication(info: FollowerInfo)
     = delegate.requestReplication(ClusterFollowerInfo(term, info))

}