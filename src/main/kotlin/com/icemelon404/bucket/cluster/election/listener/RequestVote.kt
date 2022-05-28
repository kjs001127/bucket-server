package com.icemelon404.bucket.cluster.election.listener

interface RequestVote {
    val term : Long
    fun vote()
}