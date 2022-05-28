package com.icemelon404.bucket.cluster.replication

import com.icemelon404.bucket.cluster.election.Term
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.replication.ReplicationSourceConnector

class ReplicationSourceConnectorAdapter(
    private val delegate: ClusterAwareReplicationSourceConnector,
    private val term: Term
): ReplicationSourceConnector {

    override fun connect(address: InstanceAddress) =
        ReplicationSourceAdapter(term.value, delegate.connect(address))
}