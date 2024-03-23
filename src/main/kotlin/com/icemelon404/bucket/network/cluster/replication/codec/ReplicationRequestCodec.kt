package com.icemelon404.bucket.network.cluster.replication.codec

import com.icemelon404.bucket.network.cluster.replication.ReplicationRequest
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.common.sizeOfString
import com.icemelon404.bucket.common.putString
import com.icemelon404.bucket.common.string
import com.icemelon404.bucket.replication.VersionAndOffset
import java.nio.ByteBuffer

class ReplicationRequestCodec(packetId: Int) : MessageCodec<ReplicationRequest>(ReplicationRequest::class, packetId) {

    override fun resolve(packet: Packet): ReplicationRequest {
        with(ByteBuffer.wrap(packet.body)) {
            val replicationId = long
            val instanceId = string
            val lastMasterId = string
            val lastMasterOffset = long
            return ReplicationRequest(instanceId, replicationId, VersionAndOffset(lastMasterId, lastMasterOffset))
        }
    }

    override fun serialize(msg: ReplicationRequest): ByteArray {
        return ByteBuffer.allocate(sizeOfString(msg.instanceId) + sizeOfString(msg.lastMaster.id) + 24).apply {
            putLong(msg.replicationId)
            putString(msg.instanceId)
            putString(msg.lastMaster.id)
            putLong(msg.lastMaster.offset)
        }.array()
    }
}
