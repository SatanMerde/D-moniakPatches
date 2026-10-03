package com.dmoniak.patches.undercover

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_UNDERCOVER
import java.util.logging.Logger

@Suppress("unused")
val undercoverAmoledThemeAndPrivacyPatch = bytecodePatch(
    name = "AMOLED Dark Theme & Privacy - Undercover (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Injects pure OLED pitch black (#000000) for party night sessions and strips analytics telemetry (Firebase, Facebook SDK, AppsFlyer) in Undercover.",
) {
    compatibleWith(COMPATIBILITY_UNDERCOVER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeUndercoverThemeAndPrivacyLogic(logger)
    }
}

fun BytecodePatchContext.executeUndercoverThemeAndPrivacyLogic(logger: Logger) {
    logger.info("Executing AMOLED Dark Theme & Privacy patch for Undercover...")
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

            // Hook dark theme / AMOLED mode enforcement
            if (!isStatic && (
                mName == "isdarkthemeenabled" ||
                mName == "isnightmodeactive" ||
                mName == "shouldusedarktheme"
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

            // Hook background color to pure pitch black (#000000 = 0xff000000 = -16777216)
            if (!isStatic && (
                mName == "getbackgroundcolor" ||
                mName == "getsurfacecolor" ||
                mName == "getdefaultthemebackground"
            ) && retType == "I") {
                mutableClass.findMutableMethodOf(method)?.let { mutableMethod ->
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/high16 v0, -0x1000000
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                }
            }

            // Suppress analytics telemetry dispatchers
            if (!isStatic && (
                mName == "logevent" ||
                mName == "sendanalytics" ||
                mName == "trackevent" ||
                mName == "reportanalyticsevent"
            ) && retType == "V") {
                mutableClass.findMutableMethodOf(method)?.let { mutableMethod ->
                    mutableMethod.addInstructions(
                        0,
                        """
                        return-void
                        """.trimIndent()
                    )
                    hookedPoints++
                }
            }
        }
    }

    logger.info("Hooked $hookedPoints theme and telemetry points in Undercover.")
}
