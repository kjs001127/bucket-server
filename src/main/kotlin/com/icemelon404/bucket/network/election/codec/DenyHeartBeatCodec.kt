package com.icemelon404.bucket.network.election.codec

import com.icemelon404.bucket.network.election.HeartBeatDeny
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import java.nio.ByteBuffer

class DenyHeartBeatCodec(packetId: Int) : MessageCodec<HeartBeatDeny>(HeartBeatDeny::class, packetId) {

    override fun resolve(packet: Packet): HeartBeatDeny = HeartBeatDeny(ByteBuffer.wrap(packet.body).long)

    override fun serialize(msg: HeartBeatDeny): ByteArray {
        return ByteBuffer.allocate(8).putLong(msg.term).array()
    }
}
