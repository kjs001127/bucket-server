package com.icemelon404.bucket.replication

import com.icemelon404.bucket.common.logger
import com.icemelon404.bucket.replication.api.*
import com.icemelon404.bucket.storage.KeyValue
import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import kotlin.math.min

class Master(
    private val id: Long,
    private val scheduler: ExecutorService,
    private val replicatorFactory: ReplicatorFactory,
    private val aof: VersionOffsetWriter,
) : ReplicationLifecycle {

    private val replication = mutableMapOf<String, Replication>()

    override fun start() {
        aof.versionAndOffset = IdAndOffset(id, aof.versionAndOffset.offset)
        logger().info { "Master state with id: ${aof.versionAndOffset.id}, last-id: ${aof.lastVersionAndOffset}" }
    }

    override fun onRequest(request: ReplicationContext, acceptor: ReplicationAcceptor) {
        if (!shouldIgnore(request))
            startNewReplication(request, acceptor)
    }

    override fun check(): Status {
        return Status(readable = true, writable = true, redirectAddress = null)
    }

    private fun startNewReplication(request: ReplicationContext, acceptor: ReplicationAcceptor) {
        logger().info { "Starting new replication id: ${request.replicationId} to ${request.instanceId}" }
        replication[request.instanceId]?.cancel()
        replication[request.instanceId] = Replication(newReplicationTask(request, acceptor), request.replicationId)
    }

    private fun newReplicationTask(request: ReplicationContext, acceptor: ReplicationAcceptor) =
        scheduler.submit {
            val startOffset = replicationStartOffset(request.lastMaster)
            val stream = acceptor.accept(IdAndOffset(aof.versionAndOffset.id, startOffset))
            sendReplicationData(stream, startOffset, 500, 500)
        }

    private fun replicationStartOffset(replicaIdOffset: IdAndOffset): Long {
        return setOf(currentIdAndOffset(), aof.lastVersionAndOffset)
            .find { it?.id == replicaIdOffset.id }
            ?.let { min(it.offset, replicaIdOffset.offset) }
            ?: 0
    }

    private fun currentIdAndOffset(): IdAndOffset {
        return aof.versionAndOffset
    }

    private fun sendReplicationData(
        request: ReplicationDataSender,
        startOffset: Long,
        timeout: Long,
        maxSize: Long
    ) {
        val replicator = replicatorFactory.newReplicator(startOffset)

        while (!Thread.currentThread().isInterrupted) {
            val toSend = mutableListOf<KeyValue>()
            val currentTimeout = System.currentTimeMillis() + timeout
            while (toSend.size < maxSize && System.currentTimeMillis() < currentTimeout) {
                replicator.next()?.also { toSend.add(it) }
            }
            request.sendData(toSend)
        }
    }

    private fun shouldIgnore(request: ReplicationContext) =
        replication[request.instanceId]?.let {
            it.replicationId >= request.replicationId
        } ?: false


    override fun close() {
        for ((_, task) in replication)
            task.cancel()
    }

}

interface ReplicatorFactory {
    fun newReplicator(offset: Long): Replicator
}

interface Replicator : Closeable {
    fun next(): KeyValue?
}

class Replication(
    private val task: Future<*>,
    val replicationId: Long
) {
    fun cancel() {
        task.cancel(true)
    }
}