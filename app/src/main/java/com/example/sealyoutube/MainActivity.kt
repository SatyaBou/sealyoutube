package com.example.sealyoutube

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
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
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.sealyoutube.ui.theme.SealYouTubeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        startBackgroundService()

        setContent {
            SealYouTubeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    YouTubeScreen(
                        onBackPressed = {
                            moveTaskToBack(true)
                        }
                    )
                }
            }
        }
    }

    private fun startBackgroundService() {
        val serviceIntent = Intent(this, BackgroundAudioService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop service when activity is completely destroyed to avoid draining battery
        stopService(Intent(this, BackgroundAudioService::class.java))
    }
}

class BackgroundAudioService : MediaBrowserServiceCompat() {
    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        
        mediaSession = MediaSessionCompat(this, "BackgroundAudioService").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            isActive = true
            
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    mediaCommandListener?.invoke("PLAY")
                    updateMediaSessionState(currentTitle, true)
                }

                override fun onPause() {
                    mediaCommandListener?.invoke("PAUSE")
                    updateMediaSessionState(currentTitle, false)
                }

                override fun onSkipToNext() {
                    mediaCommandListener?.invoke("NEXT")
                }

                override fun onSkipToPrevious() {
                    mediaCommandListener?.invoke("PREVIOUS")
                }
            })
        }
        sessionToken = mediaSession?.sessionToken

        createNotificationChannel()
        updateMediaSessionState(currentTitle, isCurrentlyPlaying)
    }

    private fun buildNotification(title: String, isPlaying: Boolean): android.app.Notification {
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause, "Pause",
                androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play, "Play",
                androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
            )
        }

        val previousAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous, "Previous",
            androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        )

        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next, "Next",
            androidx.media.session.MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Playing" else "Paused")
            .setSmallIcon(R.drawable.ic_logo)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            
        return builder.build()
    }

    private fun updateMediaSessionState(title: String, isPlaying: Boolean) {
        currentTitle = title
        isCurrentlyPlaying = isPlaying

        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackActions = PlaybackStateCompat.ACTION_PLAY or 
                              PlaybackStateCompat.ACTION_PAUSE or 
                              PlaybackStateCompat.ACTION_SKIP_TO_NEXT or 
                              PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                              PlaybackStateCompat.ACTION_STOP
                              
        val stateCompat = PlaybackStateCompat.Builder()
            .setActions(playbackActions)
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()
        mediaSession?.setPlaybackState(stateCompat)

        val metadata = android.support.v4.media.MediaMetadataCompat.Builder()
            .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, "YouTube")
            .build()
        mediaSession?.setMetadata(metadata)

        val notification = buildNotification(title, isPlaying)
        
        // Fix for ForegroundServiceDidNotStartInTimeException:
        // Always call startForeground to satisfy the requirement of startForegroundService().
        // We do this regardless of playback state to ensure the service is properly promoted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (!isPlaying) {
            // If not playing, we can remove the foreground requirement so the notification can be swiped away,
            // but we must have called startForeground first.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                stopForeground(false)
            }
            // Update the notification to ensure it shows the paused state and is not ongoing.
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        androidx.media.session.MediaButtonReceiver.handleIntent(mediaSession, intent)
        
        if (intent != null && intent.action == "ACTION_UPDATE_METADATA") {
            val title = intent.getStringExtra("EXTRA_TITLE") ?: currentTitle
            val isPlaying = intent.getBooleanExtra("EXTRA_IS_PLAYING", isCurrentlyPlaying)
            updateMediaSessionState(title, isPlaying)
        }
        
        return START_STICKY
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(mutableListOf())
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "background_audio_channel"
        private const val NOTIFICATION_ID = 1001
        
        private var currentTitle = "YouTube"
        private var isCurrentlyPlaying = true
        
        var mediaCommandListener: ((String) -> Unit)? = null
    }
}

class WebAppInterface(private val context: Context) {
    @android.webkit.JavascriptInterface
    fun updateMediaInfo(title: String, isPlaying: Boolean) {
        val intent = Intent(context, BackgroundAudioService::class.java).apply {
            action = "ACTION_UPDATE_METADATA"
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_IS_PLAYING", isPlaying)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
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

    androidx.compose.runtime.DisposableEffect(webView) {
        BackgroundAudioService.mediaCommandListener = { command ->
            webView?.post {
                when (command) {
                    "PLAY" -> webView?.evaluateJavascript("document.querySelector('video')?.play();", null)
                    "PAUSE" -> webView?.evaluateJavascript("document.querySelector('video')?.pause();", null)
                    "NEXT" -> webView?.evaluateJavascript("document.querySelector('.ytp-next-button')?.click();", null)
                    "PREVIOUS" -> webView?.evaluateJavascript("window.history.back();", null)
                }
            }
        }
        onDispose {
            BackgroundAudioService.mediaCommandListener = null
        }
    }

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
                object : WebView(context) {
                    override fun onWindowVisibilityChanged(visibility: Int) {
                        // Force WebView to think it's always visible to continue playback in background
                        super.onWindowVisibilityChanged(View.VISIBLE)
                    }

                    override fun onVisibilityChanged(changedView: View, visibility: Int) {
                        // Force WebView to think it's always visible to continue playback in background
                        super.onVisibilityChanged(changedView, View.VISIBLE)
                    }
                }.apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    
                    addJavascriptInterface(WebAppInterface(context), "AndroidMedia")
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Inject JavaScript to trick YouTube's Page Visibility API and report video state
                            view?.evaluateJavascript("""
                                (function() {
                                    Object.defineProperty(document, 'visibilityState', { value: 'visible', writable: false, configurable: true });
                                    Object.defineProperty(document, 'hidden', { value: false, writable: false, configurable: true });
                                    document.dispatchEvent(new Event('visibilitychange'));
                                    
                                    Object.defineProperty(document, 'webkitVisibilityState', { value: 'visible', writable: false, configurable: true });
                                    Object.defineProperty(document, 'webkitHidden', { value: false, writable: false, configurable: true });
                                    
                                    if (!window.hasMediaPoller) {
                                        window.hasMediaPoller = true;
                                        window.lastTitle = '';
                                        window.lastIsPlaying = null;
                                        setInterval(function() {
                                            var video = document.querySelector('video');
                                            var title = document.title;
                                            if (title && title.endsWith(' - YouTube')) {
                                                title = title.substring(0, title.length - 10);
                                            }
                                            var isPlaying = video ? !video.paused && !video.ended : false;
                                            
                                            if (title !== window.lastTitle || isPlaying !== window.lastIsPlaying) {
                                                window.lastTitle = title;
                                                window.lastIsPlaying = isPlaying;
                                                if (window.AndroidMedia) {
                                                    window.AndroidMedia.updateMediaInfo(title || 'YouTube', isPlaying);
                                                }
                                            }
                                        }, 1000);
                                    }
                                })();
                            """.trimIndent(), null)
                        }
                    }

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
