package com.icemelon404.bucket.cluster.election

data class TermAndOffset(val term: Long, val offset: Long)
interface LogIndex: Comparable<LogIndex> {
    val termAndOffset: TermAndOffset

    override fun compareTo(other: LogIndex): Int {
        val mine = termAndOffset
        val others = other.termAndOffset
        return mine.term.compareTo(others.term).takeIf { it != 0 }
            ?: mine.offset.compareTo(others.offset)
    }
}