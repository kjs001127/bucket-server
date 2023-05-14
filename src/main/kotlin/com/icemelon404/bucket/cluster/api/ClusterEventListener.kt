package com.icemelon404.bucket.cluster.api

interface ClusterEventListener {
    fun onHeartBeat(claim: LeaderHeartBeat) {}
    fun onHeartBeatDeny(responseTerm : Long) {}
    fun onRequestVote(voteRequest : RequestVote) {}
    fun onVoteReceived(voteTerm : Long) {}
}