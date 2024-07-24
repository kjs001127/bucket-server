package com.icemelon404.bucket.util

import mu.KotlinLogging


inline fun <reified T> T.logger() =
    KotlinLogging.logger(T::class.qualifiedName?:"anonymous")

