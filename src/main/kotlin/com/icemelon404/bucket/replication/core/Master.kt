package com.icemelon404.bucket.replication.core

import com.icemelon404.bucket.util.logger
import com.icemelon404.bucket.replication.*
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import kotlin.math.min

class Master(
    private val scheduler: ExecutorService,
    private val replicatorFactory: ReplicatorFactory,
    private val offsetReadable: OffsetReadable,
    private val versionOffsetManager: Recorder
) : ReplicationStatus {

    private val replicationTask = mutableMapOf<String, ReplicationTask>()

    override fun start() {
        versionOffsetManager.rollWith(offsetReadable.offset)
        logger().info { "Master state with id: ${versionOffsetManager.currentVersion}, last-id: ${versionOffsetManager.lastVersionAndOffset}" }
    }

    override fun onRequest(request: ReplicationContext, acceptor: ReplicationAcceptor) {
        if (shouldAcceptNewReplication(request)) {
            val startOffset = replicationStartOffset(request.lastMaster)
            createNewReplication(request, startOffset)
            acceptor.accept(VersionAndOffset(versionOffsetManager.currentVersion, startOffset))
        }
    }

    private fun createNewReplication(request: ReplicationContext, offset: Long) {
        logger().info { "Starting new replication id: ${request.replicationId} to ${request.instanceId}" }
        replicationTask[request.instanceId]?.cancel()
        replicationTask[request.instanceId] = ReplicationTask(null, request.replicationId, offset)
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
        var seqNo = 0L
        while (!Thread.currentThread().isInterrupted) {
            replicator.read(buf, timeout)
            buf.flip()

            val data = ByteArray(buf.remaining())
            buf.get(data, buf.position(), buf.limit())
            request.sendData(seqNo, data)

            buf.clear()
            seqNo++
        }
    }

    private fun shouldAcceptNewReplication(request: ReplicationContext) =
        replicationTask[request.instanceId]?.let {
            it.replicationId < request.replicationId
        } ?: true

    private fun shouldAcceptAck(ack: Ack) =
        replicationTask[ack.instanceId]?.let {
            it.replicationId == ack.replicationId
        }?:false

    override fun onAck(ack: Ack, dataSender: ReplicationDataSender) {
        if (shouldAcceptAck(ack)) {
            logger().info { "replication ack received from ${ack.instanceId}, ${ack.replicationId}" }
            replicationTask[ack.instanceId]?.let {
                it.task = this.scheduler.submit{
                    sendReplicationData(dataSender, it.startOffset, 1000, 1024*1024)
                }
            }
        }
    }


    override fun close() {
        for ((_, task) in replicationTask)
            task.cancel()
    }

}

class ReplicationTask(
    var task: Future<*>?,
    val replicationId: Long,
    val startOffset: Long
) {

    fun cancel() {
        task?.cancel(true)
    }
}
