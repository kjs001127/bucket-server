package com.icemelon404.bucket.adapter.core

import com.icemelon404.bucket.adapter.ClusterReplicationAcceptor
import com.icemelon404.bucket.adapter.ClusterReplicationDataSender
import com.icemelon404.bucket.replication.ReplicationAcceptor
import com.icemelon404.bucket.replication.ReplicationDataSender
import com.icemelon404.bucket.replication.VersionAndOffset


class ReplicationAcceptorAdapter(
    val term: Long,
    private val delegate: ClusterReplicationAcceptor
) : ReplicationAcceptor {
    override fun accept(masterInfo: VersionAndOffset): ReplicationDataSender {
        return ReplicationDataSenderAdaptor(term, delegate.accept(term, masterInfo))
    }
}

class ReplicationDataSenderAdaptor(
    private val term: Long,
    private val delegate: ClusterReplicationDataSender
) : ReplicationDataSender {
    override fun sendData(data: ByteArray) {
        delegate.sendData(term, data)
    }
}