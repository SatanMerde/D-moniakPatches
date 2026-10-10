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
    description = "⚠️ [En cours de développement / Non testé] Unlocks the full game, all oceanic abyss chapters, and in-app purchase verification in Silt by neutralizing Google Play App Signing PairIP protection, Play Integrity remediation dialogs, and Google Play Billing. (Note: Silt requires the full XAPK/OBB bundle with UnityDataAssetPack to run).",
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

    // 2. Neutralize Google Play App Signing & Play Integrity PAIRIP Protection
    // (PairIP is Google Play's injected protector that triggers the "To continue using SILT, get it on Google Play..." overlay)
    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()

        if (tl.contains("pairip")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                val mName = method.name
                if (mName == "<init>" || mName == "<clinit>") continue
                val mn = mName.lowercase()
                val retType = method.returnType

                // 2a. SignatureCheck (verifyIntegrity / verifySignatureMatches)
                if (tl.contains("signaturecheck") && (mn.contains("verify") || mn.contains("check"))) {
                    if (retType == "V") {
                        try {
                            replaceMethod(
                                method = method,
                                registerCount = 3,
                                smaliCode = "return-void"
                            )
                            hookedPoints++
                            logger.info("[Silt PairIP] Neutralized SignatureCheck->$mName (void)")
                        } catch (e: Exception) {
                            logger.fine("[Silt PairIP] Failed to hook $mName: ${e.message}")
                        }
                    } else if (retType == "Z") {
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
                            logger.info("[Silt PairIP] Spoofed SignatureCheck->$mName -> true")
                        } catch (e: Exception) {
                            logger.fine("[Silt PairIP] Failed to hook $mName: ${e.message}")
                        }
                    }
                }

                // 2b. LicenseContentProvider.onCreate -> return true without initializing LicenseClient
                if (tl.contains("licensecontentprovider") && mn == "oncreate") {
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
                        logger.info("[Silt PairIP] Neutralized LicenseContentProvider.onCreate -> true (suppressed client init)")
                    } catch (e: Exception) {
                        logger.fine("[Silt PairIP] Failed to hook provider onCreate: ${e.message}")
                    }
                }

                // 2c. LicenseClient (initializeLicenseCheck, startPaywallActivity, etc.)
                if (tl.contains("licenseclient")) {
                    if (mn in listOf(
                        "initializelicensecheck",
                        "connecttolicensingservice",
                        "checklicenseinternal",
                        "startpaywallactivity",
                        "starterrordialogactivity",
                        "processresponse",
                        "reportsuccessfullicensecheck",
                        "handleerror",
                        "retryorthrow"
                    )) {
                        try {
                            if (retType == "V") {
                                replaceMethod(
                                    method = method,
                                    registerCount = 3,
                                    smaliCode = "return-void"
                                )
                                hookedPoints++
                                logger.info("[Silt PairIP] Neutralized LicenseClient->$mName (void)")
                            }
                        } catch (e: Exception) {
                            logger.fine("[Silt PairIP] Failed to hook LicenseClient->$mName: ${e.message}")
                        }
                    } else if (mn == "performlocalinstallercheck") {
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
                            logger.info("[Silt PairIP] Spoofed LicenseClient.performLocalInstallerCheck -> true")
                        } catch (e: Exception) {
                            logger.fine("[Silt PairIP] Failed to hook performLocalInstallerCheck: ${e.message}")
                        }
                    } else if (mn == "createcloseappintentorexitifappinbackground") {
                        try {
                            replaceMethod(
                                method = method,
                                registerCount = 3,
                                smaliCode = """
                                const/4 v0, 0x0
                                return-object v0
                                """.trimIndent()
                            )
                            hookedPoints++
                            logger.info("[Silt PairIP] Suppressed close app intent")
                        } catch (e: Exception) {
                            logger.fine("[Silt PairIP] Failed close app intent hook: ${e.message}")
                        }
                    }
                }

                // 2d. LicenseActivity (onStart, showPaywallAndCloseApp, closeApp, etc.)
                if (tl.contains("licenseactivity")) {
                    if (mn in listOf(
                        "onstart",
                        "showpaywallandcloseapp",
                        "showerrordialog",
                        "logandshowerrordialog",
                        "closeapp",
                        "finishandremovetask"
                    )) {
                        try {
                            if (retType == "V") {
                                replaceMethod(
                                    method = method,
                                    registerCount = 3,
                                    smaliCode = "return-void"
                                )
                                hookedPoints++
                                logger.info("[Silt PairIP] Neutralized LicenseActivity->$mName (void)")
                            }
                        } catch (e: Exception) {
                            logger.fine("[Silt PairIP] Failed to hook LicenseActivity->$mName: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    // 3. Hook Play Integrity API Remediation Dialogs (bypasses "Get this game from Play" prompt)
    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()

        val isIntegrityClass = tl.contains("play/core/integrity") ||
            tl.contains("play/integrity") ||
            tl.contains("integritydialog") ||
            tl.contains("standardintegrity") ||
            tl.contains("integritytoken")

        if (isIntegrityClass) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                val mNameLower = method.name.lowercase()

                if (mNameLower.contains("showdialog")) {
                    try {
                        if (method.returnType == "Lcom/google/android/gms/tasks/Task;") {
                            // Returns Task<Integer> with result 0 (INTEGRITY_DIALOG_RESPONSE_CODE_SUCCESS)
                            replaceMethod(
                                method = method,
                                registerCount = 4,
                                smaliCode = """
                                const/4 v0, 0x0
                                invoke-static {v0}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                                move-result-object v0
                                invoke-static {v0}, Lcom/google/android/gms/tasks/Tasks;->forResult(Ljava/lang/Object;)Lcom/google/android/gms/tasks/Task;
                                move-result-object v0
                                return-object v0
                                """.trimIndent()
                            )
                            hookedPoints++
                            logger.info("[Silt Integrity] Neutralized showDialog (Task<Integer>) in: $type->${method.name}")
                        } else if (method.returnType == "V") {
                            replaceMethod(
                                method = method,
                                registerCount = 3,
                                smaliCode = "return-void"
                            )
                            hookedPoints++
                            logger.info("[Silt Integrity] Neutralized showDialog (void) in: $type->${method.name}")
                        } else if (method.returnType == "I") {
                            replaceMethod(
                                method = method,
                                registerCount = 3,
                                smaliCode = """
                                const/4 v0, 0x0
                                return v0
                                """.trimIndent()
                            )
                            hookedPoints++
                            logger.info("[Silt Integrity] Neutralized showDialog (int) in: $type->${method.name}")
                        }
                    } catch (e: Exception) {
                        logger.fine("[Silt Integrity] Failed showDialog hook: ${e.message}")
                    }
                }
            }
        }
    }

    // 3. Hook Google Play License Verification Library (LVL) & Installer Verification
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

    // 4. Hook game unlock, Play Pass, and chapter access checks
    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("com/google")) return@classDefForEach

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType
            val pTypes = method.parameterTypes

            // Full game license checks & Play Pass entitlement
            if (!isStatic && (
                mName == "isfullgameunlocked" ||
                mName == "isgameunlocked" ||
                mName == "haspurchasedfullgame" ||
                mName == "isfullgamepurchased" ||
                mName == "isfullversion" ||
                mName == "canaccessfullgame" ||
                mName == "isunlocked" ||
                mName == "hasfullaccess" ||
                mName == "isplaypasssubscribed" ||
                mName == "hasplaypass" ||
                mName == "isgoogleplaypass" ||
                mName == "isplaypass"
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
                    logger.info("[Silt Full Game] Unlocked full game / Play Pass check in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.fine("[Silt Full Game] Failed to hook ${method.name}: ${e.message}")
                }
            }

            // Suppress trial / demo mode flags
            if (!isStatic && (
                mName == "istrial" ||
                mName == "isdemo" ||
                mName == "isfreeversion" ||
                mName == "islimitedversion" ||
                mName == "requiretrial"
            ) && retType == "Z" && pTypes.isEmpty()) {
                try {
                    replaceMethod(
                        method = method,
                        registerCount = 3,
                        smaliCode = """
                        const/4 v0, 0x0
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                    logger.info("[Silt Full Game] Suppressed trial/demo flag in: ${classDef.type}->${method.name}")
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
