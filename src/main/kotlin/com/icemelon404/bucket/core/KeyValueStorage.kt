package com.icemelon404.bucket.core

import com.icemelon404.bucket.core.KeyValue

interface KeyValueStorage {
    fun write(keyValue: KeyValue)
    fun read(key: String): ByteArray?
    fun clear()
}
