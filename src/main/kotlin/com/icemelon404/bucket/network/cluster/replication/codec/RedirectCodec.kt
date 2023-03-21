package com.icemelon404.bucket.network.cluster.replication.codec

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.replication.Redirect
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.common.bufferSizeOf
import com.icemelon404.bucket.common.putString
import com.icemelon404.bucket.common.string
import java.nio.ByteBuffer

class RedirectCodec(packetId: Int) : MessageCodec<Redirect>(Redirect::class, packetId) {

    override fun resolve(packet: Packet): Redirect {
        with(ByteBuffer.wrap(packet.body)) {
            val dest = string
            val port = int
            return Redirect(InstanceAddress(dest, port))
        }
    }

    override fun serialize(msg: Redirect): ByteArray {
        return ByteBuffer.allocate(bufferSizeOf(msg.address.dest) + 4).apply {
            putString(msg.address.dest)
            putInt(msg.address.port)
        }.array()
    }
}