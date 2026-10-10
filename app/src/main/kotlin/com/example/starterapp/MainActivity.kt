package com.example.starterapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.starterapp.navigation.StarterNavHost
import com.example.starterapp.sync.SyncQueueWorker
import com.example.starterapp.ui.theme.StarterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as StarterApplication
        // Opportunistic drain: if anything was queued in a previous session, try to sync it as
        // soon as the app is opened again. SyncQueueWorker is also enqueued the moment an action
        // is queued; this covers the "app was closed the whole time" case.
        SyncQueueWorker.enqueue(applicationContext)
        setContent {
            StarterTheme {
                StarterNavHost(app = app)
            }
        }
    }
}
