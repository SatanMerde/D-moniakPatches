package com.dmoniak.patches.hotstar

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.shared.AdBlockHelper.executeComprehensiveAdBlock
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_HOTSTAR
import com.dmoniak.patches.shared.replaceMethod
import java.util.logging.Logger

@Suppress("unused")
val hotstarAdFreePatch = bytecodePatch(
    name = "Ad-Free & Live Sports Ad-Block - JioHotstar",
    description = "Strips pre-roll, mid-roll, and ad pods during live cricket and sports streams, and eliminates banner and sponsored recommendation cards in JioHotstar.",
) {
    compatibleWith(COMPATIBILITY_HOTSTAR)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeHotstarAdFreeLogic(logger)
    }
}

fun BytecodePatchContext.executeHotstarAdFreeLogic(logger: Logger) {
    logger.info("Executing Ad-Free & Live Sports Ad-Block patch for JioHotstar (in.startv.hotstar)...")
    var hookedPoints = executeComprehensiveAdBlock(logger, "JioHotstar")

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()
        if (tl.startsWith("landroidx/") || tl.startsWith("lkotlin/") || tl.startsWith("ljava/")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        // Target Hotstar video ad controllers, Google IMA SDK, FreeWheel, live match ad insertion, and interstitial pods
        val isAdRelated = tl.contains("ad") || tl.contains("ads") || tl.contains("preroll") ||
                tl.contains("midroll") || tl.contains("adpod") || tl.contains("freewheel") ||
                tl.contains("interactivemedia") || tl.contains("sponsored") || tl.contains("videoplayer") ||
                tl.contains("contentmetadata") || tl.contains("feedwidget")

        if (isAdRelated) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
                val mName = method.name.lowercase()
                val retType = method.returnType

                // 1. Force Ad-Free status checks -> return true
                val isAdFreeCheck = !isStatic && (
                    mName == "isadfree" ||
                    mName == "isadfreeuser" ||
                    mName == "hasadfreeexperience" ||
                    mName == "hasadfreesubscription" ||
                    mName == "isadhocadfree"
                ) && retType == "Z"

                if (isAdFreeCheck) {
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
                        logger.fine("[Hotstar AdBlock] Hooked ad-free check: ${type}->${method.name} -> true")
                    } catch (e: Exception) {
                        logger.fine("[Hotstar AdBlock] Skip ad-free check hook: ${e.message}")
                    }
                }

                // 2. Intercept ad status checks & ad enablement -> return false
                val isAdStatusCheck = !isStatic && (
                    mName == "isad" ||
                    mName == "isadplaying" ||
                    mName == "isadshowing" ||
                    mName == "isliveadplaying" ||
                    mName == "isadbreakplaying" ||
                    mName == "shouldshowads" ||
                    mName == "shouldplayad" ||
                    mName == "canplayad" ||
                    mName == "hasad" ||
                    mName == "hasadpod" ||
                    mName == "hasadbreak" ||
                    mName == "isprerollad" ||
                    mName == "ismidrollad" ||
                    mName == "islinearad" ||
                    mName == "isadsupported" ||
                    mName == "isadsupporteduser" ||
                    mName == "isadsenabled" ||
                    mName == "areadsenabled" ||
                    mName == "isadenabled"
                ) && retType == "Z"

                if (isAdStatusCheck) {
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
                        logger.fine("[Hotstar AdBlock] Hooked ad check: ${type}->${method.name} -> false")
                    } catch (e: Exception) {
                        logger.fine("[Hotstar AdBlock] Skip status check hook: ${e.message}")
                    }
                }

                // 3. Intercept ad trigger / load / show / request methods -> return-void
                val isAdTrigger = !isStatic && (
                    mName == "loadad" ||
                    mName == "showad" ||
                    mName == "playad" ||
                    mName == "schedulead" ||
                    mName == "requestadpod" ||
                    mName == "requestads" ||
                    mName == "submitrequest" ||
                    mName == "startadplayback" ||
                    mName == "onadbreakstarted" ||
                    mName == "onadstarted" ||
                    mName == "handleadplayback" ||
                    mName == "insertad" ||
                    mName == "insertlivead" ||
                    mName == "triggerad"
                ) && retType == "V"

                if (isAdTrigger) {
                    try {
                        mutableClass.replaceMethod(
                            method,
                            registerCount = 2,
                            smaliCode = "return-void"
                        )
                        hookedPoints++
                        logger.fine("[Hotstar AdBlock] Neutralized ad presentation: ${type}->${method.name}")
                    } catch (e: Exception) {
                        logger.fine("[Hotstar AdBlock] Skip trigger hook: ${e.message}")
                    }
                }

                // 4. Intercept ad metadata / cue points / ad tag uri getters -> return null
                val isAdMetadataGetter = !isStatic && (
                    mName == "getadpod" ||
                    mName == "getadbreaks" ||
                    mName == "getadcuepoints" ||
                    mName == "getadtaguri"
                ) && retType.startsWith("L")

                if (isAdMetadataGetter) {
                    try {
                        mutableClass.replaceMethod(
                            method,
                            registerCount = 2,
                            smaliCode = """
                                const/4 v0, 0x0
                                return-object v0
                            """.trimIndent()
                        )
                        hookedPoints++
                        logger.fine("[Hotstar AdBlock] Neutralized ad metadata getter: ${type}->${method.name} -> null")
                    } catch (e: Exception) {
                        logger.fine("[Hotstar AdBlock] Skip metadata getter hook: ${e.message}")
                    }
                }
            }
        }
    }

    logger.info("[JioHotstar AdBlock] Total ad hooks applied: $hookedPoints")
}

