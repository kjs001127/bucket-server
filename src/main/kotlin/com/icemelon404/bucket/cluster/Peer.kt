package com.icemelon404.bucket.cluster

import com.icemelon404.bucket.common.InstanceAddress

interface Peer{
    val address : InstanceAddress
    fun requestVote(term: Long, index: Log)
    fun heartBeat(term: Long)
}
