package com.icemelon404.bucket.common

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock

inline fun <T> ReentrantLock.withTry(f: () -> T): T {
    this.tryLock()
    try {
        return f()
    } finally {
        this.unlock()
    }
}

inline fun <T> ReentrantReadWriteLock.ReadLock.withTry(f: () -> T): T {
    this.tryLock()
    try {
        return f()
    } finally {
        this.unlock()
    }
}
