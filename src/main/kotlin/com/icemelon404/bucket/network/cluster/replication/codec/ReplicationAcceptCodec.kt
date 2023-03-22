package com.icemelon404.bucket.network.cluster.replication.codec

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.replication.ReplicationAcceptRequest
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.common.bufferSizeOf
import com.icemelon404.bucket.common.putString
import com.icemelon404.bucket.common.string
import com.icemelon404.bucket.replication.api.IdAndOffset
import java.nio.ByteBuffer

class ReplicationAcceptCodec(packetId: Int) : MessageCodec<ReplicationAcceptRequest>(ReplicationAcceptRequest::class, packetId) {

    override fun resolve(packet: Packet): ReplicationAcceptRequest {
        with(ByteBuffer.wrap(packet.body)) {
            val term = long
            val replicationId = long
            val masterId = long
            val masterOffset = long
            val masterIp = string
            val masterPort = int
            return ReplicationAcceptRequest(
                term,
                replicationId,
                InstanceAddress(masterIp, masterPort),
                IdAndOffset(masterId, masterOffset)
            )
        }
    }

    override fun serialize(msg: ReplicationAcceptRequest): ByteArray {
        return ByteBuffer.allocate(bufferSizeOf(msg.masterAddress.dest) + 36).apply {
            putLong(msg.term)
            putLong(msg.replicationId)
            putLong(msg.masterInfo.id)
            putLong(msg.masterInfo.offset)
            putString(msg.masterAddress.dest)
            putInt(msg.masterAddress.port)
        }.array()
    }
}