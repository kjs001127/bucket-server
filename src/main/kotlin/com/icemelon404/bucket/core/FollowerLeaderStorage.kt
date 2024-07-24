package com.icemelon404.bucket.core

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.core.storage.FollowerStorage
import com.icemelon404.bucket.core.storage.LeaderStorage
import com.icemelon404.bucket.core.storage.StorageStatus
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.KeyValueStorage

class FollowerLeaderStorage(
    private val leader: LeaderStorage,
    private val follower: FollowerStorage,
):  KeyValueStorage{

    @Volatile
    private var status: StorageStatus? = null
    override fun write(keyValue: KeyValue) {
        status?.write(keyValue)?: error("storage is idle")
    }

    override fun read(key: String): ByteArray {
        return status?.read(key)?: error("storage is idle")
    }

    fun toLeader(term: Long) {
        status?.close()
        status = leader
        leader.startWith(term)
    }

    fun toFollower(leader: InstanceAddress) {
        status?.close()
        follower.startWith(leader)
        status = follower
    }

    fun setIdle() {
        status = null
    }

    override fun clear() {
        status?.clear()
    }
}
