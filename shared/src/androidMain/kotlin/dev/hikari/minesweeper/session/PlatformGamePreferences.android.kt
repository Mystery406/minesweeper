package dev.hikari.minesweeper.session

import android.content.Context
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferencesSerializer
import okio.FileSystem
import okio.Path.Companion.toPath

private val gamePreferencesLock = Any()
private var androidGamePreferences: GamePreferences? = null

fun createGamePreferences(context: Context): GamePreferences = synchronized(gamePreferencesLock) {
    androidGamePreferences ?: DataStoreGamePreferences(
        dataStore = createPreferencesDataStore(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                producePath = {
                    context.applicationContext.filesDir
                        .resolve(DATA_STORE_FILE_NAME)
                        .absolutePath
                        .toPath()
                },
            ),
        ),
    ).also { androidGamePreferences = it }
}
