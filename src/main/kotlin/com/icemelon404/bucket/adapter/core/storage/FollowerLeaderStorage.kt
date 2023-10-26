package com.icemelon404.bucket.adapter.core.storage

import com.icemelon404.bucket.common.InstanceAddress
import com.icemelon404.bucket.storage.KeyValue
import com.icemelon404.bucket.storage.KeyValueStorage

class FollowerLeaderStorage(
    private val leader: LeaderStorage,
    private val follower: FollowerStorage,
):  KeyValueStorage{

    @Volatile
    private var status: StorageStatus? = null
    override fun write(keyValue: KeyValue) {
        status?.write(keyValue)?: error("storage null")
    }

    override fun read(key: String): ByteArray {
        return status?.read(key)?: error("storage null")
    }

    fun toLeader(term: Long) {
        leader.setTerm(term)
        status = leader
        leader.start()
    }

    fun toFollower(leader: InstanceAddress) {
        follower.setLeader(leader)
        status = follower
        follower.start()
    }

    fun setIdle() {
        status = null
    }

    override fun clear() {
        status?.clear()
    }
}