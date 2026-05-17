package com.waju.factory.app.generator.core.util

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

class DocumentPickerHelper(
    private val contentResolver: ContentResolver
) {
    fun buildHtmlImportIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/html"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
    }

    fun buildAssetFolderIntent(initialUri: Uri? = null): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            initialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, it) }
        }
    }

    fun takeReadPermission(uri: Uri, flags: Int, onPermissionUnavailable: (Uri) -> Unit) {
        val persistableFlags = flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
        val flagsToPersist = if (persistableFlags != 0) persistableFlags else Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            contentResolver.takePersistableUriPermission(uri, flagsToPersist)
        } catch (_: SecurityException) {
            onPermissionUnavailable(uri)
        }
    }

    fun readText(uri: Uri): String {
        return contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Could not read text from uri")
    }
}