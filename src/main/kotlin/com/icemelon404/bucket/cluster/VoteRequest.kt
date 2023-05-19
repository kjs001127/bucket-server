package com.icemelon404.bucket.cluster

interface VoteRequest {
    val term : Long
    val log: ClusterLog
    fun vote()
}