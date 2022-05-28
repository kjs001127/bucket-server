package com.icemelon404.bucket.common

import mu.KotlinLogging
import org.slf4j.LoggerFactory


inline fun <reified T> T.logger() =
    KotlinLogging.logger(T::class.qualifiedName?:"anonymous")

