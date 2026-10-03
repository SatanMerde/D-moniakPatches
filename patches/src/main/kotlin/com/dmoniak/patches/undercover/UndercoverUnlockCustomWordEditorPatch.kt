package com.dmoniak.patches.undercover

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_UNDERCOVER
import java.util.logging.Logger

@Suppress("unused")
val undercoverUnlockCustomWordEditorPatch = bytecodePatch(
    name = "Unlock Custom Words Creator - Undercover (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Unlocks the custom word pack creator allowing players to create, save, and edit unlimited secret word pairs and custom clue databases without subscription restrictions.",
) {
    compatibleWith(COMPATIBILITY_UNDERCOVER)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeUndercoverUnlockCustomWordEditorLogic(logger)
    }
}

fun BytecodePatchContext.executeUndercoverUnlockCustomWordEditorLogic(logger: Logger) {
    logger.info("Executing Unlock Custom Words Creator patch for Undercover...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("google")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType

            // Hook custom words creator gates
            if (!isStatic && (
                mName == "iscustomwordsunlocked" ||
                mName == "cancustomizewords" ||
                mName == "cancreatecustompack" ||
                mName == "iscustompackallowed" ||
                mName == "hascustompackfeature"
            ) && retType == "Z") {
                mutableClass.findMutableMethodOf(method)?.let { mutableMethod ->
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/4 v0, 0x1
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                }
            }

            // Hook max custom packs limit to unlimited (e.g. 999)
            if (!isStatic && (
                mName == "getmaxcustompacks" ||
                mName == "getcustompackslimit" ||
                mName == "getcustomwordlimit"
            ) && retType == "I") {
                mutableClass.findMutableMethodOf(method)?.let { mutableMethod ->
                    mutableMethod.addInstructions(
                        0,
                        """
                        const/16 v0, 0x3e7
                        return v0
                        """.trimIndent()
                    )
                    hookedPoints++
                }
            }
        }
    }

    logger.info("Hooked $hookedPoints custom word editor checkpoints in Undercover.")
}
