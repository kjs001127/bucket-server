package com.icemelon404.bucket.storage

interface KeyValueStorage {
    fun write(keyValue: KeyValue)
    fun read(key: String): ByteArray?
    fun clear()
}