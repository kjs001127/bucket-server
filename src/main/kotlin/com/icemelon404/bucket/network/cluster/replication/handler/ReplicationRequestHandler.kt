package com.icemelon404.bucket.network.cluster.replication.handler

import com.icemelon404.bucket.cluster.replication.ClusterReplicationRequest
import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.replication.ReplicationAccept
import com.icemelon404.bucket.network.cluster.replication.ReplicationData
import com.icemelon404.bucket.network.cluster.replication.ReplicationRequest
import com.icemelon404.bucket.network.common.MessageHandler
import com.icemelon404.bucket.replication.listener.ReplicationListener
import com.icemelon404.bucket.replication.listener.IdAndOffset
import com.icemelon404.bucket.storage.KeyValue
import io.netty.channel.ChannelHandlerContext
import kotlin.properties.Delegates

class ReplicationRequestHandler(
    private val listener: ReplicationListener,
    private val storageAddress: InstanceAddress
): MessageHandler<ReplicationRequest>(ReplicationRequest::class) {

    override fun onMessage(ctx: ChannelHandlerContext?, msg: ReplicationRequest) {
        val request = object: ClusterReplicationRequest {
            override val term: Long
                get() = msg.term
            override var currentTerm by Delegates.notNull<Long>()
            override val replicationId: Long
                get() = msg.replicationId
            override val instanceId: String
                get() = msg.instanceId
            override val lastMaster: IdAndOffset
                get() = msg.lastMaster

            override fun accept(masterInfo: IdAndOffset) {
                ctx?.writeAndFlush(ReplicationAccept(currentTerm, msg.replicationId, storageAddress, masterInfo))
            }

            override fun sendData(content: List<KeyValue>) {
                ctx?.writeAndFlush(ReplicationData(currentTerm, msg.replicationId, content))
            }
        }
        listener.onReplicationRequest(request)
    }
}