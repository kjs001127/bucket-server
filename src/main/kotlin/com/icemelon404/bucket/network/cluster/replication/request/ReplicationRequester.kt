package com.icemelon404.bucket.network.cluster.replication.request

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.connection.ClusterNode
import com.icemelon404.bucket.network.cluster.replication.ReplicationRequest
import com.icemelon404.bucket.replication.FollowerInfo
import com.icemelon404.bucket.replication.ReplicationSource

class ReplicationRequester(
    private val node: ClusterNode
): ReplicationSource {

    override val address: InstanceAddress
        get() = node.address

    override fun requestReplication(request: FollowerInfo) {
        node.write(
            ReplicationRequest(
                request.instanceId,
                request.replicationId,
                request.lastMaster
            )
        )
    }
}
