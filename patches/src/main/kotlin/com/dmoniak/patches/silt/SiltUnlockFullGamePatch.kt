package com.dmoniak.patches.silt

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.replaceMethod
import com.dmoniak.patches.shared.BillingHookHelper.executeGooglePlayBillingBypass
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SILT
import java.util.logging.Logger

@Suppress("unused")
val siltUnlockFullGamePatch = bytecodePatch(
    name = "Unlock Full Game - Silt (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks the full game, all oceanic abyss chapters, and in-app purchase verification in Silt by hooking Google Play Billing and full game license verification checks.",
) {
    compatibleWith(COMPATIBILITY_SILT)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeSiltUnlockFullGameLogic(logger)
    }
}

fun BytecodePatchContext.executeSiltUnlockFullGameLogic(logger: Logger) {
    logger.info("Executing Unlock Full Game patch for Silt (com.snapbreak.silt)...")
    var hookedPoints = 0

    // 1. Hook Google Play Billing Client SDK and local purchase listeners
    hookedPoints += executeGooglePlayBillingBypass(logger, "Silt")

    // 2. Hook Google Play License Verification Library (LVL) & Installer Verification
    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()

        // Google Play LVL Licensing (LicenseChecker / LicenseCheckerCallback)
        val implementsCallback = classDef.interfaces.any { it.contains("LicenseCheckerCallback") }
        if (implementsCallback || tl.contains("licensing") || tl.contains("licensechecker")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                val mName = method.name

                // Hook checkAccess(LicenseCheckerCallback) -> force immediate allow(256)
                if (mName == "checkAccess" && method.parameterTypes.size == 1) {
                    val cbType = method.parameterTypes[0]
                    if (cbType.contains("Callback") || cbType.contains("Listener")) {
                        try {
                            // Total registers = 4: v0, v1 (locals), p0 = v2, p1 = v3 (cb)
                            replaceMethod(
                                method = method,
                                registerCount = 4,
                                smaliCode = """
                                const/16 v0, 0x100
                                invoke-interface {p1, v0}, $cbType->allow(I)V
                                return-void
                                """.trimIndent()
                            )
                            hookedPoints++
                            logger.info("[Silt LVL] Hooked checkAccess to force allow(0x100) in: $type->$mName")
                        } catch (e: Exception) {
                            logger.fine("[Silt LVL] Failed to hook checkAccess: ${e.message}")
                        }
                    }
                }

                // Neutralize dontAllow(int reason) to prevent license denial
                if (mName == "dontAllow" && method.returnType == "V") {
                    try {
                        replaceMethod(
                            method = method,
                            registerCount = 3,
                            smaliCode = "return-void"
                        )
                        hookedPoints++
                        logger.info("[Silt LVL] Neutralized dontAllow check in: $type->$mName")
                    } catch (e: Exception) {
                        logger.fine("[Silt LVL] Failed to hook dontAllow: ${e.message}")
                    }
                }

                // Neutralize applicationError(int errorCode)
                if (mName == "applicationError" && method.returnType == "V") {
                    try {
                        replaceMethod(
                            method = method,
                            registerCount = 3,
                            smaliCode = "return-void"
                        )
                        hookedPoints++
                        logger.info("[Silt LVL] Neutralized applicationError in: $type->$mName")
                    } catch (e: Exception) {
                        logger.fine("[Silt LVL] Failed to hook applicationError: ${e.message}")
                    }
                }
            }
        }

        // Spoof installer source to Google Play (com.android.vending)
        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val mName = method.name.lowercase()
            if ((mName == "getinstallerpackagename" || mName == "getinstallsource") &&
                method.returnType == "Ljava/lang/String;" && method.parameterTypes.size <= 1
            ) {
                try {
                    replaceMethod(
                        method = method,
                        registerCount = 3,
                        smaliCode = """
                        const-string v0, "com.android.vending"
                        return-object v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Silt Installer] Spoofed installer package name to com.android.vending in: $type->${method.name}")
                } catch (e: Exception) {
                    logger.fine("[Silt Installer] Failed installer hook: ${e.message}")
                }
            }
        }
    }

    // 3. Hook game unlock and chapter access checks
    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("com/google")) return@classDefForEach

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType
            val pTypes = method.parameterTypes

            // Full game license checks
            if (!isStatic && (
                mName == "isfullgameunlocked" ||
                mName == "isgameunlocked" ||
                mName == "haspurchasedfullgame" ||
                mName == "isfullgamepurchased" ||
                mName == "isfullversion" ||
                mName == "canaccessfullgame" ||
                mName == "isunlocked" ||
                mName == "hasfullaccess"
            ) && retType == "Z" && pTypes.isEmpty()) {
                try {
                    replaceMethod(
                        method = method,
                        registerCount = 3,
                        smaliCode = """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Silt Full Game] Unlocked full game check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.fine("[Silt Full Game] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // Chapter / level access checks (with int chapter index or no params)
            if (!isStatic && (
                mName == "ischapterunlocked" ||
                mName == "islevelunlocked" ||
                mName == "canplaychapter"
            ) && retType == "Z" && pTypes.size <= 1) {
                try {
                    replaceMethod(
                        method = method,
                        registerCount = 4,
                        smaliCode = """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Silt Full Game] Unlocked chapter check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.fine("[Silt Full Game] Failed chapter hook ${method.name}: ${e.message}")
                }
            }
        }
    }

    logger.info("[Silt Full Game] Total hooks applied: $hookedPoints")
}
