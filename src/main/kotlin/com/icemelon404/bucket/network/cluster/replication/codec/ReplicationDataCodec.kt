package com.icemelon404.bucket.network.cluster.replication.codec

import com.icemelon404.bucket.network.cluster.replication.ReplicationData
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.codec.KeyValueCodec
import java.nio.ByteBuffer

class ReplicationDataCodec(
    packetId: Int,
    val codec: KeyValueCodec
) : MessageCodec<ReplicationData>(ReplicationData::class, packetId) {

    override fun resolve(packet: Packet): ReplicationData {
        with(ByteBuffer.wrap(packet.body)) {
            val term = long
            val replicationId = long
            val seqNo = long
            val keyValues = codec.deserialize(this)
                .takeUnless { hasRemaining() }
                ?: error("Error in replication data packet: ${remaining()} extra bytes")
            return ReplicationData(term, replicationId, seqNo, keyValues)
        }
    }


    override fun serialize(msg: ReplicationData): ByteArray {
        val data = codec.serialize(msg.data)
        return ByteBuffer.allocate(data.limit() + 16).apply {
            putLong(msg.term)
            putLong(msg.replicationId)
            putLong(msg.seqNo)
            put(data)
        }.array()
    }


}