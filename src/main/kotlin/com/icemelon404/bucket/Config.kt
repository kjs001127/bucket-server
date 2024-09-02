package com.icemelon404.bucket

data class Config(
    val host: String,
    val port: Int,
    val clusterPort: Int = port + 10000,
    val replicationPort:Int = port + 20000,
    val clusterNodes: List<String>,
    val dataPath: String
)
