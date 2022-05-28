package com.icemelon404.bucket.network.common

import kotlin.reflect.KClass


abstract class MessageCodec<T : Any>(
    private val clazz: KClass<T>,
    private val packetId: Int
){
    fun supportsPacket(packet : Packet) = packet.packetType == packetId
    abstract fun resolve(packet : Packet) : T
    fun supportsMessage(msg : Any) = clazz.isInstance(msg)
    fun toPacket(msg: Any) = Packet(packetId, serialize(msg as T))
    protected abstract fun serialize(msg : T) : ByteArray
}