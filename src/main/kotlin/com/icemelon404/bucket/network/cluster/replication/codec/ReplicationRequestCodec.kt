package com.icemelon404.bucket.network.cluster.replication.codec

import com.icemelon404.bucket.network.cluster.replication.ReplicationRequest
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.common.bufferSizeOf
import com.icemelon404.bucket.common.putString
import com.icemelon404.bucket.common.string
import com.icemelon404.bucket.replication.api.IdAndOffset
import java.nio.ByteBuffer

class ReplicationRequestCodec(packetId: Int) : MessageCodec<ReplicationRequest>(ReplicationRequest::class, packetId) {

    override fun resolve(packet: Packet): ReplicationRequest {
        with(ByteBuffer.wrap(packet.body)) {
            val term = long
            val replicationId = long
            val instanceId = string
            val lastMasterId = long
            val lastMasterOffset = long
            return ReplicationRequest(term, instanceId, replicationId, IdAndOffset(lastMasterId, lastMasterOffset))
        }
    }

    override fun serialize(msg: ReplicationRequest): ByteArray {
        return ByteBuffer.allocate(bufferSizeOf(msg.instanceId) + 32).apply {
            putLong(msg.term)
            putLong(msg.replicationId)
            putString(msg.instanceId)
            putLong(msg.lastMaster.id)
            putLong(msg.lastMaster.offset)
        }.array()
    }
}