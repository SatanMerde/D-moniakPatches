package com.dmoniak.patches.pinterest

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_PINTEREST
import java.util.logging.Logger

@Suppress("unused")
val pinterestAdBlockPatch = bytecodePatch(
    name = "Remove Promoted Pins & Shopping Ads - Pinterest (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips sponsored promoted pins, affiliate shopping product carousels, and paid recommendations from Pinterest home and search feeds.",
) {
    compatibleWith(COMPATIBILITY_PINTEREST)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executePinterestAdBlockLogic(logger)
    }
}

fun BytecodePatchContext.executePinterestAdBlockLogic(logger: Logger) {
    logger.info("Executing Remove Promoted Pins & Shopping Ads patch for Pinterest...")
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

            // 1. Promoted / sponsored pin flags
            if (!isStatic && (
                mName == "ispromoted" ||
                mName == "ispromotedpin" ||
                mName == "issponsored" ||
                mName == "hasshoppingaffiliate" ||
                mName == "ispaidpartnership" ||
                mName == "isadfeeditem" ||
                mName == "iscommercialpin"
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
                    logger.info("[Pinterest Ads] Disabled promoted pin indicator: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Pinterest Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Pinterest Ads] Total promoted pin removal hooks applied: $hookedPoints")
}
