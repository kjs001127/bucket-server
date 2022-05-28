package com.icemelon404.bucket.replication

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.common.logger
import com.icemelon404.bucket.replication.listener.*
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.KeyValueStorage
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Slave(
    private val instanceId: String,
    private val master: InstanceAddress,
    private val executorService: ScheduledExecutorService,
    private val replicationLogHandler: ReplicationLogHandler,
    private val replicationDest: OffsetAwareWritable,
    private val storage: KeyValueStorage,
    connect: (InstanceAddress) -> ReplicationSource
) : ReplicationStatus {

    private val replicationSource = connect(master)
    private lateinit var requestJob: ScheduledFuture<*>
    private lateinit var masterStorageAddr: InstanceAddress
    private val lock = ReentrantLock()
    private var replicationId: Long = 0

    @Volatile
    private var timeout = System.currentTimeMillis()

    override fun onStart() {
        logger().info { "Slave state" }
        requestJob = executorService.scheduleWithFixedDelay({
            if (timeout < System.currentTimeMillis())
                requestReplication()
        }, 0, 1000, TimeUnit.MILLISECONDS)
    }

    private fun requestReplication() {
        lock.withLock {
            replicationId++
            replicationSource.requestReplication(
                FollowerInfo(
                    instanceId,
                    replicationId,
                    replicationLogHandler.currentMaster
                )
            )
            logger().info { "Requesting replication id: $replicationId to $master with id: ${replicationLogHandler.currentMaster}" }
        }
    }

    private fun refreshSchedule() {
        timeout = System.currentTimeMillis() + 3000
    }

    override fun onData(replication: DataReplication) {
        if (replication.replicationId != replicationId)
            return
        writeData(replication.content)
    }

    private fun writeData(content: List<KeyValue>) {
        lock.withLock {
            refreshSchedule()
            if (content.isNotEmpty())
                replicationDest.write(content)
        }
    }

    override fun close() {
        requestJob.cancel(true)
    }

    override fun onReplicationAccept(accept: ReplicationAccept) {
        lock.withLock {
            if (accept.replicationId != replicationId)
                return
            refreshSchedule()
            masterStorageAddr = accept.masterAddress
            replicationDest.offset = accept.dataInfo.offset
            replicationLogHandler.newMasterId(accept.dataInfo.id)
            logger().info { "Replication accepted with id:  ${replicationLogHandler.currentMaster}" }
        }
    }

    override fun write(keyValue: KeyValue) {
        throw RedirectException(masterStorageAddr)
    }

    override fun read(key: String): ByteArray? {
        return storage.read(key)
    }
}

interface ReplicationSource {
    fun requestReplication(info: FollowerInfo)
}

class RedirectException(val dest: InstanceAddress) : RuntimeException()

data class FollowerInfo(val instanceId: String, val replicationId: Long, val lastMaster: IdAndOffset)

interface ReplicationSourceConnector {
    fun connect(address: InstanceAddress): ReplicationSource
}

interface OffsetAwareWritable {
    var offset: Long
    fun write(keyValues: List<KeyValue>)
}
