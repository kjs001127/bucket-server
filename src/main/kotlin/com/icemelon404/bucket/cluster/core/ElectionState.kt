package com.icemelon404.bucket.cluster.core

import com.icemelon404.bucket.cluster.ElectionService

interface ElectionState : ElectionService {
    fun onStart() {}
}
