package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.network.cluster.replication.ReplicationData
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.DataReplication
import com.icemelon404.bucket.replication.ReplicationService
import io.netty.channel.ChannelHandlerContext

class ReplicationDataHandler(
    private val listener: ReplicationService
) : MessageHandler<ReplicationData>(ReplicationData::class) {
    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationData) {
        listener.onData(DataReplication( msg.data, msg.seqNo, msg.replicationId))
    }
}
