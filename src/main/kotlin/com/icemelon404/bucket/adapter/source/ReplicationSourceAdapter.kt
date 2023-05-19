package com.icemelon404.bucket.adapter.source

import com.icemelon404.bucket.adapter.ClusterAwareReplicationSource
import com.icemelon404.bucket.adapter.ClusterFollowerInfo
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.core.FollowerInfo
import com.icemelon404.bucket.replication.core.ReplicationSource

class ReplicationSourceAdapter(
    private val term: Long,
    private val delegate: ClusterAwareReplicationSource
) : ReplicationSource {

    override val address: InstanceAddress
        get() = delegate.address

    override fun requestReplication(info: FollowerInfo)
     = delegate.requestReplication(ClusterFollowerInfo(term, info))

}