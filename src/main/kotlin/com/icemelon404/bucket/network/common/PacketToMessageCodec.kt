package com.icemelon404.bucket.network.common

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec

class PacketToMessageCodec(private val codecs: List<MessageCodec<*>>) : MessageToMessageCodec<Packet, Any>() {

    override fun decode(ctx: ChannelHandlerContext?, msg: Packet?, out: MutableList<Any>?) {
        msg ?: return
        out?.add(findCodec(msg).resolve(msg))
    }

    override fun encode(ctx: ChannelHandlerContext?, msg: Any?, out: MutableList<Any>?) {
        msg ?: return
        out?.add(findCodec(msg).toPacket(msg))
    }

    private fun findCodec(packet: Packet) =
        codecs.firstOrNull { it.supportsPacket(packet) } ?: error("처리 불가능한 패킷 타입입니다: ${packet.packetType}")

    private fun findCodec(msg: Any) =
        codecs.firstOrNull { it.supportsMessage(msg) } ?: error("처리 불가능한 메세지입니다")
}