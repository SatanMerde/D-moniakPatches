package com.dmoniak.patches.capcut

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.shared.BillingHookHelper.executeGooglePlayBillingBypass
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_CAPCUT
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_CAPCUT_ALT
import com.dmoniak.patches.shared.replaceMethod
import java.util.logging.Logger

@Suppress("unused")
val capCutUnlockProPatch = bytecodePatch(
    name = "Unlock Pro & VIP Effects - CapCut (Experimental)",
    description = "⚠️ [Experimental / Untested] Unlocks CapCut Pro filters, VIP transitions, premium audio effects, high-bitrate export settings, and removes default ending watermark prompts by hooking VIP user entitlements and Google Play Billing.",
) {
    compatibleWith(COMPATIBILITY_CAPCUT, COMPATIBILITY_CAPCUT_ALT)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeCapCutUnlockProLogic(logger)
    }
}

fun BytecodePatchContext.executeCapCutUnlockProLogic(logger: Logger) {
    logger.info("Executing Unlock Pro & VIP Effects patch for CapCut...")
    var hookedPoints = executeGooglePlayBillingBypass(logger, "CapCut")

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.startsWith("lkotlin/") || tl.startsWith("landroidx/") || tl.startsWith("landroid/")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType
            val pTypes = method.parameterTypes

            // 1. Pro / VIP status checks -> true
            if (!isStatic && pTypes.isEmpty() && retType == "Z" && (
                mName == "ispro" || mName == "isvip" || mName == "isprouser" ||
                mName == "isvipuser" || mName == "haspro" || mName == "hasvip" ||
                mName == "isvipmember" || mName == "ispromember" || mName == "checkvipstatus" ||
                mName == "issubscriptionactive" || mName == "isproversion" || mName == "isvalidvip"
            )) {
                try {
                    mutableClass.replaceMethod(
                        method,
                        registerCount = 2,
                        smaliCode = """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.fine("[CapCut Pro] Hooked Pro status getter: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CapCut Pro] Failed to hook ${classDef.type}->${method.name}: ${e.message}")
                }
            }

            // 2. VIP / Subscription Expiration -> false
            if (!isStatic && pTypes.isEmpty() && retType == "Z" && (
                mName == "isexpired" || mName == "isvipexpired" || mName == "isproexpired"
            )) {
                try {
                    mutableClass.replaceMethod(
                        method,
                        registerCount = 2,
                        smaliCode = """
                        const/4 v0, 0x0
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.fine("[CapCut Pro] Hooked expiration check: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CapCut Pro] Failed to hook ${classDef.type}->${method.name}: ${e.message}")
                }
            }

            // 3. Pro / VIP Level / Member Type -> 2 (Pro tier)
            if (!isStatic && pTypes.isEmpty() && retType == "I" && (
                mName == "getviplevel" || mName == "getprolevel" || mName == "getusertype" ||
                mName == "getviptype" || mName == "getmembertype"
            )) {
                try {
                    mutableClass.replaceMethod(
                        method,
                        registerCount = 2,
                        smaliCode = """
                        const/4 v0, 0x2
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.fine("[CapCut Pro] Hooked tier level getter: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CapCut Pro] Failed to hook ${classDef.type}->${method.name}: ${e.message}")
                }
            }

            // 4. Expiration Timestamp -> Far future (0x7fffffffffffffffL)
            if (!isStatic && pTypes.isEmpty() && retType == "J" && (
                mName == "getexpiretime" || mName == "getexpiredate" || mName == "getvipexpiretime" ||
                mName == "getproexpiretime"
            )) {
                try {
                    mutableClass.replaceMethod(
                        method,
                        registerCount = 3,
                        smaliCode = """
                        const-wide v0, 0x7fffffffffffffffL
                        return-wide v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.fine("[CapCut Pro] Hooked expiration timestamp: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CapCut Pro] Failed to hook ${classDef.type}->${method.name}: ${e.message}")
                }
            }

            // 5. Pro Feature / Effect access checks -> true
            if (!isStatic && retType == "Z" && (
                mName == "isprofeature" || mName == "isfeatureunlocked" || mName == "canuseproeffect" ||
                mName == "isprotemplate" || mName == "canexportin4k"
            )) {
                try {
                    mutableClass.replaceMethod(
                        method,
                        registerCount = 2,
                        smaliCode = """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.fine("[CapCut Pro] Hooked feature access check: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[CapCut Pro] Failed to hook ${classDef.type}->${method.name}: ${e.message}")
                }
            }
        }
    }

    logger.info("[CapCut Pro] Total Pro & VIP hooks applied: $hookedPoints")
}
