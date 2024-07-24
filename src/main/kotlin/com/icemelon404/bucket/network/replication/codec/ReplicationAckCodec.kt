package com.icemelon404.bucket.network.replication.codec

import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.common.sizeOfString
import com.icemelon404.bucket.common.putString
import com.icemelon404.bucket.common.string
import com.icemelon404.bucket.network.replication.ReplicationAck
import java.nio.ByteBuffer

class ReplicationAckCodec(packetId: Int) :
    MessageCodec<ReplicationAck>(ReplicationAck::class, packetId) {

    override fun resolve(packet: Packet): ReplicationAck {
        with(ByteBuffer.wrap(packet.body)) {
            val replicationId = long
            val instanceId = string
            return ReplicationAck(
                replicationId = replicationId,
                instanceId = instanceId,
            )
        }
    }

    override fun serialize(msg: ReplicationAck): ByteArray {
        return ByteBuffer.allocate(
            sizeOfString(msg.instanceId) +
                    Long.SIZE_BYTES,
        ).apply {
            putLong(msg.replicationId)
            putString(msg.instanceId)
        }.array()
    }
}
