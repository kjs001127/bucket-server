package com.icemelon404.bucket.network.replication.codec

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.replication.ReplicationAcceptRequest
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.common.sizeOfString
import com.icemelon404.bucket.common.putString
import com.icemelon404.bucket.common.string
import com.icemelon404.bucket.replication.VersionAndOffset
import java.nio.ByteBuffer

class ReplicationAcceptCodec(packetId: Int) :
    MessageCodec<ReplicationAcceptRequest>(ReplicationAcceptRequest::class, packetId) {

    override fun resolve(packet: Packet): ReplicationAcceptRequest {
        with(ByteBuffer.wrap(packet.body)) {
            val replicationId = long
            val masterId = string
            val masterOffset = long
            val masterIp = string
            val masterPort = int
            return ReplicationAcceptRequest(
                replicationId,
                InstanceAddress(masterIp, masterPort),
                VersionAndOffset(masterId, masterOffset)
            )
        }
    }

    override fun serialize(msg: ReplicationAcceptRequest): ByteArray {
        return ByteBuffer.allocate(
            sizeOfString(msg.masterInfo.id) +
                    sizeOfString(msg.masterAddress.dest) +
                    Long.SIZE_BYTES * 3 +
                    Int.SIZE_BYTES
        ).apply {
            putLong(msg.replicationId)
            putString(msg.masterInfo.id)
            putLong(msg.masterInfo.offset)
            putString(msg.masterAddress.dest)
            putInt(msg.masterAddress.port)
        }.array()
    }
}
