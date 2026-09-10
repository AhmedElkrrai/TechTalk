package com.elkrrai.techtalk.data.local.content.tip

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import platform.Foundation.NSUTF8StringEncoding

/** Writes to the iOS Documents dir. */
@OptIn(ExperimentalForeignApi::class)
class IosTipPackFileHandler : TipPackFileHandler {

    private val fileManager = NSFileManager.defaultManager

    private fun packDirPath(): String {
        val documents = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        val dirPath = requireNotNull(documents?.path) { "Could not resolve iOS documents directory" } + "/tip_packs"
        if (!fileManager.fileExistsAtPath(dirPath)) {
            fileManager.createDirectoryAtPath(dirPath, withIntermediateDirectories = true, attributes = null, error = null)
        }
        return dirPath
    }

    @OptIn(BetaInteropApi::class)
    override suspend fun saveToFile(fileName: String, jsonContent: String): String {
        val path = packDirPath() + "/" + fileName
        val nsString = NSString.create(string = jsonContent)
        val data: NSData? = nsString.dataUsingEncoding(NSUTF8StringEncoding)
        data?.writeToFile(path, atomically = true)
        return path
    }

    override suspend fun readFromFile(filePath: String): String =
        NSString.stringWithContentsOfFile(filePath, encoding = NSUTF8StringEncoding, error = null) ?: ""

    override suspend fun listFiles(): List<String> {
        val dirPath = packDirPath()
        @Suppress("UNCHECKED_CAST")
        val contents = fileManager.contentsOfDirectoryAtPath(dirPath, error = null) as? List<String>
        return contents ?: emptyList()
    }

    override suspend fun deleteFile(fileName: String): Boolean {
        val path = packDirPath() + "/" + fileName
        return fileManager.removeItemAtPath(path, error = null)
    }
}
