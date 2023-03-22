package com.icemelon404.bucket.cluster.election

import com.icemelon404.bucket.cluster.election.api.ClusterEventListener

interface InstanceStatus : ClusterEventListener {
    fun onStart() {}
}