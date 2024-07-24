package com.icemelon404.bucket.network.replication.handler

import com.icemelon404.bucket.network.replication.ReplicationData
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.DataReplication
import com.icemelon404.bucket.replication.ReplicationStrategy
import io.netty.channel.ChannelHandlerContext

class ReplicationDataHandler(
    private val listener: ReplicationStrategy
) : MessageHandler<ReplicationData>(ReplicationData::class) {
    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationData) {
        listener.onData(DataReplication( msg.data, msg.seqNo, msg.replicationId))
    }
}
