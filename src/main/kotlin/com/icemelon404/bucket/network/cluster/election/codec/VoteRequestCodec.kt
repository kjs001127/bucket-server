package com.icemelon404.bucket.network.cluster.election.codec

import com.icemelon404.bucket.network.cluster.election.VoteRequest
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import java.nio.ByteBuffer


class VoteRequestCodec(packetId: Int) : MessageCodec<VoteRequest>(VoteRequest::class, packetId) {

    override fun resolve(packet: Packet): VoteRequest = VoteRequest(ByteBuffer.wrap(packet.body).long)

    override fun serialize(msg: VoteRequest): ByteArray {
        return ByteBuffer.allocate(8).putLong(msg.term).array()
    }
}

