package com.icemelon404.bucket.cluster.election.listener

import com.icemelon404.bucket.cluster.election.AppendLogIndex

interface RequestVote {
    val term : Long
    val logIndex: AppendLogIndex
    fun vote()
}