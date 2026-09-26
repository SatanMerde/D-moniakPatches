package com.dmoniak.patches.terabox

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_TERABOX
import java.util.logging.Logger

@Suppress("unused")
val teraBoxAdBlockDeclutterPatch = bytecodePatch(
    name = "Block Ads & Hide VIP Nags - TeraBox (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips video startup ads, cloud storage interstitial banners, and persistent TeraBox Premium VIP subscription nag popups.",
) {
    compatibleWith(COMPATIBILITY_TERABOX)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeTeraBoxAdBlockDeclutterLogic(logger)
    }
}

fun BytecodePatchContext.executeTeraBoxAdBlockDeclutterLogic(logger: Logger) {
    logger.info("Executing Block Ads & Hide VIP Nags patch for TeraBox...")
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

            // Suppress TeraBox ads (video, banner, interstitials)
            if (!isStatic && (
                mName == "shouldshowad" ||
                mName == "isadvisible" ||
                mName == "isinterstitialloaded" ||
                mName == "shouldplayvideoad" ||
                mName == "shouldshowvipupgradepopup" ||
                mName == "isvippromopopupvisible"
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
                    logger.info("[TeraBox Ads] Suppressed ad/VIP nag in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[TeraBox Ads] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[TeraBox Block Ads & VIP] Total hooks applied: $hookedPoints")
}
