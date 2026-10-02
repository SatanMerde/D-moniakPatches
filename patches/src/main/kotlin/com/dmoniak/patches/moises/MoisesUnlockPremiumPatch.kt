package com.dmoniak.patches.moises

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.dmoniak.patches.hungryshark.util.findMutableMethodOf
import com.dmoniak.patches.shared.BillingHookHelper.executeGooglePlayBillingBypass
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_MOISES
import java.util.logging.Logger

@Suppress("unused")
val moisesUnlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium & Pro Features - Moises (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Hooks Google Play BillingClient, local subscription data classes (Subscription, UserSubscription), and entitlement checks in Moises to unlock client-side Pro features, bypass startup upgrade paywalls, and enable Smart Metronome, chord detection, and pitch/speed controls.",
) {
    compatibleWith(COMPATIBILITY_MOISES)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeMoisesUnlockPremiumLogic(logger)
    }
}

fun BytecodePatchContext.executeMoisesUnlockPremiumLogic(logger: Logger) {
    logger.info("Executing Unlock Premium & Pro patch for Moises...")

    // 1. Google Play Billing Client bypass
    val hookedBilling = executeGooglePlayBillingBypass(logger, "Moises")
    logger.info("[Moises Pro] Total Billing hooks applied: $hookedBilling")

    var hookedMethods = 0
    var hookedDataClasses = 0

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()

        // Skip Android / Kotlin framework internals
        if (tl.startsWith("landroid/") || tl.startsWith("lkotlin/") || tl.startsWith("ljava/")) return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }

        // Check if class defines or handles subscription data models by inspecting string constants
        var isSubscriptionDataClass = false
        for (m in classDef.methods) {
            val impl = m.implementation ?: continue
            for (ins in impl.instructions) {
                if (ins is ReferenceInstruction && (ins.opcode == Opcode.CONST_STRING || ins.opcode == Opcode.CONST_STRING_JUMBO)) {
                    val ref = ins.reference
                    if (ref is StringReference) {
                        val s = ref.string
                        if (
                            s.contains("Subscription(isPremium=") ||
                            s.contains("UserSubscription(isPremium=") ||
                            s.contains("Subscription(isSubscriptionActive=") ||
                            s.contains(", isPremium=") ||
                            s.contains(", isPro=")
                        ) {
                            isSubscriptionDataClass = true
                            break
                        }
                    }
                }
            }
            if (isSubscriptionDataClass) break
        }

        // If this is a detected Subscription data model, hook its fields in <init> and getters
        if (isSubscriptionDataClass) {
            hookedDataClasses++
            logger.info("[Moises Pro] Found Subscription data model: ${classDef.type}")

            // Hook constructor to initialize fields to PRO / true
            val initMethod = classDef.methods.firstOrNull { it.name == "<init>" && it.implementation != null }
            if (initMethod != null) {
                try {
                    val mInit = mutableClass.findMutableMethodOf(initMethod)
                    val instructions = buildString {
                        for (field in classDef.fields) {
                            if (AccessFlags.STATIC.isSet(field.accessFlags)) continue
                            when (field.type) {
                                "Z" -> {
                                    appendLine("const/4 v0, 0x1")
                                    appendLine("iput-boolean v0, p0, ${classDef.type}->${field.name}:Z")
                                }
                                "Ljava/lang/Boolean;" -> {
                                    appendLine("sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;")
                                    appendLine("iput-object v0, p0, ${classDef.type}->${field.name}:Ljava/lang/Boolean;")
                                }
                                "Ljava/lang/String;" -> {
                                    appendLine("const-string v0, \"PRO\"")
                                    appendLine("iput-object v0, p0, ${classDef.type}->${field.name}:Ljava/lang/String;")
                                }
                            }
                        }
                    }
                    if (instructions.isNotBlank()) {
                        mInit.addInstructions(0, instructions)
                        logger.info("[Moises Pro] Hooked <init> in subscription class ${classDef.type}")
                    }
                } catch (e: Exception) {
                    logger.fine("[Moises Pro] Failed to hook <init> in ${classDef.type}: ${e.message}")
                }
            }
        }

        // 2. Hook named entitlement and subscription methods
        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType
            val pTypes = method.parameterTypes

            // Hook primitive boolean getters
            if (!isStatic && pTypes.isEmpty() && retType == "Z") {
                if (
                    isSubscriptionDataClass ||
                    mName == "ispro" ||
                    mName == "ispremium" ||
                    mName == "issubscribed" ||
                    mName == "hasproaccess" ||
                    mName == "haspremiumaccess" ||
                    mName == "isprosubscriber" ||
                    mName == "canuseprofeature" ||
                    mName == "canaccesspro" ||
                    mName == "isproactive" ||
                    mName == "isvalidsubscription" ||
                    mName == "hasactivesubscription" ||
                    mName == "issubscriptionactive" ||
                    mName == "ispaid" ||
                    mName == "isvip" ||
                    mName == "hasactiveplan" ||
                    mName == "isplanactive" ||
                    mName == "isaccountpro"
                ) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const/4 v0, 0x1
                            return v0
                            """.trimIndent()
                        )
                        hookedMethods++
                        logger.info("[Moises Pro] Hooked boolean ${classDef.type}->${method.name} -> true")
                    } catch (e: Exception) {
                        logger.fine("[Moises Pro] Failed to hook ${method.name}: ${e.message}")
                    }
                }
            }

            // Hook boxed Boolean getters
            if (!isStatic && pTypes.isEmpty() && retType == "Ljava/lang/Boolean;") {
                if (
                    isSubscriptionDataClass ||
                    mName == "ispro" ||
                    mName == "ispremium" ||
                    mName == "issubscribed" ||
                    mName == "hasproaccess" ||
                    mName == "haspremiumaccess" ||
                    mName == "issubscriptionactive" ||
                    mName == "ispaid" ||
                    mName == "isvip"
                ) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                            return-object v0
                            """.trimIndent()
                        )
                        hookedMethods++
                        logger.info("[Moises Pro] Hooked Boolean ${classDef.type}->${method.name} -> Boolean.TRUE")
                    } catch (e: Exception) {
                        logger.fine("[Moises Pro] Failed to hook ${method.name}: ${e.message}")
                    }
                }
            }

            // Hook single-parameter feature permission checks
            if (!isStatic && pTypes.size == 1 && retType == "Z") {
                if (
                    mName == "isfeatureunlocked" ||
                    mName == "canaccessfeature" ||
                    mName == "isfeatureavailable" ||
                    mName == "isfeatureenabled" ||
                    mName == "canusefeature"
                ) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const/4 v0, 0x1
                            return v0
                            """.trimIndent()
                        )
                        hookedMethods++
                        logger.info("[Moises Pro] Hooked 1-param check ${classDef.type}->${method.name} -> true")
                    } catch (e: Exception) {
                        logger.fine("[Moises Pro] Failed to hook ${method.name}: ${e.message}")
                    }
                }
            }

            // Hook string getters for tier / plan name -> "PRO"
            if (!isStatic && pTypes.isEmpty() && retType == "Ljava/lang/String;") {
                if (
                    isSubscriptionDataClass ||
                    mName == "getsubscriptiontier" ||
                    mName == "getplan" ||
                    mName == "getplanname" ||
                    mName == "gettier" ||
                    mName == "getaccounttier" ||
                    mName == "getsubscriptiontype" ||
                    mName == "getcurrentplan" ||
                    mName == "getplantype"
                ) {
                    try {
                        val mutableMethod = mutableClass.findMutableMethodOf(method)
                        mutableMethod.addInstructions(
                            0,
                            """
                            const-string v0, "PRO"
                            return-object v0
                            """.trimIndent()
                        )
                        hookedMethods++
                        logger.info("[Moises Pro] Hooked string ${classDef.type}->${method.name} -> 'PRO'")
                    } catch (e: Exception) {
                        logger.fine("[Moises Pro] Failed to hook ${method.name}: ${e.message}")
                    }
                }
            }
        }
    }

    logger.info("[Moises Pro] Finished: $hookedDataClasses data classes and $hookedMethods client entitlement hooks applied.")
}
