package com.icemelon404.bucket.core.storage

import com.icemelon404.bucket.core.KeyValueStorage

interface StorageStatus: KeyValueStorage {
    fun close()
}
