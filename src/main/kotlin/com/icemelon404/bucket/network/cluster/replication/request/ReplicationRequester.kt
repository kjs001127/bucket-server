package com.icemelon404.bucket.network.cluster.replication.request

import com.icemelon404.bucket.adapter.ClusterAwareReplicationSource
import com.icemelon404.bucket.adapter.ClusterFollowerInfo
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.connection.ClusterNode
import com.icemelon404.bucket.network.cluster.replication.ReplicationRequest

class ReplicationRequester(
    private val node: ClusterNode
): ClusterAwareReplicationSource {

    override val address: InstanceAddress
        get() = node.address

    override fun requestReplication(request: ClusterFollowerInfo) {
        node.write(
            ReplicationRequest(
                request.term,
                request.info.instanceId,
                request.info.replicationId,
                request.info.lastMaster
            )
        )
    }
}