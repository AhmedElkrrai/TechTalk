package com.elkrrai.techtalk.presentation.feed.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.elkrrai.techtalk.presentation.component.AppButton

@Composable
actual fun JsonFilePicker(onFileSelected: (jsonContent: String?) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) {
            onFileSelected(null)
            return@rememberLauncherForActivityResult
        }
        val content = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        onFileSelected(content)
    }
    AppButton(text = "Import JSON file", onClick = { launcher.launch("application/json") })
}
