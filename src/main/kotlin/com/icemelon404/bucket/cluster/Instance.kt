package com.icemelon404.bucket.cluster

import com.icemelon404.bucket.common.InstanceAddress

interface Instance{
    val address : InstanceAddress
    fun requestVote(term: Long, index: LogIndex)
    fun heartBeat(term: Long)
}
