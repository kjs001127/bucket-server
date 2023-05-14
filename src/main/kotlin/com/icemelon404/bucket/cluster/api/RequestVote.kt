package com.icemelon404.bucket.cluster.api

import com.icemelon404.bucket.cluster.LogIndex

interface RequestVote {
    val term : Long
    val logIndex: LogIndex
    fun vote()
}