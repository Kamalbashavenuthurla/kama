package com.kama.tracker

import android.app.Application
import com.kama.tracker.data.EvidenceStorage
import com.kama.tracker.data.KamaDatabase
import com.kama.tracker.data.KamaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KamaApplication : Application() {
    lateinit var repository: KamaRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val db = KamaDatabase.build(this)
        repository = KamaRepository(db, EvidenceStorage(this))
        appScope.launch { repository.ensureDefaults() }
    }
}
