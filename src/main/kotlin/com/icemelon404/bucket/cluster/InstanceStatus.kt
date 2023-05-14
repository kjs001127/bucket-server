package com.icemelon404.bucket.cluster

import com.icemelon404.bucket.cluster.api.ClusterEventListener

interface InstanceStatus : ClusterEventListener {
    fun onStart() {}
}