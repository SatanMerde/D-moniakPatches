package com.dmoniak.patches.undercover

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_UNDERCOVER
import java.util.logging.Logger

@Suppress("unused")
val undercoverBlockAdsPatch = bytecodePatch(
    name = "Block Ads & Commercials - Undercover (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Strips interstitial video ads between rounds, banner ads in lobby and voting screens, and rewarded video gates by neutralizing ad SDK calls.",
) {
    compatibleWith(COMPATIBILITY_UNDERCOVER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeUndercoverBlockAdsLogic(logger)
    }
}

fun BytecodePatchContext.executeUndercoverBlockAdsLogic(logger: Logger) {
    logger.info("Executing Block Ads & Commercials patch for Undercover...")
    var hookedCount = 0

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        // 1. Hook Google AdMob SDK classes directly
        if (
            type.contains("com/google/android/gms/ads/interstitial/InterstitialAd") ||
            type.contains("com/google/android/gms/ads/AdView") ||
            type.contains("com/google/android/gms/ads/rewarded/RewardedAd") ||
            type.contains("com/google/android/gms/ads/BaseAdView")
        ) {
            for (method in classDef.methods.toList()) {
                val mName = method.name
                val retType = method.returnType

                if ((mName == "show" || mName == "loadAd" || mName == "resume" || mName == "showAd") && retType == "V") {
                    try {
                        val mm = mutableClass.findMutableMethodOf(method)
                        mm?.addInstructions(
                            0,
                            """
                            return-void
                            """.trimIndent()
                        )
                        hookedCount++
                    } catch (e: Exception) {
                        logger.fine("Failed to hook $mName: ${e.message}")
                    }
                }
            }
        }

        // 2. Hook Undercover's own internal ad activity wrapper (Adtznhrcum)
        if (type.contains("com/yanstarstudio/joss/undercover/Adtznhrcum")) {
            for (method in classDef.methods.toList()) {
                val mName = method.name
                val retType = method.returnType

                if ((mName == "start" || mName == "onShow" || mName == "show") && retType == "V") {
                    try {
                        val mm = mutableClass.findMutableMethodOf(method)
                        mm?.addInstructions(
                            0,
                            """
                            return-void
                            """.trimIndent()
                        )
                        hookedCount++
                    } catch (e: Exception) {
                        logger.fine("Failed to hook Adtznhrcum.$mName: ${e.message}")
                    }
                }
            }
        }

        // 3. Obfuscation-resistant hook: inspect methods referencing ad keywords
        if (!tl.startsWith("landroid/") && !tl.startsWith("lkotlin/")) {
            for (method in classDef.methods.toList()) {
                val impl = method.implementation ?: continue
                val retType = method.returnType

                var isAdMethod = false
                for (instruction in impl.instructions) {
                    if (instruction is ReferenceInstruction) {
                        val ref = instruction.reference
                        if (ref is StringReference) {
                            val str = ref.string.lowercase()
                            if (
                                str.contains("interstitial") ||
                                str.contains("admob rewarded ad") ||
                                str.contains("show interstitial ad")
                            ) {
                                isAdMethod = true
                                break
                            }
                        }
                    }
                }

                if (isAdMethod) {
                    if (retType == "Z") {
                        try {
                            val mm = mutableClass.findMutableMethodOf(method)
                            mm?.addInstructions(
                                0,
                                """
                                const/4 v0, 0x0
                                return v0
                                """.trimIndent()
                            )
                            hookedCount++
                        } catch (e: Exception) {
                            logger.fine("Failed to hook ad boolean method ${method.name}: ${e.message}")
                        }
                    } else if (retType == "V") {
                        try {
                            val mm = mutableClass.findMutableMethodOf(method)
                            mm?.addInstructions(
                                0,
                                """
                                return-void
                                """.trimIndent()
                            )
                            hookedCount++
                        } catch (e: Exception) {
                            logger.fine("Failed to hook ad void method ${method.name}: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    logger.info("Hooked $hookedCount ad call points in Undercover.")
}
