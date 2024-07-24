package com.icemelon404.bucket.network.replication.requester

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.connection.ClusterNode
import com.icemelon404.bucket.network.replication.ReplicationRequest
import com.icemelon404.bucket.replication.FollowerInfo
import com.icemelon404.bucket.replication.ReplicationSource

class ReplicationRequester(
    private val node: ClusterNode
): ReplicationSource {

    override val address: InstanceAddress
        get() = node.address

    override fun requestReplication(info: FollowerInfo) {
        node.write(
            ReplicationRequest(
                info.instanceId,
                info.replicationId,
                info.lastMaster
            )
        )
    }
}
