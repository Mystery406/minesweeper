package dev.hikari.minesweeper.session

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferencesSerializer
import java.io.File
import okio.FileSystem
import okio.Path.Companion.toPath

private val desktopGamePreferences: GamePreferences by lazy {
    DataStoreGamePreferences(
        dataStore = createPreferencesDataStore(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                producePath = { desktopDataStoreFile().absolutePath.toPath() },
            ),
        ),
    )
}

fun createGamePreferences(): GamePreferences = desktopGamePreferences

private fun desktopDataStoreFile(): File {
    val userHome = File(checkNotNull(System.getProperty("user.home")))
    val osName = System.getProperty("os.name").orEmpty().lowercase()
    val applicationDirectory = when {
        "win" in osName -> {
            val appData = System.getenv("APPDATA")
                ?.takeIf(String::isNotBlank)
                ?.let(::File)
                ?: userHome.resolve("AppData/Roaming")
            appData.resolve("Minesweeper")
        }
        "mac" in osName -> userHome.resolve("Library/Application Support/Minesweeper")
        else -> {
            val configHome = System.getenv("XDG_CONFIG_HOME")
                ?.takeIf(String::isNotBlank)
                ?.let(::File)
                ?: userHome.resolve(".config")
            configHome.resolve("minesweeper")
        }
    }
    return applicationDirectory.resolve(DATA_STORE_FILE_NAME)
}
