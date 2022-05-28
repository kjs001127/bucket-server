package com.icemelon404.bucket.network.common

data class Packet(val packetType: Int, val body: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Packet

        if (packetType != other.packetType) return false
        if (!body.contentEquals(other.body)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packetType.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }
}