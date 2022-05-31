package com.icemelon404.bucket.cluster.election

import com.icemelon404.bucket.common.InstanceAddress

interface Storage {
    fun disable()
    fun setLeader(term: Long)
    fun setFollowerOf(instanceId: InstanceAddress?)
}