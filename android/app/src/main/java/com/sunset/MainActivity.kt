package com.sunset

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.sunset.player.PlayerActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context

val Context.dataStore by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    private val SERVER_URL_KEY = stringPreferencesKey("server_url")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            var serverUrl by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                serverUrl = dataStore.data.first()[SERVER_URL_KEY]
            }

            if (serverUrl == null) {
                ServerSelectionScreen { url ->
                    scope.launch {
                        dataStore.edit { it[SERVER_URL_KEY] = url }
                        serverUrl = url
                    }
                }
            } else {
                SunSetWebView(serverUrl!!)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ServerSelectionScreen(onServerSelected: (String) -> Unit) {
        var input by remember { mutableStateOf("") }
        
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("SunSet", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Server URL") },
                placeholder = { Text("http://your-server:7867") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (input.isNotBlank()) onServerSelected(input) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect")
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun SunSetWebView(url: String) {
        AndroidView(factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }
                }
                
                addJavascriptInterface(SunSetBridge(context), "SunSetAndroid")
                loadUrl(url)
            }
        }, modifier = Modifier.fillMaxSize())
    }

    inner class SunSetBridge(private val context: Context) {
        @JavascriptInterface
        fun playVideo(url: String, title: String, itemId: String) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra("video_url", url)
                putExtra("video_title", title)
                putExtra("item_id", itemId)
            }
            context.startActivity(intent)
        }
        
        @JavascriptInterface
        fun resetServer() {
            lifecycleScope.launch {
                context.dataStore.edit { it.remove(SERVER_URL_KEY) }
                recreate()
            }
        }
    }
}
