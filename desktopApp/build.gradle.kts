import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.components.resources)
}

compose.desktop {
    application {
        mainClass = "dev.hikari.minesweeper.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            modules("jdk.unsupported")
            packageName = "Minesweeper"
            packageVersion = "1.0.0"

            windows {
                iconFile.set(project.file("src/main/resources/app-icon.ico"))
                shortcut = true
            }
            linux {
                iconFile.set(project.file("src/main/composeResources/drawable/app_icon.png"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/app-icon.icns"))
            }
        }
    }
}
