package com.icemelon404.bucket.cluster.core

import com.icemelon404.bucket.cluster.PeerEventListener

interface ElectionState : PeerEventListener {
    fun onStart() {}
}
