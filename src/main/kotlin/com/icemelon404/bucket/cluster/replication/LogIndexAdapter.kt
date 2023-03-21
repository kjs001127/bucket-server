package com.icemelon404.bucket.cluster.replication

import com.icemelon404.bucket.cluster.election.LogIndex
import com.icemelon404.bucket.cluster.election.TermAndOffset
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