package com.icemelon404.bucket.cluster

data class TermAndOffset(val term: Long, val offset: Long)
interface Log: Comparable<Log> {
    val termAndOffset: TermAndOffset

    override fun compareTo(other: Log): Int {
        val mine = termAndOffset
        val others = other.termAndOffset
        return mine.term.compareTo(others.term).takeIf { it != 0 }
            ?: mine.offset.compareTo(others.offset)
    }
}
