package com.example.sealyoutube

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sealyoutube.ui.theme.SealYouTubeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        setContent {
            SealYouTubeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    YouTubeScreen(
                        onBackPressed = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeScreen(onBackPressed: () -> Unit) {
    val activity = LocalActivity.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    BackHandler {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
        } else if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onBackPressed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .then(if (customView == null) Modifier.systemBarsPadding() else Modifier),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    
                    webViewClient = WebViewClient()

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            customView = view
                            customViewCallback = callback
                            activity?.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        }

                        override fun onHideCustomView() {
                            customView = null
                            customViewCallback = null
                            activity?.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }
                    loadUrl("https://www.youtube.com")
                    webView = this
                }
            },
            update = {
                webView = it
            }
        )

        customView?.let { view ->
            AndroidView(
                factory = { view },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
