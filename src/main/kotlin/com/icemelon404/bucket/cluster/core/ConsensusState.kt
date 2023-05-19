package com.icemelon404.bucket.cluster.core

import com.icemelon404.bucket.cluster.ConsensusService

interface ConsensusState : ConsensusService {
    fun onStart() {}
}