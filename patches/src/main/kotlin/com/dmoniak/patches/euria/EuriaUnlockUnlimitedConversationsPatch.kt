package com.dmoniak.patches.euria

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_EURIA
import java.util.logging.Logger

@Suppress("unused")
val euriaUnlockUnlimitedConversationsPatch = bytecodePatch(
    name = "Unlock Unlimited Conversations & Memory - Euria AI (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Bypasses daily interaction quotas, unlocks persistent discussion history, and enables extended memory context in Euria AI.",
) {
    compatibleWith(COMPATIBILITY_EURIA)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeEuriaUnlockUnlimitedConversationsLogic(logger)
    }
}

fun BytecodePatchContext.executeEuriaUnlockUnlimitedConversationsLogic(logger: Logger) {
    logger.info("Executing Unlock Unlimited Conversations patch for Euria AI...")
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

            // 1. Quota reached check -> false
            if (!isStatic && (
                mName == "isdailyquotareached" ||
                mName == "islimitreached" ||
                mName == "hasexceededfreelimit" ||
                mName == "shouldshowsubscriptionwall"
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
                    logger.info("[Euria Quota] Bypassed quota check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Euria Quota] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // 2. Premium / kSuite Pro subscription status flags -> true
            if (!isStatic && (
                mName == "ispro" ||
                mName == "ispremium" ||
                mName == "hasunlimitedaccess" ||
                mName == "hasksuitepro" ||
                mName == "canuseextendedcontext" ||
                mName == "canaccessadvancedmodel"
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
                    logger.info("[Euria Pro] Forced Pro/Extended status in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Euria Pro] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[Euria Pro] Total unlimited hooks applied: $hookedPoints")
}
