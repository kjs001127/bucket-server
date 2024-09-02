package com.icemelon404.bucket.network.storage.message

import com.icemelon404.bucket.core.KeyValue

class Get(val requestId: Long, val key: String)
class Set(val requestId: Long, val keyValue: KeyValue)
class Ack(val requestId: Long)
class Nack(val requestId: Long)
class Value(val requestId: Long, val byteArray: ByteArray?)
