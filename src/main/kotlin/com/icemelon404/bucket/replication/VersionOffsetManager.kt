package com.icemelon404.bucket.replication

import com.icemelon404.bucket.replication.api.IdAndOffset

interface VersionOffsetManager {
    var versionAndOffset: IdAndOffset
}