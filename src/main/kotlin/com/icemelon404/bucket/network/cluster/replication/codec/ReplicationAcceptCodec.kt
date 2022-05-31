package com.icemelon404.bucket.network.cluster.replication.codec

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.network.cluster.replication.ReplicationAccept
import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.network.util.bufferSize
import com.icemelon404.bucket.network.util.putString
import com.icemelon404.bucket.network.util.string
import com.icemelon404.bucket.replication.listener.IdAndOffset
import java.nio.ByteBuffer

class ReplicationAcceptCodec(packetId: Int) : MessageCodec<ReplicationAccept>(ReplicationAccept::class, packetId) {

    override fun resolve(packet: Packet): ReplicationAccept {
        with(ByteBuffer.wrap(packet.body)) {
            val term = long
            val replicationId = long
            val masterId = long
            val masterOffset = long
            val masterIp = string
            val masterPort = int
            return ReplicationAccept(
                term,
                replicationId,
                InstanceAddress(masterIp, masterPort),
                IdAndOffset(masterId, masterOffset)
            )
        }
    }

    override fun serialize(msg: ReplicationAccept): ByteArray {
        return ByteBuffer.allocate(bufferSize(msg.masterAddress.dest) + 36).apply {
            putLong(msg.term)
            putLong(msg.replicationId)
            putLong(msg.masterInfo.id)
            putLong(msg.masterInfo.offset)
            putString(msg.masterAddress.dest)
            putInt(msg.masterAddress.port)
        }.array()
    }
}