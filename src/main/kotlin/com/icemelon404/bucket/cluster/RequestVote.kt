package com.icemelon404.bucket.cluster

interface RequestVote {
    val term : Long
    val log: ClusterLog
    fun vote()
}