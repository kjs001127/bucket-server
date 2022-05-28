package com.icemelon404.bucket.network.storage.codec

import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.network.storage.message.Ack
import java.nio.ByteBuffer

class AckCodec(packetId: Int) : MessageCodec<Ack>(Ack::class, packetId) {

    override fun resolve(packet: Packet): Ack {
        with(ByteBuffer.wrap(packet.body)) {
            return Ack(long)
        }
    }

    override fun serialize(msg: Ack): ByteArray =
        ByteBuffer.allocate(8).putLong(msg.requestId).array()

}