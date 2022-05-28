package com.icemelon404.bucket.network.cluster.replication.codec

import com.icemelon404.bucket.network.cluster.replication.ReplicationRequest
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.network.util.bufferSize
import com.icemelon404.bucket.network.util.putString
import com.icemelon404.bucket.network.util.string
import com.icemelon404.bucket.replication.listener.IdAndOffset
import java.nio.ByteBuffer

class ReplicationRequestCodec(packetId: Int) : MessageCodec<ReplicationRequest>(ReplicationRequest::class, packetId) {


    override fun resolve(packet: Packet): ReplicationRequest {
        with(ByteBuffer.wrap(packet.body)) {
            val term = long
            val replicationId = long
            val instanceId = string
            val lastMasterId = string
            val lastMasterOffset = long
            return ReplicationRequest(term, instanceId, replicationId, IdAndOffset(lastMasterId, lastMasterOffset))
        }
    }

    override fun serialize(msg: ReplicationRequest): ByteArray {
        return ByteBuffer.allocate(bufferSize(msg.lastMaster.id)+ bufferSize(msg.instanceId) + 24).apply {
            putLong(msg.term)
            putLong(msg.replicationId)
            putString(msg.instanceId)
            putString(msg.lastMaster.id)
            putLong(msg.lastMaster.offset)
        }.array()
    }
}