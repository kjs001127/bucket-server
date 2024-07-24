package com.icemelon404.bucket.cluster

interface ElectionService {
    fun onHeartBeat(claim: LeaderHeartBeat) {}
    fun onHeartBeatDeny(responseTerm : Long) {}
    fun onRequestVote(voteRequest : VoteRequest) {}
    fun onVoteReceived(voteTerm : Long) {}
}
