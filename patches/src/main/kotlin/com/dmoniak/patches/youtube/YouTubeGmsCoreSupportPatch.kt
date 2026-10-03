package com.dmoniak.patches.youtube

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction31c
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_YOUTUBE
import java.util.logging.Logger

@Suppress("unused")
val youtubeGmsCoreSupportPatch = bytecodePatch(
    name = "GmsCore Support - YouTube (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Redirects Google Play Services and account authentication calls to GmsCore (MicroG / app.revanced.android.gms) to allow logging in to YouTube without root.",
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeYouTubeGmsCoreSupportLogic(logger)
    }
}

fun BytecodePatchContext.executeYouTubeGmsCoreSupportLogic(logger: Logger) {
    logger.info("Executing dedicated GmsCore Support patch for YouTube...")
    var redirectCount = 0

    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        for (method in classDef.methods.toList()) {
            val impl = method.implementation ?: continue

            for ((index, instruction) in impl.instructions.withIndex()) {
                if (instruction !is ReferenceInstruction) continue
                val ref = instruction.reference
                if (ref !is StringReference) continue
                val str = ref.string

                val newString = when {
                    str == "com.google.android.gms" -> "app.revanced.android.gms"
                    str == "com.google.android.gms.auth.api.credentials.service.START" ->
                        "app.revanced.android.gms.auth.api.credentials.service.START"
                    str == "com.google.android.gms.chimera.container.GmsModuleProvider" ->
                        "app.revanced.android.gms.chimera.container.GmsModuleProvider"
                    str == "com.google.android.gms.phenotype.provider" ->
                        "app.revanced.android.gms.phenotype.provider"
                    str == "com.google" && classDef.type.contains("Account") ->
                        "app.revanced"
                    else -> null
                }

                if (newString != null) {
                    try {
                        val mutableMethod = mutableClass.methods.first { it.name == method.name && it.parameterTypes == method.parameterTypes }
                        if (instruction is BuilderInstruction21c) {
                            mutableMethod.replaceInstruction(index, BuilderInstruction21c(instruction.opcode, instruction.registerA, newString))
                            redirectCount++
                        } else if (instruction is BuilderInstruction31c) {
                            mutableMethod.replaceInstruction(index, BuilderInstruction31c(instruction.opcode, instruction.registerA, newString))
                            redirectCount++
                        }
                    } catch (e: Exception) {
                        logger.fine("[YouTube GmsCore] String replace skipped at index $index: ${e.message}")
                    }
                }
            }
        }
    }

    logger.info("[YouTube GmsCore] Finished: $redirectCount Google Play Services references redirected to GmsCore.")
}
