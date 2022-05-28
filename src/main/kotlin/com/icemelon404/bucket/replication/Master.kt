package com.icemelon404.bucket.replication

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.common.logger
import com.icemelon404.bucket.replication.listener.IdAndOffset
import com.icemelon404.bucket.replication.listener.ReplicationRequest
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.KeyValueStorage
import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import kotlin.math.min

class Master(
    private val scheduler: ExecutorService,
    private val replicatorFactory: ReplicatorFactory,
    private val replicationLogHandler: ReplicationLogHandler,
    private val storage: KeyValueStorage
) : ReplicationStatus {

    private val replication = mutableMapOf<String, Replication>()

    override fun onStart() {
        replicationLogHandler.newMasterId()
        logger().info { "Master state with id: ${replicationLogHandler.currentMaster}" }
    }

    override fun onReplicationRequest(request: ReplicationRequest) {
        if (!shouldIgnore(request))
            startNewReplication(request)
    }

    private fun startNewReplication(request: ReplicationRequest) {
        logger().info { "Starting new replication id: ${request.replicationId} to ${request.instanceId}" }
        replication[request.instanceId]?.cancel()
        replication[request.instanceId] = Replication(newReplicationTask(request), request.replicationId)
    }

    private fun newReplicationTask(request: ReplicationRequest) =
        scheduler.submit {
            val startOffset = replicationStartOffset(request.lastMaster)
            request.accept(IdAndOffset(replicationLogHandler.currentMaster.id, startOffset))
            sendReplicationData(request, startOffset, 500, 500)
        }

    private fun replicationStartOffset(replicaIdOffset: IdAndOffset) =
        setOf(replicationLogHandler.currentMaster, replicationLogHandler.lastMaster)
            .find { it?.id == replicaIdOffset.id }
            ?.let { min(it.offset, replicaIdOffset.offset) }
            ?: 0

    private fun sendReplicationData(
        request: ReplicationRequest,
        startOffset: Long,
        timeout: Long,
        maxSize: Long
    ) {
        replicatorFactory.newReplicator(startOffset).use {
            var send = mutableListOf<KeyValue>()
            var currentTimeout = System.currentTimeMillis() + timeout
            it.withKeyValue { keyValue ->
                if (keyValue != null)
                    send += keyValue
                if (send.size >= maxSize || currentTimeout < System.currentTimeMillis()) {
                    try {
                        request.sendData(send)
                    } catch (e: Exception) {
                        return@withKeyValue false
                    }
                    send = mutableListOf()
                    currentTimeout = System.currentTimeMillis() + timeout
                }
                !Thread.currentThread().isInterrupted
            }
        }
    }

    private fun shouldIgnore(request: ReplicationRequest) =
        replication[request.instanceId]?.let {
            it.replicationId >= request.replicationId
        } ?: false


    override fun close() {
        for ((_, task) in replication)
            task.cancel()
    }

    override fun write(keyValue: KeyValue) = storage.write(keyValue)

    override fun read(key: String) = storage.read(key)
}

interface ReplicatorFactory {
    fun newReplicator(offset: Long): Replicator
}

interface Replicator : Closeable {
    fun withKeyValue(consumer: (KeyValue?) -> Boolean)
}

class Replication(
    private val task: Future<*>,
    val replicationId: Long
) {
    fun cancel() {
        task.cancel(true)
    }
}