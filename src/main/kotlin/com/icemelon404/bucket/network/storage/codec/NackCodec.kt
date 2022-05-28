package com.icemelon404.bucket.network.storage.codec

import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.network.storage.message.Nack
import java.nio.ByteBuffer

class NackCodec(packetId: Int) : MessageCodec<Nack>(Nack::class, packetId) {
    override fun resolve(packet: Packet): Nack {
        with(ByteBuffer.wrap(packet.body)) {
            return Nack(long)
        }
    }

    override fun serialize(msg: Nack): ByteArray =
        ByteBuffer.allocate(8).putLong(msg.requestId).array()

}