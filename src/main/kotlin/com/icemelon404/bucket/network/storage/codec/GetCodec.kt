package com.icemelon404.bucket.network.storage.codec

import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.network.storage.message.Get
import com.icemelon404.bucket.common.bufferSizeOf
import com.icemelon404.bucket.common.putString
import com.icemelon404.bucket.common.string
import java.nio.ByteBuffer

class GetCodec(packetId: Int) : MessageCodec<Get>(Get::class, packetId) {

    override fun resolve(packet: Packet): Get {
        with(ByteBuffer.wrap(packet.body)) {
            val requestId = long
            val key = string
            return Get(requestId, key)
        }
    }

    override fun serialize(msg: Get): ByteArray {
        return ByteBuffer.allocate(bufferSizeOf(msg.key)).apply {
            putLong(msg.requestId)
            putString(msg.key)
        }.array()
    }
}