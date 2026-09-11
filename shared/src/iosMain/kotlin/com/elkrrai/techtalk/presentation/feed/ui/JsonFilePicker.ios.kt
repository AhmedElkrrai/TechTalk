package com.elkrrai.techtalk.presentation.feed.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.elkrrai.techtalk.presentation.component.AppButton
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
private class JsonDocumentPickerDelegate(
    private val onFileSelected: (String?) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        val content = url?.let {
            NSString.stringWithContentsOfURL(it, encoding = NSUTF8StringEncoding, error = null) as String?
        }
        onFileSelected(content)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onFileSelected(null)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun JsonFilePicker(onFileSelected: (jsonContent: String?) -> Unit) {
    val delegate = remember { JsonDocumentPickerDelegate(onFileSelected) }
    AppButton(
        text = "Import JSON file",
        onClick = {
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON))
            picker.delegate = delegate
            UIApplication.sharedApplication.keyWindow
                ?.rootViewController
                ?.presentViewController(picker, animated = true, completion = null)
        }
    )
}
