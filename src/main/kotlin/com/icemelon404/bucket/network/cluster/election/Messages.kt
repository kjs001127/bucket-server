package com.icemelon404.bucket.network.cluster.election

import com.icemelon404.bucket.common.InstanceAddress

class VoteRequest( val term: Long)
class Vote( val term: Long)
class HeartBeat( val term: Long, val address: InstanceAddress)
class HeartBeatDeny( val term: Long)