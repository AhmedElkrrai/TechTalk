package com.elkrrai.techtalk.presentation.feed.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.elkrrai.techtalk.presentation.component.AppOutlinedButton

@Composable
actual fun JsonFilePicker(label: String, onFileSelected: (jsonContent: String?) -> Unit) {
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

    AppOutlinedButton(
        text = label,
        onClick = { launcher.launch("application/json") },
        modifier = Modifier.fillMaxWidth()
    )
}
