package com.icemelon404.bucket.network.storage.codec

import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.network.storage.message.Set
import com.icemelon404.bucket.network.util.bufferSize
import com.icemelon404.bucket.network.util.putString
import com.icemelon404.bucket.network.util.string
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.codec.KeyValueCodec
import java.nio.ByteBuffer

class SetCodec(
    packetId: Int,
    val codec: KeyValueCodec
) : MessageCodec<Set>(Set::class, packetId) {

    override fun resolve(packet: Packet): Set {
        with(ByteBuffer.wrap(packet.body)) {
            val requestId = long
            val keyValue = codec.deserialize(this)
                .takeIf { it.size == 1 }
                .takeUnless { hasRemaining() }
                ?.first()
                ?: error("Error in set packet: ${remaining()} extra bytes")
            return Set(requestId, keyValue)
        }
    }

    override fun serialize(msg: Set): ByteArray {
        val data = codec.serialize(listOf(msg.keyValue))
        return ByteBuffer.allocate(data.limit() + 8).apply {
            putLong(msg.requestId)
            put(data)
        }.array()
    }
}