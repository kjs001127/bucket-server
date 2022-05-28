package com.icemelon404.bucket.cluster.election

import com.icemelon404.bucket.cluster.election.listener.ClusterEventListener

interface InstanceStatus : ClusterEventListener {
    fun onStart() {}
}