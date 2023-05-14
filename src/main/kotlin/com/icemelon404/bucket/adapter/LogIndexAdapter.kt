package com.icemelon404.bucket.adapter

import com.icemelon404.bucket.cluster.LogIndex
import com.icemelon404.bucket.cluster.TermAndOffset
import com.icemelon404.bucket.replication.VersionOffsetManager

class LogIndexAdapter(
    private val delegate: VersionOffsetManager,
) : LogIndex {
    override val termAndOffset: TermAndOffset
        get() {
            val res = delegate.versionAndOffset
            return TermAndOffset(res.id, res.offset)
        }
}