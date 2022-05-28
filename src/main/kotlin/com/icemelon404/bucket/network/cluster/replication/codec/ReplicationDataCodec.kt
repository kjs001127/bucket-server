package com.icemelon404.bucket.network.cluster.replication.codec

import com.icemelon404.bucket.network.cluster.replication.ReplicationData
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.network.util.*
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.codec.KeyValueCodec
import java.nio.ByteBuffer

class ReplicationDataCodec(
    packetId: Int,
    val codec: KeyValueCodec
) : MessageCodec<ReplicationData>(ReplicationData::class, packetId) {

    override fun resolve(packet: Packet): ReplicationData {
        with(ByteBuffer.wrap(packet.body)) {
            val term = long
            val replicationId = long
            val keyValues = codec.deserialize(this)
                .takeUnless { hasRemaining() }
                ?: error("Error in replication data packet: ${remaining()} extra bytes")
            return ReplicationData(term, replicationId, keyValues)
        }
    }


    override fun serialize(msg: ReplicationData): ByteArray {
        val data = codec.serialize(msg.data)
        return ByteBuffer.allocate(data.limit() + 16).apply {
            putLong(msg.term)
            putLong(msg.replicationId)
            put(data)
        }.array()
    }


}