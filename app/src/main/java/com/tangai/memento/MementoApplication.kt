package com.tangai.memento

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class MementoApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: android.content.Context): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(100L * 1024L * 1024L)
                    .build()
            }
            .build()

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        if (BuildConfig.DEBUG && File(filesDir, FIREBASE_EMULATOR_MARKER).exists()) {
            FirebaseAuth.getInstance().useEmulator(EMULATOR_HOST, 9099)
            FirebaseFirestore.getInstance().useEmulator(EMULATOR_HOST, 8080)
            FirebaseStorage.getInstance().useEmulator(EMULATOR_HOST, 9199)
            FirebaseFunctions.getInstance("asia-southeast1").useEmulator(EMULATOR_HOST, 5001)
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory())
        signInEmulatorTestAccount()
    }

    private fun signInEmulatorTestAccount() {
        if (!BuildConfig.DEBUG) return

        val credentials = File(filesDir, FIREBASE_EMULATOR_ACCOUNT)
            .takeIf(File::isFile)
            ?.readLines()
            ?: return
        val email = credentials.getOrNull(0)?.trim().orEmpty()
        val password = credentials.getOrNull(1).orEmpty()
        if (email.isBlank() || password.isBlank()) return

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
    }

    private companion object {
        const val FIREBASE_EMULATOR_MARKER = "use_firebase_emulators"
        const val FIREBASE_EMULATOR_ACCOUNT = "firebase_emulator_account"
        const val EMULATOR_HOST = "10.0.2.2"
    }
}
