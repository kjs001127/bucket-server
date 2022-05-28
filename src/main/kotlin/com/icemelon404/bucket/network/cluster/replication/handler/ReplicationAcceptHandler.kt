package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.cluster.replication.ClusterReplicationAccept
import com.icemelon404.bucket.network.cluster.replication.ReplicationAccept
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.listener.ReplicationListener
import io.netty.channel.ChannelHandlerContext

class ReplicationAcceptHandler(
    private val listener: ReplicationListener
): MessageHandler<ReplicationAccept>(ReplicationAccept::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationAccept) {
        listener.onReplicationAccept(ClusterReplicationAccept(msg.term, msg.masterAddress, msg.replicationId, msg.masterInfo))
    }
}