package com.icemelon404.bucket.replication

import com.icemelon404.bucket.replication.listener.IdAndOffset

interface VersionOffsetManager {
    var versionAndOffset: IdAndOffset
}