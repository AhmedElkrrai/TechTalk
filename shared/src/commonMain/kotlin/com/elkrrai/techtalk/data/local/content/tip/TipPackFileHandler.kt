package com.elkrrai.techtalk.data.local.content.tip

/** Platform-implemented file IO for user-shareable tip pack JSON files. */
interface TipPackFileHandler {
    suspend fun saveToFile(fileName: String, jsonContent: String): String
    suspend fun readFromFile(filePath: String): String
    suspend fun listFiles(): List<String>
    suspend fun deleteFile(fileName: String): Boolean
}
