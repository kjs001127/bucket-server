package com.icemelon404.bucket.cluster.election

interface AppendLogIndex : Comparable<AppendLogIndex> {
    val id: Long
    val offset: Long

    override fun compareTo(other: AppendLogIndex): Int {
        return id.compareTo(other.id).takeIf { it != 0 }
            ?: offset.compareTo(other.offset)
    }
}