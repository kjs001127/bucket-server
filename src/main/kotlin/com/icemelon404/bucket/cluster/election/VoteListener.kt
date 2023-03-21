package com.icemelon404.bucket.cluster.election

import com.icemelon404.bucket.common.InstanceAddress

interface VoteListener {
    fun onVotePending()
    fun onElectedAsLeader(term: Long)
    fun onLeaderFound(term: Long, leaderAddress: InstanceAddress)
}