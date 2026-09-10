package com.elkrrai.techtalk.data.local.content.tip

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Writes to the app-specific external files dir. */
class AndroidTipPackFileHandler(private val context: Context) : TipPackFileHandler {

    private fun packDir(): File =
        (context.getExternalFilesDir(null) ?: context.filesDir)
            .resolve("tip_packs")
            .apply { mkdirs() }

    override suspend fun saveToFile(fileName: String, jsonContent: String): String =
        withContext(Dispatchers.IO) {
            val file = File(packDir(), fileName)
            file.writeText(jsonContent)
            file.absolutePath
        }

    override suspend fun readFromFile(filePath: String): String =
        withContext(Dispatchers.IO) {
            File(filePath).readText()
        }

    override suspend fun listFiles(): List<String> =
        withContext(Dispatchers.IO) {
            packDir().listFiles()?.map { it.name } ?: emptyList()
        }

    override suspend fun deleteFile(fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            File(packDir(), fileName).delete()
        }
}
