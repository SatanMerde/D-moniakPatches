package com.dmoniak.patches.hungryshark

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_HUNGRY_SHARK_WORLD
import java.util.logging.Logger

@Suppress("unused")
val freeShoppingPatch = bytecodePatch(
    name = "Free Shopping",
    description = "Unlocks shop items and in-app purchases in Hungry Shark World by intercepting Google Play Billing transactions for free.",
) {
    compatibleWith(COMPATIBILITY_HUNGRY_SHARK_WORLD)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        logger.info("Executing Free Shopping / In-App Billing patch for Hungry Shark World...")

        val count = applyBillingCallSitePatch(logger)
        logger.info("Free Shopping: patched $count launchBillingFlow call site(s).")
        logger.info("Free Shopping patch execution finished.")
    }
}

/**
 * Scans all game classes (excluding known SDK packages) for call sites of
 * BillingClient.launchBillingFlow and prepends a hook that immediately returns
 * an OK BillingResult via the HungrySharkBillingHelper extension.
 *
 * This approach avoids modifying BillingClient implementation methods entirely,
 * preventing any Dalvik verifier issues during class loading at startup.
 */
private fun BytecodePatchContext.applyBillingCallSitePatch(logger: Logger): Int {
    var count = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        // Skip SDK/framework classes that don't contain game code
        if (tl.contains("androidx") ||
            tl.contains("android/support") ||
            tl.contains("com/android/billingclient") ||
            tl.contains("com/google/android") ||
            tl.contains("com/applovin") ||
            tl.contains("com/unity3d") ||
            tl.contains("com/ironsource") ||
            tl.contains("okhttp") ||
            tl.contains("retrofit")
        ) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods) {
            val impl = method.implementation ?: continue

            val instructions = impl.instructions.toList()
            var didPatch = false

            for ((index, insn) in instructions.withIndex()) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.name != "launchBillingFlow") continue
                if (ref.returnType != "Lcom/android/billingclient/api/BillingResult;") continue

                // Extract registers from the call site instruction
                val clientReg: Int
                val activityReg: Int
                val paramsReg: Int

                when (insn) {
                    is BuilderInstruction35c -> {
                        if (insn.registerCount < 3) continue
                        clientReg = insn.registerC
                        activityReg = insn.registerD
                        paramsReg = insn.registerE
                    }
                    is BuilderInstruction3rc -> {
                        clientReg = insn.startRegister
                        activityReg = insn.startRegister + 1
                        paramsReg = insn.startRegister + 2
                    }
                    else -> continue
                }

                try {
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    // Inject BEFORE the original launchBillingFlow call site
                    // We call our helper, then skip the real call by jumping back
                    // with the result already on the stack. But since we need to
                    // properly replace the result, we inject an unconditional return.
                    // 
                    // Strategy: insert our call + return-object BEFORE the original call.
                    // The original call becomes dead code (unreachable), which is valid.
                    mutableMethod.addInstructions(
                        index,
                        """
                        invoke-static {v$clientReg, v$activityReg, v$paramsReg}, Lcom/dmoniak/patches/extension/HungrySharkBillingHelper;->handleLaunchBillingFlow(Ljava/lang/Object;Landroid/app/Activity;Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v$clientReg
                        return-object v$clientReg
                        """.trimIndent(),
                    )
                    count++
                    didPatch = true
                } catch (e: Exception) {
                    logger.warning("Failed to patch call site in ${classDef.type}.${method.name}: ${e.message}")
                }
                if (didPatch) break // one patch per method is sufficient
            }
        }
    }
    return count
}
