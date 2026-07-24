package dev.hikari.minesweeper.session

import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.preferences.core.PreferencesSerializer

private val webGamePreferences: GamePreferences by lazy {
    DataStoreGamePreferences(
        dataStore = createPreferencesDataStore(
            storage = WebLocalStorage(
                serializer = PreferencesSerializer,
                name = DATA_STORE_FILE_NAME,
            ),
        ),
    )
}

fun createGamePreferences(): GamePreferences = webGamePreferences
