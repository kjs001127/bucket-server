package com.icemelon404.bucket.cluster.election.listener

interface ClusterEventListener {
    fun onHeartBeat(claim: LeaderHeartBeat) {}
    fun onHeartBeatDeny(responseTerm : Long) {}
    fun onRequestVote(voteRequest : RequestVote) {}
    fun onVoteReceived(voteTerm : Long) {}
}