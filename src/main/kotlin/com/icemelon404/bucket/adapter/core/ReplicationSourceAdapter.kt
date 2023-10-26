package com.icemelon404.bucket.adapter.core

import com.icemelon404.bucket.adapter.ClusterAwareReplicationSource
import com.icemelon404.bucket.adapter.ClusterAwareReplicationSourceConnector
import com.icemelon404.bucket.adapter.ClusterFollowerInfo
import com.icemelon404.bucket.cluster.core.Term
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.FollowerInfo
import com.icemelon404.bucket.replication.ReplicationSource
import com.icemelon404.bucket.replication.ReplicationSourceConnector

class ReplicationSourceConnectorAdapter(
    private val delegate: ClusterAwareReplicationSourceConnector,
    private val term: Term
): ReplicationSourceConnector {

    override fun connect(address: InstanceAddress) =
        ReplicationSourceAdapter(term.value, delegate.connect(address))
}
class ReplicationSourceAdapter(
    private val term: Long,
    private val delegate: ClusterAwareReplicationSource
) : ReplicationSource {

    override val address: InstanceAddress
        get() = delegate.address

    override fun requestReplication(info: FollowerInfo)
            = delegate.requestReplication(ClusterFollowerInfo(term, info))
}