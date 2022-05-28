package com.icemelon404.bucket.cluster.election

interface Transition {
    fun toLeader()
    fun toCandidate()
    fun toFollower()
}