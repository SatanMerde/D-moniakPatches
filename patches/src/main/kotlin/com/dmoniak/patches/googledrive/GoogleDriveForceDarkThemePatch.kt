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
    name = "Force Dark Theme System-Wide - Google Drive (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Forces pure OLED dark theme (#000000) across all Google Drive screens including file browser, folder views, sharing dialogs, and document detail drawers regardless of system theme.",
) {
    compatibleWith(COMPATIBILITY_GOOGLE_DRIVE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeGoogleDriveForceDarkThemeLogic(logger)
    }
}

fun BytecodePatchContext.executeGoogleDriveForceDarkThemeLogic(logger: Logger) {
    logger.info("Executing Force Dark Theme System-Wide patch for Google Drive...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // Force dark mode enabled flag
            if (!isStatic && (
                mName == "isdarkmodeenabled" ||
                mName == "shouldusedarktheme" ||
                mName == "isnightmodeactive"
            ) && retType == "Z") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Drive Dark] Forced dark in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Dark] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // Return OLED black for background colors
            if (!isStatic && (
                mName == "getappbackgroundcolor" ||
                mName == "getlistbackgroundcolor" ||
                mName == "getscaffoldbackground"
            ) && retType == "I") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/high16 v0, -0x1000000
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Drive Dark] OLED black injected in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Drive Dark] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Google Drive Force Dark Theme] Total hooks applied: $hookedPoints")
}
