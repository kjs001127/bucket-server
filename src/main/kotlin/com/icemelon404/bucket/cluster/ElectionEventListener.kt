package com.icemelon404.bucket.cluster

import com.icemelon404.bucket.common.InstanceAddress

interface ElectionEventListener {
    fun onVotePending()
    fun onElectedAsLeader(term: Long)
    fun onLeaderFound(term: Long, leaderAddress: InstanceAddress)
}
