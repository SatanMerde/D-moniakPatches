package com.dmoniak.patches.virtualmaster

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.shared.BillingHookHelper.executeGooglePlayBillingBypass
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_VIRTUAL_MASTER
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_VIRTUAL_MASTER_ALT
import com.dmoniak.patches.shared.replaceMethod
import java.util.logging.Logger

@Suppress("unused")
val virtualMasterUnlockProPatch = bytecodePatch(
    name = "Unlock Pro & Multi-Instance - Virtual Master (Experimental)",
    description = "⚠️ [Experimental / Untested] Unlocks Virtual Master Pro edition, allowing unlimited virtual Android instances, root permission toggles, customized device ID spoofing, and advanced backup/restore by hooking VIP/Pro status models and billing interfaces.",
) {
    compatibleWith(COMPATIBILITY_VIRTUAL_MASTER, COMPATIBILITY_VIRTUAL_MASTER_ALT)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeVirtualMasterUnlockProLogic(logger)
    }
}

fun BytecodePatchContext.executeVirtualMasterUnlockProLogic(logger: Logger) {
    logger.info("Executing Unlock Pro patch for Virtual Master...")
    var hookedPoints = executeGooglePlayBillingBypass(logger, "VirtualMaster")

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
                mName == "ispro" || mName == "isvip" || mName == "ispremium" ||
                mName == "issubscribed" || mName == "haspro" || mName == "isvipuser" ||
                mName == "isvalidvip" || mName == "isproversion" || mName == "checkpro"
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
                    logger.fine("[Virtual Master Pro] Hooked Pro status getter: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Virtual Master Pro] Failed to hook ${classDef.type}->${method.name}: ${e.message}")
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
                    logger.fine("[Virtual Master Pro] Hooked expiration check: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Virtual Master Pro] Failed to hook ${classDef.type}->${method.name}: ${e.message}")
                }
            }

            // 3. Pro / VIP Level -> 2 (Premium/Pro tier)
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
                    logger.fine("[Virtual Master Pro] Hooked tier level getter: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Virtual Master Pro] Failed to hook ${classDef.type}->${method.name}: ${e.message}")
                }
            }

            // 4. Expiration Timestamp -> Far future
            if (!isStatic && pTypes.isEmpty() && retType == "J" && (
                mName == "getexpiretime" || mName == "getexpiredate" || mName == "getvipexpiretime"
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
                    logger.fine("[Virtual Master Pro] Hooked expiration timestamp: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Virtual Master Pro] Failed to hook ${classDef.type}->${method.name}: ${e.message}")
                }
            }

            // 5. Multi-Instance clone limits (return high limit e.g. 100)
            if (!isStatic && pTypes.isEmpty() && retType == "I" && (
                mName == "getmaxinstancecount" || mName == "getinstancelimit" || mName == "getmaxclones"
            )) {
                try {
                    mutableClass.replaceMethod(
                        method,
                        registerCount = 2,
                        smaliCode = """
                        const/16 v0, 0x64
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.fine("[Virtual Master Pro] Hooked instance limit: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[Virtual Master Pro] Failed to hook ${classDef.type}->${method.name}: ${e.message}")
                }
            }
        }
    }

    logger.info("[Virtual Master Pro] Total Pro hooks applied: $hookedPoints")
}
