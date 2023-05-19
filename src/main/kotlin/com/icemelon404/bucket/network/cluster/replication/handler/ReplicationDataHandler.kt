package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.adapter.core.ClusterAwareReplicationService
import com.icemelon404.bucket.adapter.core.ClusterDataReplication
import com.icemelon404.bucket.network.cluster.replication.ReplicationData
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.DataReplication
import io.netty.channel.ChannelHandlerContext

class ReplicationDataHandler(
    private val listener: ClusterAwareReplicationService
) : MessageHandler<ReplicationData>(ReplicationData::class) {
    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationData) {
        listener.onData(ClusterDataReplication(msg.term, DataReplication( msg.data, msg.seqNo, msg.replicationId)))
    }
}