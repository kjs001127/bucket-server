package com.icemelon404.bucket.adapter.core.storage

import com.icemelon404.bucket.storage.KeyValueStorage

interface StorageStatus: KeyValueStorage {
    fun start()
    fun close()
}