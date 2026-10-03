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
val undercoverUnlockAllWordPacksPatch = bytecodePatch(
    name = "Unlock All Word Packs & Premium - Undercover (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Hooks in-app purchase verification and license state to unlock all premium word packs (Adult 18+, Pop Culture, Geek, Cinema, Science & History) and remove all paywalls.",
) {
    compatibleWith(COMPATIBILITY_UNDERCOVER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeUndercoverUnlockWordPacksLogic(logger)
    }
}

fun BytecodePatchContext.executeUndercoverUnlockWordPacksLogic(logger: Logger) {
    logger.info("Executing Unlock All Word Packs & Premium patch for Undercover...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        // 1. Hook Google Play Billing SDK primitives directly
        if (type.contains("com/android/billingclient/api/")) {
            for (method in classDef.methods.toList()) {
                val mName = method.name
                val retType = method.returnType

                // BillingClient.isReady() -> true
                if (mName == "isReady" && retType == "Z") {
                    try {
                        val mm = mutableClass.findMutableMethodOf(method)
                        mm?.addInstructions(
                            0,
                            """
                            const/4 v0, 0x1
                            return v0
                            """.trimIndent()
                        )
                        hookedPoints++
                    } catch (e: Exception) {
                        logger.fine("Failed to hook $mName: ${e.message}")
                    }
                }

                // BillingResult.getResponseCode() -> OK (0)
                if (mName == "getResponseCode" && retType == "I") {
                    try {
                        val mm = mutableClass.findMutableMethodOf(method)
                        mm?.addInstructions(
                            0,
                            """
                            const/4 v0, 0x0
                            return v0
                            """.trimIndent()
                        )
                        hookedPoints++
                    } catch (e: Exception) {
                        logger.fine("Failed to hook $mName: ${e.message}")
                    }
                }

                // Purchase.getPurchaseState() -> PURCHASED (1)
                if (mName == "getPurchaseState" && retType == "I") {
                    try {
                        val mm = mutableClass.findMutableMethodOf(method)
                        mm?.addInstructions(
                            0,
                            """
                            const/4 v0, 0x1
                            return v0
                            """.trimIndent()
                        )
                        hookedPoints++
                    } catch (e: Exception) {
                        logger.fine("Failed to hook $mName: ${e.message}")
                    }
                }

                // Purchase.isAcknowledged() -> true
                if (mName == "isAcknowledged" && retType == "Z") {
                    try {
                        val mm = mutableClass.findMutableMethodOf(method)
                        mm?.addInstructions(
                            0,
                            """
                            const/4 v0, 0x1
                            return v0
                            """.trimIndent()
                        )
                        hookedPoints++
                    } catch (e: Exception) {
                        logger.fine("Failed to hook $mName: ${e.message}")
                    }
                }
            }
        }

        // 2. Obfuscation-resistant hook: inspect methods referencing Undercover's real purchase constants
        if (!tl.startsWith("landroid/") && !tl.startsWith("lkotlin/")) {
            for (method in classDef.methods.toList()) {
                val impl = method.implementation ?: continue
                val retType = method.returnType

                var referencesPurchaseKey = false
                for (instruction in impl.instructions) {
                    if (instruction is ReferenceInstruction) {
                        val ref = instruction.reference
                        if (ref is StringReference) {
                            val str = ref.string
                            if (
                                str.startsWith("BOUGHT_STATUS_") ||
                                str == "is_bought_purchase_1" ||
                                str == "PREMIUM" ||
                                str == "premium" ||
                                str == "is_adfree" ||
                                str.contains("online_create_game_premium")
                            ) {
                                referencesPurchaseKey = true
                                break
                            }
                        }
                    }
                }

                if (referencesPurchaseKey && retType == "Z") {
                    try {
                        val mm = mutableClass.findMutableMethodOf(method)
                        mm?.addInstructions(
                            0,
                            """
                            const/4 v0, 0x1
                            return v0
                            """.trimIndent()
                        )
                        hookedPoints++
                    } catch (e: Exception) {
                        logger.fine("Failed to hook purchase method ${method.name}: ${e.message}")
                    }
                }
            }
        }
    }

    logger.info("Hooked $hookedPoints word pack and premium checkpoints in Undercover.")
}
