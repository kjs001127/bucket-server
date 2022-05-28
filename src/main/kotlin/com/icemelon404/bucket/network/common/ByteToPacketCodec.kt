package com.icemelon404.bucket.network.common

import io.netty.channel.ChannelHandlerContext
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.ByteToMessageCodec

class ByteToPacketCodec : ByteToMessageCodec<Packet>() {

    private val HEADER_SIZE = 8

    override fun decode(context: ChannelHandlerContext, byteBuf: ByteBuf, list: MutableList<Any>) {
        byteBuf.markReaderIndex()
        if (byteBuf.readableBytes() >= HEADER_SIZE) {
            val packetType = byteBuf.readInt()
            val bodyLength = byteBuf.readInt()
            if (byteBuf.readableBytes() < bodyLength) {
                byteBuf.resetReaderIndex()
                return
            }
            val body = ByteArray(bodyLength)
            byteBuf.readBytes(body)
            list.add(Packet(packetType, body))
        }
    }

    override fun encode(ctx: ChannelHandlerContext?, msg: Packet?, out: ByteBuf?) {
        msg?:return
        out?:return
        out.writeInt(msg.packetType)
        out.writeInt(msg.body.size)
        out.writeBytes(msg.body)
    }
}