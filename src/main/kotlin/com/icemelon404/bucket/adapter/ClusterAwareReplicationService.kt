package com.icemelon404.bucket.adapter

import com.icemelon404.bucket.replication.DataReplication
import com.icemelon404.bucket.replication.ReplicationAccept
import com.icemelon404.bucket.replication.ReplicationContext
import com.icemelon404.bucket.replication.VersionAndOffset


interface ClusterAwareReplicationService {
    fun onAccept(accept: ClusterReplicationAccept)
    fun onRequest(context: ClusterReplicationContext, acceptor: ClusterReplicationAcceptor)
    fun onData(data: ClusterDataReplication)
}

class ClusterReplicationAccept(
    val term: Long,
    val data: ReplicationAccept
)

class ClusterDataReplication(
    val term: Long,
    val data: DataReplication
)

class ClusterReplicationContext(
    val context: ReplicationContext,
    val term: Long
)

interface ClusterReplicationAcceptor {
    fun accept(currentTerm: Long, masterInfo: VersionAndOffset): ClusterReplicationDataSender
}

interface ClusterReplicationDataSender {
    fun sendData(currentTerm: Long, data: ByteArray)
}
