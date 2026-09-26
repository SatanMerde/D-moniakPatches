package com.dmoniak.patches.googledrive

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_GOOGLE_DRIVE
import java.util.logging.Logger

@Suppress("unused")
val googleDriveForceDarkThemePatch = bytecodePatch(
    name = "Force Dark Theme System-Wide - Google Drive",
    description = "Forces Dark Mode (AppCompatDelegate.MODE_NIGHT_YES) across all Google Drive screens, file browsers, and previewers even when the Android system is set to Light Mode.",
) {
    compatibleWith(COMPATIBILITY_GOOGLE_DRIVE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeGoogleDriveForceDarkThemeLogic(logger)
    }
}

fun BytecodePatchContext.executeGoogleDriveForceDarkThemeLogic(logger: Logger) {
    logger.info("Executing Force Dark Theme System-Wide patch for Google Drive...")
    var hookedEntries = 0

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()
        val mutableClass by lazy { mutableClassDefBy(classDef) }

        // Inject setDefaultNightMode(2) in Application.onCreate or Main Activity onCreate
        val superType = classDef.superType ?: ""
        val isAppOrActivity = superType.contains("Application") || superType.contains("Activity") || type.contains("Activity")

        if (isAppOrActivity && tl.contains("com/google/android/apps/docs")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
                val mName = method.name
                val pTypes = method.parameterTypes

                if (!isStatic && mName == "onCreate" && (pTypes.isEmpty() || (pTypes.size == 1 && pTypes[0] == "Landroid/os/Bundle;"))) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const/4 v0, 0x2
                            invoke-static {v0}, Landroidx/appcompat/app/AppCompatDelegate;->setDefaultNightMode(I)V
                            """.trimIndent()
                        )
                        hookedEntries++
                        logger.info("[Drive Dark] Injected setDefaultNightMode(MODE_NIGHT_YES) in ${type}->${mName}")
                    } catch (e: Exception) {
                        logger.warning("[Drive Dark] Failed to inject setDefaultNightMode in ${type}->${mName}: ${e.message}")
                    }
                }
            }
        }
    }
    logger.info("[Google Drive Force Dark Theme] Total entries hooked: $hookedEntries")
}
