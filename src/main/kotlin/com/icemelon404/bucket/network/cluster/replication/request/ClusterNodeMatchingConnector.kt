package com.icemelon404.bucket.network.cluster.replication.request

import com.icemelon404.bucket.adapter.ClusterAwareReplicationSource
import com.icemelon404.bucket.adapter.ClusterAwareReplicationSourceConnector
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.connection.ClusterNode

class ClusterNodeMatchingConnector(
    private val nodes: MutableCollection<ClusterNode>
) : ClusterAwareReplicationSourceConnector {

    override fun connect(address: InstanceAddress): ClusterAwareReplicationSource =
        nodes
            .firstOrNull { it.address == address }
            ?.let { ReplicationRequester(it) }
            ?: error("클러스터에 마스터 노드가 존재하지 않습니다: $address")

    fun addInstance(node: ClusterNode) = nodes.add(node)
}