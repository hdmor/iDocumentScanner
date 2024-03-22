package com.i.document.scanner

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.i.document.scanner.ui.theme.IDocumentScannerTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GmsDocumentScannerOptions.Builder().setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true).setPageLimit(5)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF).build()
        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            IDocumentScannerTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var imagesUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = {
                            if (it.resultCode == RESULT_OK) {
                                val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                                imagesUris = result?.pages?.map { uri -> uri.imageUri } ?: emptyList()
                                result?.pdf?.let { pdf ->
                                    val fos = FileOutputStream(File(filesDir, "document.pdf"))
                                    contentResolver.openInputStream(pdf.uri)?.use { inputStream ->
                                        inputStream.copyTo(fos)
                                    }
                                }
                            }
                        }
                    )
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        imagesUris.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                        Button(
                            onClick = {
                                scanner.getStartScanIntent(this@MainActivity)
                                    .addOnSuccessListener {
                                        scannerLauncher.launch(IntentSenderRequest.Builder(it).build())
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(applicationContext, it.message, Toast.LENGTH_LONG).show()
                                    }
                            }
                        ) { Text(text = "Scan PDF") }
                    }
                }
            }
        }
    }
}