package com.dmoniak.patches.truecaller

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_TRUECALLER
import java.util.logging.Logger

@Suppress("unused")
val truecallerAdFreePatch = bytecodePatch(
    name = "Ad-Free & Clean Dialer - Truecaller (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Removes intrusive post-call ads, banner ads inside the call history and dialer tabs, and promotional Gold upsells.",
) {
    compatibleWith(COMPATIBILITY_TRUECALLER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeTruecallerAdFreeLogic(logger)
    }
}

fun BytecodePatchContext.executeTruecallerAdFreeLogic(logger: Logger) {
    logger.info("Executing Ad-Free & Clean Dialer patch for Truecaller...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("com/google")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // 1. Disable post-call and dialer banner ads
            if (!isStatic && (
                mName == "isadenabled" ||
                mName == "shouldshowaftercallad" ||
                mName == "isbanneradvisible" ||
                mName == "isaftercalladready" ||
                mName == "haspromobanner" ||
                mName == "shouldshowpromocard"
            ) && retType == "Z") {
                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/4 v0, 0x0
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Truecaller AdFree] Disabled ad trigger: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Truecaller AdFree] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Truecaller AdFree] Total dialer ad hooks applied: $hookedPoints")
}
