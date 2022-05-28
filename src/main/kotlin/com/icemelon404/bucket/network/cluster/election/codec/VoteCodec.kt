package com.icemelon404.bucket.network.cluster.election.codec

import com.icemelon404.bucket.network.cluster.election.Vote
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import java.nio.ByteBuffer


class VoteCodec(packetId: Int) : MessageCodec<Vote>(Vote::class, packetId) {

    override fun resolve(packet: Packet) = Vote(ByteBuffer.wrap(packet.body).long)

    override fun serialize(msg: Vote): ByteArray {
        return ByteBuffer.allocate(8).putLong(msg.term).array()
    }
}