package com.watchocr.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.watchocr.app.data.AppDatabase
import com.watchocr.app.data.AppSettings
import com.watchocr.app.data.ImageBucket
import com.watchocr.app.data.MediaStoreImages
import com.watchocr.app.data.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Permission needed to query MediaStore for images on this OS version. */
private val mediaImagesPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

@Composable
fun SettingsScreen(settingsDataStore: SettingsDataStore, settings: AppSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The text fields own their state and are seeded from DataStore exactly once.
    // Keying remember on the round-tripped settings value would let a stale
    // DataStore emission reset the field mid-typing and drop keystrokes.
    var apiKey by rememberSaveable { mutableStateOf(settings.apiKey) }
    var model by rememberSaveable { mutableStateOf(settings.model) }
    var seededFromStore by rememberSaveable { mutableStateOf(false) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    // Non-null while the folder picker dialog is showing.
    var pickerBuckets by remember { mutableStateOf<List<ImageBucket>?>(null) }

    LaunchedEffect(Unit) {
        if (!seededFromStore) {
            val stored = settingsDataStore.settingsFlow.first()
            apiKey = stored.apiKey
            model = stored.model
            seededFromStore = true
        }
    }

    fun openFolderPicker() {
        scope.launch {
            pickerBuckets = withContext(Dispatchers.IO) { MediaStoreImages.queryBuckets(context) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openFolderPicker()
        } else {
            Toast.makeText(context, "Photo access is required to choose a folder.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Monitored Folder", style = MaterialTheme.typography.titleMedium)
        Text(settings.bucketName ?: "No folder selected", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = {
            val permission = mediaImagesPermission
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                openFolderPicker()
            } else {
                permissionLauncher.launch(permission)
            }
        }) {
            Text("Choose Folder")
        }

        Divider()

        Text("Gemini API Key", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                scope.launch { settingsDataStore.setApiKey(it) }
            },
            label = { Text("API Key") },
            singleLine = true,
            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                    Icon(
                        imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Divider()

        Text("Gemini Model (OCR)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = model,
            onValueChange = {
                model = it
                scope.launch { settingsDataStore.setModel(it) }
            },
            label = { Text("Model") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    pickerBuckets?.let { buckets ->
        AlertDialog(
            onDismissRequest = { pickerBuckets = null },
            title = { Text("Choose a folder") },
            text = {
                if (buckets.isEmpty()) {
                    Text("No image folders found on this device.")
                } else {
                    LazyColumn {
                        items(buckets, key = { it.id }) { bucket ->
                            Text(
                                "${bucket.name} (${bucket.imageCount})",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val changed = bucket.id != settings.bucketId
                                        scope.launch {
                                            if (changed) {
                                                AppDatabase.getInstance(context).monitoredFileDao().clear()
                                            }
                                            settingsDataStore.setWatchedBucket(bucket.id, bucket.name)
                                        }
                                        pickerBuckets = null
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerBuckets = null }) { Text("Cancel") }
            }
        )
    }
}
