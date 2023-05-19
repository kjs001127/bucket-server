package com.icemelon404.bucket.replication.core

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.common.logger
import com.icemelon404.bucket.replication.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Slave(
    private val instanceId: String,
    private val executorService: ScheduledExecutorService,
    private val versionManager: VersionOffsetManager,
    private val aof: OffsetAwareWritable,
    private val replicationSrc: ReplicationSource
) : ReplicationStatus {

    private lateinit var requestJob: ScheduledFuture<*>
    private val lock = ReentrantLock()
    private var replicationId: Long = 0
    private var replicationSeqNo: Long = 0

    @Volatile
    private var timeout = System.currentTimeMillis()

    override fun start() {
        logger().info { "Slave state" }
        requestJob = executorService.scheduleWithFixedDelay({
            if (timeout < System.currentTimeMillis())
                requestReplication()
        }, 0, 1000, TimeUnit.MILLISECONDS)
    }

    private fun requestReplication() {
        lock.withLock {
            replicationId++
            replicationSeqNo = 0
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

    private fun currentIdAndOffset(): VersionAndOffset =
        VersionAndOffset(this.versionManager.currentVersion, aof.offset)

    private fun refreshRequestTimeout() {
        timeout = System.currentTimeMillis() + 3000
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
        requestJob.cancel(true)
    }

    override fun onAccept(accept: ReplicationAccept) {
        lock.withLock {
            if (accept.replicationId != replicationId)
                return
            refreshRequestTimeout()
            aof.truncate(accept.dataInfo.offset)
            versionManager.rollWith(accept.dataInfo.id, accept.dataInfo.offset)
            logger().info { "Replication accepted with id:  ${currentIdAndOffset().id}" }
        }
    }
}

interface ReplicationSource {
    val address: InstanceAddress
    fun requestReplication(info: FollowerInfo)
}

data class FollowerInfo(val instanceId: String, val replicationId: Long, val lastMaster: VersionAndOffset)

interface ReplicationSourceConnector {
    fun connect(address: InstanceAddress): ReplicationSource
}