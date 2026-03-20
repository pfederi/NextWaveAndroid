package com.lakeshorestudios.nextwave.data.utils

import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetch text from a URL with configurable timeouts.
 * Replaces java.net.URL.readText() which has no timeout.
 */
fun URL.readTextWithTimeout(
    connectTimeoutMs: Int = 15_000,
    readTimeoutMs: Int = 15_000
): String {
    val connection = openConnection() as HttpURLConnection
    connection.connectTimeout = connectTimeoutMs
    connection.readTimeout = readTimeoutMs
    return try {
        connection.inputStream.bufferedReader().readText()
    } finally {
        connection.disconnect()
    }
}
