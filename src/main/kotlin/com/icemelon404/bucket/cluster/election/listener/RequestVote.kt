package com.icemelon404.bucket.cluster.election.listener

import com.icemelon404.bucket.cluster.election.LogIndex

interface RequestVote {
    val term : Long
    val logIndex: LogIndex
    fun vote()
}