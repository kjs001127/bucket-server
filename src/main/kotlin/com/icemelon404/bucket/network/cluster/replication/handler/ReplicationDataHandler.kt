package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.cluster.replication.ClusterDataReplication
import com.icemelon404.bucket.network.cluster.replication.ReplicationData
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.listener.ReplicationListener
import io.netty.channel.ChannelHandlerContext

class ReplicationDataHandler(
    private val listener: ReplicationListener
) : MessageHandler<ReplicationData>(ReplicationData::class) {
    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationData) {
        listener.onData(ClusterDataReplication(msg.term, msg.data, msg.replicationId))
    }
}