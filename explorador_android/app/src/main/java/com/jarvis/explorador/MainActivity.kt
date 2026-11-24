package com.jarvis.explorador

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jarvis.explorador.ui.ExplorerScreen
import com.jarvis.explorador.ui.ExplorerViewModel
import com.jarvis.explorador.ui.ExploradorTheme
import com.jarvis.explorador.ui.FolderColor

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PATH = "path"
    }

    private val vm: ExplorerViewModel by viewModels()
    private val granted = mutableStateOf(false)

    private val legacyLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            granted.value = hasPermission()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        granted.value = hasPermission()
        val startPath = intent?.getStringExtra(EXTRA_PATH)
        setContent {
            ExploradorTheme {
                if (granted.value) {
                    LaunchedEffect(Unit) { vm.start(startPath) }
                    ExplorerScreen(vm)
                } else {
                    PermissionScreen { requestPermission() }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val g = hasPermission()
        if (g != granted.value) granted.value = g
        if (g) vm.refreshAll()
    }

    private fun hasPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            legacyLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }
}

@androidx.compose.runtime.Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.FolderOpen, null, tint = FolderColor, modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(16.dp))
            Text("Explorador necesita acceso a tus archivos", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Para mostrar, ordenar, copiar y mover cualquier archivo de la tablet y la tarjeta SD, " +
                    "activa «Permitir acceso para gestionar todos los archivos» en la siguiente pantalla.",
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 560.dp),
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest) { Text("Conceder acceso") }
        }
    }
}
