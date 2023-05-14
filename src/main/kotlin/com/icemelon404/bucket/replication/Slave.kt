package com.icemelon404.bucket.replication

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.common.logger
import com.icemelon404.bucket.replication.api.*
import com.icemelon404.bucket.storage.KeyValue
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Slave(
    private val instanceId: String,
    private val executorService: ScheduledExecutorService,
    private val aof: VersionOffsetWriter,
    private val replicationSrc: ReplicationSource
) : ReplicationLifecycle {

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

    private fun currentIdAndOffset(): IdAndOffset = this.aof.versionAndOffset

    private fun refreshRequestTimeout() {
        timeout = System.currentTimeMillis() + 3000
    }

    override fun onData(replication: DataReplication) = lock.withLock {
        if (replication.replicationId != replicationId || replication.seqNo != replicationSeqNo)
            return
        replicationSeqNo++
        writeData(replication.content)
    }

    private fun writeData(content: List<KeyValue>) {
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
            aof.versionAndOffset = accept.dataInfo
            logger().info { "Replication accepted with id:  ${currentIdAndOffset().id}" }
        }
    }

    override fun check(): Status {
        return Status(readable = true, writable = false, this.replicationSrc.address)
    }
}

interface ReplicationSource {
    val address: InstanceAddress
    fun requestReplication(info: FollowerInfo)
}

data class FollowerInfo(val instanceId: String, val replicationId: Long, val lastMaster: IdAndOffset)

interface ReplicationSourceConnector {
    fun connect(address: InstanceAddress): ReplicationSource
}