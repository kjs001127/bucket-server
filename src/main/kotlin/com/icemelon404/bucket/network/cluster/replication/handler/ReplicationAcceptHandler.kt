package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.adapter.ClusterAwareReplicationListener
import com.icemelon404.bucket.adapter.ClusterReplicationAccept
import com.icemelon404.bucket.network.cluster.replication.ReplicationAcceptRequest
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.api.ReplicationAccept
import io.netty.channel.ChannelHandlerContext

class ReplicationAcceptHandler(
    private val listener: ClusterAwareReplicationListener
): MessageHandler<ReplicationAcceptRequest>(ReplicationAcceptRequest::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationAcceptRequest) {
        listener.onAccept(
            ClusterReplicationAccept(
                msg.term,
                ReplicationAccept(msg.replicationId, msg.masterInfo)
            )
        )
    }
}