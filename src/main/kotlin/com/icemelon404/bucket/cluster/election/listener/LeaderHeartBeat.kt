package com.icemelon404.bucket.cluster.election.listener

import com.icemelon404.bucket.common.InstanceAddress

interface LeaderHeartBeat {
    val instanceId : InstanceAddress
    val term: Long
    fun deny(term : Long)
}
