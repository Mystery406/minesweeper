package dev.hikari.minesweeper.session

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences

internal const val DATA_STORE_FILE_NAME = "minesweeper.preferences_pb"

internal fun createPreferencesDataStore(
    storage: Storage<Preferences>,
): DataStore<Preferences> = DataStoreFactory.create(
    storage = storage,
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
