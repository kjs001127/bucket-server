package com.icemelon404.bucket.adapter.source

import com.icemelon404.bucket.adapter.ClusterAwareReplicationSourceConnector
import com.icemelon404.bucket.cluster.core.Term
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.core.ReplicationSourceConnector

class ReplicationSourceConnectorAdapter(
    private val delegate: ClusterAwareReplicationSourceConnector,
    private val term: Term
): ReplicationSourceConnector {

    override fun connect(address: InstanceAddress) =
        ReplicationSourceAdapter(term.value, delegate.connect(address))
}