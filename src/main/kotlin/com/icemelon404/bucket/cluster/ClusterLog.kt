package com.icemelon404.bucket.cluster

data class TermAndOffset(val term: Long, val offset: Long)
interface ClusterLog: Comparable<ClusterLog> {
    val termAndOffset: TermAndOffset

    override fun compareTo(other: ClusterLog): Int {
        val mine = termAndOffset
        val others = other.termAndOffset
        return mine.term.compareTo(others.term).takeIf { it != 0 }
            ?: mine.offset.compareTo(others.offset)
    }
}