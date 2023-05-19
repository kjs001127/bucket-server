package com.icemelon404.bucket.adapter.storage

import com.icemelon404.bucket.storage.KeyValueStorage

interface StorageStatus: KeyValueStorage {
    fun start()
    fun close()
}