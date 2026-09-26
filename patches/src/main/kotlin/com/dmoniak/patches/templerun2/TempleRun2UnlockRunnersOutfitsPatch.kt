package com.dmoniak.patches.templerun2

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_TEMPLE_RUN_2
import java.util.logging.Logger

@Suppress("unused")
val templeRun2UnlockRunnersOutfitsPatch = bytecodePatch(
    name = "Unlock Runners & Outfits - Temple Run 2 (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Bypasses character and costume paywalls to unlock all idols, characters, and costumes in Temple Run 2.",
) {
    compatibleWith(COMPATIBILITY_TEMPLE_RUN_2)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeTempleRun2UnlockRunnersOutfitsLogic(logger)
    }
}

fun BytecodePatchContext.executeTempleRun2UnlockRunnersOutfitsLogic(logger: Logger) {
    logger.info("Executing Unlock Runners & Outfits patch for Temple Run 2...")
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

            if (!isStatic && (
                mName == "ischaracterunlocked" ||
                mName == "isoutfitunlocked" ||
                mName == "isrunnerpurchased" ||
                mName == "hasunlockeditem" ||
                mName == "canselectcharacter"
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
                    logger.info("[TempleRun2 Unlocks] Unlocked runner/outfit in: ${classDef.type}->${method.name}")
                } catch (e: Exception) {
                    logger.warning("[TempleRun2 Unlocks] Failed to hook ${method.name}: ${e.message}")
                }
            }
        }
    }
    logger.info("[TempleRun2 Unlocks] Total unlock hooks applied: $hookedPoints")
}
