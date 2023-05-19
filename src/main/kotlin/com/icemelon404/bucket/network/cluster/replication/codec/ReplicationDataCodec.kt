package com.icemelon404.bucket.network.cluster.replication.codec

import com.icemelon404.bucket.network.cluster.replication.ReplicationData
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.codec.KeyValueCodec
import java.nio.ByteBuffer

class ReplicationDataCodec(
    packetId: Int
) : MessageCodec<ReplicationData>(ReplicationData::class, packetId) {

    override fun resolve(packet: Packet): ReplicationData {
        with(ByteBuffer.wrap(packet.body)) {
            val term = long
            val replicationId = long
            val seqNo = long
            val data = ByteArray(remaining()).also { get(it) }
            return ReplicationData(term, replicationId, seqNo, data)
        }
    }


    override fun serialize(msg: ReplicationData): ByteArray {
        return ByteBuffer.allocate(msg.data.size + Long.SIZE_BYTES*3).apply {
            putLong(msg.term)
            putLong(msg.replicationId)
            putLong(msg.seqNo)
            put(msg.data)
        }.array()
    }


}