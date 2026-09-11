package com.elkrrai.techtalk.presentation.feed.ui

import androidx.compose.runtime.Composable

/** Renders a button that opens a platform file picker restricted to JSON files.
 * Invokes [onFileSelected] with the file's text content, or null if the user
 * cancelled / the file couldn't be read. */
@Composable
expect fun JsonFilePicker(label: String = "Import JSON file", onFileSelected: (jsonContent: String?) -> Unit)
