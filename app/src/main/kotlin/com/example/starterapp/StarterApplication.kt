package com.example.starterapp

import android.app.Application
import com.example.starterapp.connectivity.ConnectivityObserver
import com.example.starterapp.connectivity.NetworkConnectivityObserver
import com.example.starterapp.data.ServerConfig
import com.example.starterapp.data.ServerConfigStore
import com.example.starterapp.data.local.AppDatabase
import com.example.starterapp.data.local.dao.PendingActionDao
import com.example.starterapp.data.remote.StarterApi
import com.example.starterapp.data.repository.ActionRepository
import com.example.starterapp.di.ApiClientFactory
import com.example.starterapp.sync.SyncQueueWorker
import kotlinx.coroutines.runBlocking

/**
 * Composition root. A hand-rolled object graph is clearer than a DI framework
 * (Hilt/Dagger) while the app is small; swap one in when it stops being so.
 */
class StarterApplication : Application() {
    lateinit var database: AppDatabase
        private set

    lateinit var serverConfigStore: ServerConfigStore
        private set

    lateinit var connectivityObserver: ConnectivityObserver
        private set

    lateinit var actionRepository: ActionRepository
        private set

    val pendingActionDao: PendingActionDao
        get() = database.pendingActionDao()

    /** Builds an API client against the currently saved server config. Never cached: the
     * config can change at runtime from the server settings screen. */
    private fun currentApi(): StarterApi {
        val config: ServerConfig = runBlocking { serverConfigStore.current() }
        return ApiClientFactory.create(config)
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        serverConfigStore = ServerConfigStore(this)
        connectivityObserver = NetworkConnectivityObserver(this)
        actionRepository =
            ActionRepository(
                pendingActionDao = database.pendingActionDao(),
                connectivityObserver = connectivityObserver,
                apiProvider = ::currentApi,
                onActionQueued = { SyncQueueWorker.enqueue(this) }
            )
    }
}
