package com.icemelon404.bucket.network.cluster.election.codec

import com.icemelon404.bucket.network.cluster.election.VoteRequest
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import java.nio.ByteBuffer


class VoteRequestCodec(packetId: Int) : MessageCodec<VoteRequest>(VoteRequest::class, packetId) {

    override fun resolve(packet: Packet): VoteRequest {
        with(ByteBuffer.wrap(packet.body)) {
            val term = long
            val id = long
            val offset = long
            return VoteRequest(term, id, offset)
        }
    }

    override fun serialize(msg: VoteRequest): ByteArray {
        return ByteBuffer.allocate(24).apply {
            putLong(msg.term)
            putLong(msg.logId)
            putLong(msg.logOffset)
        }.array()
    }
}

