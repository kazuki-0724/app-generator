package com.waju.factory.app.generator.feature.home

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.waju.factory.app.generator.data.model.MiniApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    enum class SampleApp(val id: String, val title: String, val indexPath: String) {
        TODO("miniapp_todo_list", "TODO", "samples/todo/index.html"),
        CALCULATOR("sample-calculator", "Calculator", "samples/calculator/index.html"),
        QR("sample-qr", "QR Reader", "samples/qr/index.html"),
        CANVAS("sample-canvas", "Canvas", "samples/canvas/index.html"),
    }

    private val sharedPreferences = application.getSharedPreferences("MiniAppData", Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- State (UIの状態) ---

    private val _miniApps = MutableStateFlow<List<MiniApp>>(emptyList())
    val miniApps: StateFlow<List<MiniApp>> = _miniApps.asStateFlow()

    private val _selectedAppId = MutableStateFlow<String?>(null)
    val selectedAppId: StateFlow<String?> = _selectedAppId.asStateFlow()

    private val _showNewAppDialog = MutableStateFlow(false)
    val showNewAppDialog: StateFlow<Boolean> = _showNewAppDialog.asStateFlow()

    val newAppTitle: StateFlow<String> = savedStateHandle.getStateFlow("NEW_APP_TITLE", "")
    val htmlContent: StateFlow<String> = savedStateHandle.getStateFlow("HTML_CONTENT", "")

    fun updateNewAppTitle(title: String) {
        savedStateHandle["NEW_APP_TITLE"] = title
    }

    fun updateHtmlContent(htmlContent: String) {
        savedStateHandle["HTML_CONTENT"] = htmlContent
    }

    init {
        loadMiniApps()
    }

    // --- ロジック ---

    private fun loadMiniApps() {
        try {
            val json = sharedPreferences.getString("mini_apps_list", "[]") ?: "[]"
            val type = object : TypeToken<List<MiniApp>>() {}.type
            val loadedApps = gson.fromJson<List<MiniApp>>(json, type).orEmpty()
            val normalizedApps = ensureSampleAppExists(loadedApps)
            _miniApps.value = normalizedApps
            if (normalizedApps.size != loadedApps.size) {
                saveMiniApps(normalizedApps)
            }
        } catch (_: Exception) {
            val fallbackApps = ensureSampleAppExists(emptyList())
            _miniApps.value = fallbackApps
            saveMiniApps(fallbackApps)
        }
    }

    private fun ensureSampleAppExists(apps: List<MiniApp>): List<MiniApp> {
        val existingIds = apps.map { it.id }.toSet()
        if (SampleApp.entries.all { it.id in existingIds }) {
            return apps
        }

        val sampleTimestamp = LocalDate
            .of(2026, 5, 5)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val newSamples = SampleApp.entries
            .filter { it.id !in existingIds }
            .mapNotNull { sample ->
                val html = readSampleHtml(sample.indexPath) ?: return@mapNotNull null
                MiniApp(
                    id = sample.id,
                    title = sample.title,
                    htmlContent = html,
                    timestamp = sampleTimestamp,
                )
            }

        return newSamples + apps
    }

    private fun readSampleHtml(indexPath: String): String? {
        return try {
            getApplication<Application>().assets.open(indexPath).bufferedReader().use { it.readText() }
        } catch (_: IOException) {
            null
        }
    }

    private fun saveMiniApps(apps: List<MiniApp>) {
        try {
            val json = gson.toJson(apps)
            sharedPreferences.edit { putString("mini_apps_list", json) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openApp(appId: String) {
        _selectedAppId.value = appId
    }

    fun closeEditor() {
        _selectedAppId.value = null
    }

    fun showDialog() {
        _showNewAppDialog.value = true
    }

    fun dismissDialog() {
        _showNewAppDialog.value = false
        savedStateHandle["NEW_APP_TITLE"] = ""
        savedStateHandle["HTML_CONTENT"] = ""
    }

    fun addApp(htmlContent: String): MiniApp {
        val title = newAppTitle.value.ifBlank { "Unnamed App" }
        val newApp = MiniApp(
            id = UUID.randomUUID().toString(),
            title = title,
            htmlContent = htmlContent,
            timestamp = System.currentTimeMillis()
        )
        val updatedList = _miniApps.value + newApp
        _miniApps.value = updatedList
        saveMiniApps(updatedList)
        return newApp
    }

    fun deleteApp(app: MiniApp) {
        if (SampleApp.entries.toTypedArray().any { it.id == app.id }) {
            return
        }
        val updatedList = _miniApps.value.filter { it.id != app.id }
        _miniApps.value = updatedList
        saveMiniApps(updatedList)
    }

    fun updateApp(updatedApp: MiniApp) {
        val updatedList = _miniApps.value.map { if (it.id == updatedApp.id) updatedApp else it }
        _miniApps.value = updatedList
        saveMiniApps(updatedList)
    }

    fun importFromCanvas(text: String): MiniApp {
        val currentContent = text.ifEmpty { htmlContent.value }
        val cleanedHTML = currentContent
            .replace("```html", "")
            .replace("```", "")
            .trim()

        updateHtmlContent(cleanedHTML)

        val newApp = addApp(cleanedHTML)
        dismissDialog()
        return newApp
    }

    fun getAppDetailData(key: String) : String {
        return sharedPreferences.getString(key, "[]") ?: "[]"
        //val type = object : TypeToken<List<MiniApp>>() {}.type
        //return gson.fromJson<List<MiniApp>>(json, type).orEmpty()
    }

    fun deleteAppDetailData(key: String) {
        sharedPreferences.edit { remove(key) }
    }

    fun getLocalData() : List<MiniApp> {
        val json = sharedPreferences.getString("mini_apps_list", "[]") ?: "[]"
        val type = object : TypeToken<List<MiniApp>>() {}.type
        return gson.fromJson<List<MiniApp>>(json, type).orEmpty()
    }
}