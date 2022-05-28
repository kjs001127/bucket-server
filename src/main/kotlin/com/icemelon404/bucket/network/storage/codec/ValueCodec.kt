package com.icemelon404.bucket.network.storage.codec

import com.icemelon404.bucket.network.common.MessageCodec
import com.icemelon404.bucket.network.common.Packet
import com.icemelon404.bucket.network.storage.message.Value
import java.nio.ByteBuffer

class ValueCodec(packetId: Int) : MessageCodec<Value>(Value::class, packetId){

    override fun resolve(packet: Packet): Value {
        with(ByteBuffer.wrap(packet.body)) {
            val requestId = long
            val bytes = int.takeIf { it != 0 }?.let { valueSize-> ByteArray(valueSize).also { get(it) } }
            return Value(requestId, bytes)
        }
    }

    override fun serialize(msg: Value): ByteArray {
        return ByteBuffer.allocate((msg.byteArray?.size?:0) + 12).apply {
            putLong(msg.requestId)
            msg.byteArray?.let {
                putInt(it.size)
                put(it)
            }?: putInt(0)
        }.array()
    }
}