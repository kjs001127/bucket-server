package com.icemelon404.bucket.replication.core

import com.icemelon404.bucket.util.logger
import com.icemelon404.bucket.replication.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Slave(
    private val instanceId: String,
    private val executorService: ScheduledExecutorService,
    private val recorder: Recorder,
    private val aof: OffsetAwareWritable,
    private val replicationSrc: ReplicationSource
) : ReplicationStatus {

    private lateinit var scheduler: ScheduledFuture<*>
    private val lock = ReentrantLock()
    private var replicationId: Long = 0
    private var replicationSeqNo: Long = 0

    @Volatile
    private var timeout = System.currentTimeMillis()

    override fun start() {
        logger().info { "Slave state" }
        scheduler = executorService.scheduleWithFixedDelay({
            if (timeout < System.currentTimeMillis()) {
                try {
                    requestReplication()
                } catch (e: Exception) {
                    logger().warn { "request replication failed. cause: $e"}
                }
            }

        }, 0, 1000, TimeUnit.MILLISECONDS)
    }

    private fun requestReplication() {
        lock.withLock {
            renewLocalMetadata()
            replicationSrc.requestReplication(
                FollowerInfo(
                    instanceId,
                    replicationId,
                    currentIdAndOffset(),
                )
            )
            logger().info { "Requesting replication id: $replicationId with id: ${currentIdAndOffset().id}}" }
        }
    }

    private fun renewLocalMetadata() {
        replicationId++
        replicationSeqNo = 0
    }

    private fun currentIdAndOffset(): VersionAndOffset =
        VersionAndOffset(this.recorder.currentVersion, aof.offset)

    private fun refreshRequestTimeout() {
        timeout = System.currentTimeMillis() + 10000
    }

    override fun onData(replication: DataReplication) = lock.withLock {
        if (replication.replicationId != replicationId || replication.seqNo != replicationSeqNo)
            return
        replicationSeqNo++
        writeData(replication.content)
    }

    private fun writeData(content: ByteArray) {
        refreshRequestTimeout()
        if (content.isNotEmpty())
            aof.write(content)
    }

    override fun close() {
        scheduler.cancel(true)
    }

    override fun onAccept(accept: ReplicationAccept, ack: ReplicationAckSender) = lock.withLock {
        if (accept.replicationId != replicationId)
            return

        refreshRequestTimeout()
        aof.truncate(accept.dataInfo.offset)
        recorder.rollWith(accept.dataInfo.id, accept.dataInfo.offset)

        ack.ack(replicationId, instanceId)

        logger().info { "Replication accepted with id:  ${currentIdAndOffset().id}" }
    }

}

