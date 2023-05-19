package com.icemelon404.bucket.replication.core

import com.icemelon404.bucket.common.logger
import com.icemelon404.bucket.replication.*
import io.netty.buffer.ByteBuf
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import kotlin.math.min

class Master(
    private val scheduler: ExecutorService,
    private val replicatorFactory: ReplicatorFactory,
    private val offsetReadable: OffsetReadable,
    private val versionOffsetManager: VersionOffsetManager
) : ReplicationStatus {

    private val replicationTask = mutableMapOf<String, ReplicationTask>()

    override fun start() {
        versionOffsetManager.rollWith(offsetReadable.offset)
        logger().info { "Master state with id: ${versionOffsetManager.currentVersion}, last-id: ${versionOffsetManager.lastVersionAndOffset}" }
    }

    override fun onRequest(request: ReplicationContext, accept: ReplicationAcceptor) {
        if (!shouldIgnore(request))
            startNewReplication(request, accept)
    }

    private fun startNewReplication(request: ReplicationContext, acceptor: ReplicationAcceptor) {
        logger().info { "Starting new replication id: ${request.replicationId} to ${request.instanceId}" }
        replicationTask[request.instanceId]?.cancel()
        replicationTask[request.instanceId] = ReplicationTask(newReplicationTask(request, acceptor), request.replicationId)
    }

    private fun newReplicationTask(request: ReplicationContext, acceptor: ReplicationAcceptor) =
        scheduler.submit {
            val startOffset = replicationStartOffset(request.lastMaster)
            val stream = acceptor.accept(VersionAndOffset(versionOffsetManager.currentVersion, startOffset))
            sendReplicationData(stream, startOffset, 100, 1024 * 32)
        }

    private fun replicationStartOffset(replicaIdOffset: VersionAndOffset): Long {
        return setOf(currentIdAndOffset(), versionOffsetManager.lastVersionAndOffset)
            .find { it?.id == replicaIdOffset.id }
            ?.let { min(it.offset, replicaIdOffset.offset) }
            ?: 0
    }

    private fun currentIdAndOffset(): VersionAndOffset {
        return VersionAndOffset(versionOffsetManager.currentVersion, offsetReadable.offset)
    }

    private fun sendReplicationData(
        request: ReplicationDataSender,
        startOffset: Long,
        timeout: Long,
        maxSize: Int,
    ) {
        val replicator = replicatorFactory.newReplicator(startOffset)
        val buf = ByteBuffer.allocate(maxSize)
        while (!Thread.currentThread().isInterrupted) {
            replicator.read(buf, timeout)
            buf.flip()
            request.sendData(buf.slice().array().clone())
            buf.clear()
        }
    }

    private fun shouldIgnore(request: ReplicationContext) =
        replicationTask[request.instanceId]?.let {
            it.replicationId >= request.replicationId
        } ?: false


    override fun close() {
        for ((_, task) in replicationTask)
            task.cancel()
    }

}

interface ReplicatorFactory {
    fun newReplicator(offset: Long): Replicator
}

interface Replicator : Closeable {
    fun read(buf: ByteBuffer, timeoutMs: Long)
}

class ReplicationTask(
    private val task: Future<*>,
    val replicationId: Long
) {
    fun cancel() {
        task.cancel(true)
    }
}