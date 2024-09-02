package com.icemelon404.bucket.network.replication.codec

import com.icemelon404.bucket.network.replication.ReplicationData
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import java.nio.ByteBuffer

class ReplicationDataCodec(
    packetId: Int
) : MessageCodec<ReplicationData>(ReplicationData::class, packetId) {

    override fun resolve(packet: Packet): ReplicationData {
        with(ByteBuffer.wrap(packet.body)) {
            val replicationId = long
            val seqNo = long
            val data = ByteArray(remaining()).also { get(it) }
            return ReplicationData(replicationId, seqNo, data)
        }
    }

    override fun serialize(msg: ReplicationData): ByteArray {
        return ByteBuffer.allocate(msg.data.size + Long.SIZE_BYTES*2).apply {
            putLong(msg.replicationId)
            putLong(msg.seqNo)
            put(msg.data)
        }.array()
    }


}
