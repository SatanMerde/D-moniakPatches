package com.dmoniak.patches.undercover

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_UNDERCOVER
import java.util.logging.Logger

@Suppress("unused")
val undercoverUnlockAdvancedGameSettingsPatch = bytecodePatch(
    name = "Unlock Advanced Game Settings - Undercover (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks advanced game configuration (custom player counts, exact role distribution sliders for Civilians, Undercover agents, and Mr. White, custom discussion timers, and voting rules).",
) {
    compatibleWith(COMPATIBILITY_UNDERCOVER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeUndercoverUnlockSettingsLogic(logger)
    }
}

fun BytecodePatchContext.executeUndercoverUnlockSettingsLogic(logger: Logger) {
    logger.info("Executing Unlock Advanced Game Settings patch for Undercover...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("google")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // Hook advanced role settings & timer toggles
            if (!isStatic && (
                mName == "isadvancedsettingsunlocked" ||
                mName == "canconfigureroles" ||
                mName == "canadjustroledistribution" ||
                mName == "iscustomtimerunlocked" ||
                mName == "isunlimitedplayersunlocked" ||
                mName == "hasmrwhitesettings"
            ) && retType == "Z") {
                mutableClass.findMutableMethodOf(method)?.let { mutableMethod ->
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                }
            }

            // Hook max allowed players count
            if (!isStatic && (
                mName == "getmaxplayerscount" ||
                mName == "getplayerlimit" ||
                mName == "getmaxallowedplayers"
            ) && retType == "I") {
                mutableClass.findMutableMethodOf(method)?.let { mutableMethod ->
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/16 v0, 0x32
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                }
            }
        }
    }

    logger.info("Hooked $hookedPoints advanced game configuration points in Undercover.")
}
