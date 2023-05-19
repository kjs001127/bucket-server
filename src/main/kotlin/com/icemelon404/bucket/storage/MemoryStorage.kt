package com.icemelon404.bucket.storage

import java.util.concurrent.ConcurrentHashMap

class MemoryStorage: KeyValueStorage {

    private val map = ConcurrentHashMap<String, ByteArray?>()

    override fun write(keyValue: KeyValue) {
        map[keyValue.key] = keyValue.value
    }

    override fun read(key: String): ByteArray? {
        return map[key]
    }

    override fun clear() {
        map.clear()
    }
}