package com.dmoniak.patches.silt

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.rawResourcePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.BuilderInstruction
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.dmoniak.patches.shared.BillingHookHelper.executeGooglePlayBillingBypass
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SILT
import com.dmoniak.patches.shared.replaceMethod
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.logging.Logger

private const val PAIRIP_PREFIX = "Lcom/pairip/"
private const val APPLICATION_CLASS = "Lcom/pairip/application/Application;"
private const val STARTUP_LAUNCHER_CLASS = "Lcom/pairip/StartupLauncher;"
private const val VMRUNNER_CLASS = "Lcom/pairip/VMRunner;"
private const val RESTORE_STRING_METHOD = "restoreString"
private val STRING_SOURCE_CLASSES = setOf(APPLICATION_CLASS, STARTUP_LAUNCHER_CLASS)
private val WRAPPER_NAME_REGEX = Regex("""\$\d+$""")

private val INVOKE_OPCODES = setOf(
    Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE,
    Opcode.INVOKE_SUPER, Opcode.INVOKE_SUPER_RANGE,
    Opcode.INVOKE_DIRECT, Opcode.INVOKE_DIRECT_RANGE,
    Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE,
    Opcode.INVOKE_INTERFACE, Opcode.INVOKE_INTERFACE_RANGE,
)
private val MOVE_RESULT_OPCODES = setOf(
    Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_WIDE, Opcode.MOVE_RESULT_OBJECT,
)
private val RETURN_OPCODES = setOf(
    Opcode.RETURN, Opcode.RETURN_WIDE, Opcode.RETURN_OBJECT, Opcode.RETURN_VOID,
)

private fun String.toSmaliLiteral(): String = buildString {
    for (c in this@toSmaliLiteral) when (c) {
        '\\' -> append("\\\\")
        34.toChar() -> append("\\\"")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        else -> append(c)
    }
}

private fun minimalReturnFor(returnType: String): String = when (returnType) {
    "V" -> "return-void"
    "Z", "B", "C", "S", "I", "F" -> "const/4 v0, 0x0\nreturn v0"
    "J", "D" -> "const-wide/16 v0, 0x0\nreturn-wide v0"
    else -> "const/4 v0, 0x0\nreturn-object v0"
}



private fun realOpcodeFor(stubInvoke: Opcode, callSiteRange: Boolean): Opcode? {
    val (base, range) = when (stubInvoke) {
        Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_VIRTUAL_RANGE -> Opcode.INVOKE_VIRTUAL to Opcode.INVOKE_VIRTUAL_RANGE
        Opcode.INVOKE_SUPER, Opcode.INVOKE_SUPER_RANGE -> Opcode.INVOKE_SUPER to Opcode.INVOKE_SUPER_RANGE
        Opcode.INVOKE_DIRECT, Opcode.INVOKE_DIRECT_RANGE -> Opcode.INVOKE_DIRECT to Opcode.INVOKE_DIRECT_RANGE
        Opcode.INVOKE_STATIC, Opcode.INVOKE_STATIC_RANGE -> Opcode.INVOKE_STATIC to Opcode.INVOKE_STATIC_RANGE
        Opcode.INVOKE_INTERFACE, Opcode.INVOKE_INTERFACE_RANGE -> Opcode.INVOKE_INTERFACE to Opcode.INVOKE_INTERFACE_RANGE
        else -> return null
    }
    return if (callSiteRange) range else base
}

private class ParsedWrapper(val realOpcode: Opcode, val realTarget: MethodReference)

private fun BytecodePatchContext.parseWrapper(ref: MethodReference): ParsedWrapper? {
    val classDef = classDefByOrNull(ref.definingClass) ?: return null
    val method = classDef.methods.firstOrNull { m ->
        m.name == ref.name &&
            m.returnType == ref.returnType &&
            m.parameterTypes.map { it.toString() } == ref.parameterTypes.map { it.toString() }
    } ?: return null
    if (!AccessFlags.STATIC.isSet(method.accessFlags)) return null
    val insns = method.implementation?.instructions?.toList() ?: return null
    val invokes = insns.filter { it.opcode in INVOKE_OPCODES }
    if (invokes.size != 1) return null
    val realInvoke = invokes[0]
    if (!insns.all { it === realInvoke || it.opcode in MOVE_RESULT_OPCODES || it.opcode in RETURN_OPCODES }) return null
    val realTarget = (realInvoke as? ReferenceInstruction)?.reference as? MethodReference ?: return null
    return ParsedWrapper(realInvoke.opcode, realTarget)
}

private fun buildInlined(callSite: Instruction, parsed: ParsedWrapper): BuilderInstruction? {
    val newOpcode = realOpcodeFor(parsed.realOpcode, callSite.opcode == Opcode.INVOKE_STATIC_RANGE) ?: return null
    return when (callSite) {
        is BuilderInstruction35c -> BuilderInstruction35c(
            newOpcode,
            callSite.registerCount,
            callSite.registerC,
            callSite.registerD,
            callSite.registerE,
            callSite.registerF,
            callSite.registerG,
            parsed.realTarget,
        )
        is BuilderInstruction3rc -> BuilderInstruction3rc(
            newOpcode,
            callSite.startRegister,
            callSite.registerCount,
            parsed.realTarget,
        )
        else -> null
    }
}

val siltUnlockFullGameBytecodePatch = bytecodePatch(
    name = "Unlock Full Game Bytecode - Silt",
    description = "Bytecode de-obfuscation and unlock engine for Silt",
) {
    compatibleWith(COMPATIBILITY_SILT)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeSiltUnlockFullGameBytecodeLogic(logger)
    }
}

@Suppress("unused")
val siltUnlockFullGamePatch = rawResourcePatch(
    name = "Unlock Full Game - Silt",
    description = "Unlocks the full game, all oceanic abyss chapters, and in-app purchase verification in Silt (v1.0.10). Automatically neutralizes PairIP native DRM in Google Play installations.",
) {
    compatibleWith(COMPATIBILITY_SILT)
    dependsOn(siltUnlockFullGameBytecodePatch)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeSiltNativeDrmNeutralization(logger)
    }
}

fun BytecodePatchContext.executeSiltUnlockFullGameBytecodeLogic(logger: Logger) {
    logger.info("Executing Unlock Full Game bytecode patch for Silt (com.snapbreak.silt)...")
    var hookedPoints = 0

    // 1. Google Play Billing Client SDK bypass
    hookedPoints += executeGooglePlayBillingBypass(logger, "Silt")

    // 2. PairIP Bytecode String De-obfuscation
    val stringMap = HashMap<String, String>()
    val placeholderClasses = HashSet<String>()

    classDefForEach { classDef ->
        if (classDef.type !in STRING_SOURCE_CLASSES) return@classDefForEach
        classDef.methods.forEach methods@{ method ->
            if (classDef.type == STARTUP_LAUNCHER_CLASS && method.name != RESTORE_STRING_METHOD) return@methods
            val insns = method.instructionsOrNull?.toList() ?: return@methods
            insns.forEachIndexed { i, insn ->
                if (insn.opcode == Opcode.SPUT_OBJECT) {
                    val ref = (insn as ReferenceInstruction).reference
                    if (ref is FieldReference) placeholderClasses += ref.definingClass
                }
                if (insn.opcode == Opcode.CONST_STRING && i + 1 < insns.size) {
                    val next = insns[i + 1]
                    if (next.opcode == Opcode.SPUT_OBJECT) {
                        val value = ((insn as ReferenceInstruction).reference as StringReference).string
                        val fieldRef = (next as ReferenceInstruction).reference.toString()
                        stringMap[fieldRef] = value
                    }
                }
            }
        }
        placeholderClasses += classDef.type
    }

    logger.info("[Silt PairIP] Found ${stringMap.size} obfuscated strings across ${placeholderClasses.size} placeholder classes")

    // Inlining obfuscated strings into call sites
    if (stringMap.isNotEmpty()) {
        var restoredStrings = 0
        classDefForEach { classDef ->
            if (classDef.type.startsWith(PAIRIP_PREFIX)) return@classDefForEach
            if (classDef.type in placeholderClasses) return@classDefForEach
            classDef.methods.forEach methods@{ method ->
                val insns = method.instructionsOrNull?.toList() ?: return@methods
                val edits = ArrayList<Triple<Int, Int, String>>()
                insns.forEachIndexed { index, insn ->
                    if (insn.opcode != Opcode.SGET_OBJECT) return@forEachIndexed
                    val fieldRef = (insn as ReferenceInstruction).reference.toString()
                    val value = stringMap[fieldRef] ?: return@forEachIndexed
                    if (index == 0) return@forEachIndexed
                    val prev = insns[index - 1]
                    if (prev.opcode != Opcode.CONST_4 && prev.opcode != Opcode.CONST_16) return@forEachIndexed
                    val reg = (insn as OneRegisterInstruction).registerA
                    edits += Triple(index - 1, index, "const-string v$reg, \"${value.toSmaliLiteral()}\"")
                }
                if (edits.isEmpty()) return@methods
                val mutableClass = mutableClassDefByOrNull(classDef.type) ?: return@methods
                val mutableMethod = mutableClass.methods.firstOrNull { m ->
                    m.name == method.name &&
                        m.returnType == method.returnType &&
                        m.parameterTypes.map { it.toString() } == method.parameterTypes.map { it.toString() }
                } ?: return@methods
                edits.sortedByDescending { it.second }.forEach { (constIndex, sgetIndex, smali) ->
                    mutableMethod.replaceInstruction(sgetIndex, smali)
                    mutableMethod.removeInstruction(constIndex)
                    restoredStrings++
                }
            }
        }
        logger.info("[Silt PairIP] Inlined $restoredStrings string references into bytecode call sites")
        hookedPoints += restoredStrings
    }

    // 3. Inlining Call Wrappers in UnityPlayerActivity and host classes
    var inlinedWrappers = 0
    val stubsToDelete = ArrayList<Pair<String, MethodReference>>()
    classDefForEach { classDef ->
        if (classDef.type.startsWith(PAIRIP_PREFIX)) return@classDefForEach
        classDef.methods.forEach methods@{ method ->
            val insns = method.instructionsOrNull?.toList() ?: return@methods
            val replacements = ArrayList<Pair<Int, BuilderInstruction>>()
            insns.forEachIndexed { index, insn ->
                if (insn.opcode != Opcode.INVOKE_STATIC && insn.opcode != Opcode.INVOKE_STATIC_RANGE) return@forEachIndexed
                val targetRef = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: return@forEachIndexed
                if (!WRAPPER_NAME_REGEX.containsMatchIn(targetRef.name)) return@forEachIndexed
                val parsed = parseWrapper(targetRef) ?: return@forEachIndexed
                val inlined = buildInlined(insn, parsed) ?: return@forEachIndexed
                replacements += index to inlined
                stubsToDelete += targetRef.definingClass to targetRef
            }
            if (replacements.isEmpty()) return@methods
            val mutableClass = mutableClassDefByOrNull(classDef.type) ?: return@methods
            val mutableMethod = mutableClass.methods.firstOrNull { m ->
                m.name == method.name &&
                    m.returnType == method.returnType &&
                    m.parameterTypes.map { it.toString() } == method.parameterTypes.map { it.toString() }
            } ?: return@methods
            replacements.forEach { (index, newInsn) ->
                mutableMethod.replaceInstruction(index, newInsn)
                inlinedWrappers++
            }
        }
    }
    logger.info("[Silt PairIP] Inlined $inlinedWrappers call wrapper stubs (retaining stubs for compatibility)")
    hookedPoints += inlinedWrappers

    // 4. Wipe methods invoking VMRunner and remove PairIP calls
    classDefForEach { classDef ->
        if (classDef.type.startsWith(PAIRIP_PREFIX)) return@classDefForEach
        val vmTargets = classDef.methods.filter { method ->
            method.instructionsOrNull?.any { insn ->
                insn.opcode == Opcode.INVOKE_STATIC &&
                    (insn as? ReferenceInstruction)?.reference?.toString()?.contains(VMRUNNER_CLASS) == true
            } == true
        }
        if (vmTargets.isEmpty()) return@classDefForEach
        val mutableClass = mutableClassDefByOrNull(classDef.type) ?: return@classDefForEach
        vmTargets.forEach { method ->
            val mutableMethod = mutableClass.methods.firstOrNull { m ->
                m.name == method.name &&
                    m.returnType == method.returnType &&
                    m.parameterTypes.map { it.toString() } == method.parameterTypes.map { it.toString() }
            } ?: return@forEach
            val count = mutableMethod.instructions.size
            mutableMethod.removeInstructions(0, count)
            mutableMethod.addInstructions(0, minimalReturnFor(method.returnType))
            hookedPoints++
        }
    }

    // 5. Neutralize PairIP DRM verification classes as safe stubs
    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()

        // Handle PairIP Application specifically: initialize base Android context cleanly and do nothing else
        if (type == APPLICATION_CLASS) {
            for (method in classDef.methods.toList()) {
                if (method.name == "<clinit>") {
                    try {
                        replaceMethod(method = method, registerCount = 1, smaliCode = "return-void")
                        hookedPoints++
                    } catch (_: Exception) {}
                } else if (method.name == "attachBaseContext") {
                    try {
                        replaceMethod(
                            method = method,
                            registerCount = 2,
                            smaliCode = """
                            invoke-super {p0, p1}, Landroid/app/Application;->attachBaseContext(Landroid/content/Context;)V
                            return-void
                            """.trimIndent(),
                        )
                        hookedPoints++
                        logger.info("[Silt PairIP] Neutralized Application.attachBaseContext to pure invoke-super")
                    } catch (_: Exception) {}
                }
            }
            return@classDefForEach
        }

        // Handle CoreComponentFactory.<clinit> so it doesn't invoke StartupLauncher
        if (type == "Landroidx/core/app/CoreComponentFactory;") {
            for (method in classDef.methods.toList()) {
                if (method.name == "<clinit>" && method.implementation != null) {
                    try {
                        replaceMethod(method = method, registerCount = 1, smaliCode = "return-void")
                        hookedPoints++
                        logger.info("[Silt PairIP] Neutralized CoreComponentFactory.<clinit>")
                    } catch (_: Exception) {}
                }
            }
            return@classDefForEach
        }

        if (tl.contains("pairip")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                val mName = method.name
                if (mName == "<init>") continue
                val mn = mName.lowercase()
                val retType = method.returnType

                if (mName == "<clinit>") {
                    try {
                        replaceMethod(method = method, registerCount = 1, smaliCode = "return-void")
                        hookedPoints++
                    } catch (_: Exception) {}
                    continue
                }

                if (tl.contains("signaturecheck") && (mn.contains("verify") || mn.contains("check"))) {
                    try {
                        if (retType == "V") {
                            replaceMethod(method = method, registerCount = 3, smaliCode = "return-void")
                        } else if (retType == "Z") {
                            replaceMethod(method = method, registerCount = 3, smaliCode = "const/4 v0, 0x1\nreturn v0")
                        }
                        hookedPoints++
                    } catch (_: Exception) {}
                    continue
                }

                if ((tl.contains("licensecontentprovider") || tl.contains("initcontextprovider")) && mn == "oncreate") {
                    try {
                        replaceMethod(method = method, registerCount = 3, smaliCode = "const/4 v0, 0x1\nreturn v0")
                        hookedPoints++
                    } catch (_: Exception) {}
                    continue
                }

                if (tl.contains("licenseclient")) {
                    try {
                        if (mn == "performlocalinstallercheck") {
                            replaceMethod(method = method, registerCount = 3, smaliCode = "const/4 v0, 0x1\nreturn v0")
                            hookedPoints++
                        } else if (retType == "V") {
                            replaceMethod(method = method, registerCount = 3, smaliCode = "return-void")
                            hookedPoints++
                        } else if (retType == "Z") {
                            replaceMethod(method = method, registerCount = 3, smaliCode = "const/4 v0, 0x1\nreturn v0")
                            hookedPoints++
                        } else {
                            replaceMethod(method = method, registerCount = 3, smaliCode = minimalReturnFor(retType))
                            hookedPoints++
                        }
                    } catch (_: Exception) {}
                    continue
                }

                if (tl.contains("licenseactivity")) {
                    if (mn == "oncreate" && method.parameterTypes.size == 1) {
                        try {
                            replaceMethod(
                                method = method,
                                registerCount = 2,
                                smaliCode = """
                                invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V
                                invoke-virtual {p0}, Landroid/app/Activity;->finish()V
                                return-void
                                """.trimIndent(),
                            )
                            hookedPoints++
                        } catch (_: Exception) {}
                        continue
                    }
                    if (retType == "V") {
                        try {
                            replaceMethod(method = method, registerCount = 3, smaliCode = "return-void")
                            hookedPoints++
                        } catch (_: Exception) {}
                        continue
                    }
                }

                if (tl.contains("vmrunner")) {
                    try {
                        replaceMethod(method = method, registerCount = 3, smaliCode = minimalReturnFor(retType))
                        hookedPoints++
                    } catch (_: Exception) {}
                    continue
                }

                if (tl.contains("startuplauncher")) {
                    try {
                        replaceMethod(method = method, registerCount = 3, smaliCode = minimalReturnFor(retType))
                        hookedPoints++
                    } catch (_: Exception) {}
                    continue
                }

                // Generic neutralization for any other PairIP method
                try {
                    replaceMethod(method = method, registerCount = 3, smaliCode = minimalReturnFor(retType))
                    hookedPoints++
                } catch (_: Exception) {}
            }
        }
    }

    // Neutralize BroadcastReceivers hijacked by PairIP
    val pairipReceivers = setOf(
        "Landroidx/core/content/pm/ShortcutManagerCompat\$1;",
        "Lcom/android/billingclient/api/zzr;",
        "Lcom/google/android/datatransport/runtime/scheduling/jobscheduling/AlarmManagerSchedulerBroadcastReceiver;",
        "Lcom/google/android/gms/common/api/internal/zabx;",
        "Lcom/google/android/play/core/assetpacks/internal/m;",
        "Lcom/unity3d/player/HFPStatus\$1;",
        "Lorg/fmod/FMOD\$PluginBroadcastReceiver;",
    )
    classDefForEach { classDef ->
        if (classDef.type in pairipReceivers) {
            for (method in classDef.methods.toList()) {
                if (method.name == "onReceive" && method.implementation != null) {
                    try {
                        replaceMethod(method = method, registerCount = 3, smaliCode = "return-void")
                        hookedPoints++
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // 6. Neutralize UnityPlayer.preloadJavaPlugins and GetGlViewContentDescription
    classDefForEach { classDef ->
        if (classDef.type == "Lcom/unity3d/player/UnityPlayer;") {
            for (method in classDef.methods.toList()) {
                if (method.name == "preloadJavaPlugins") {
                    try {
                        replaceMethod(method = method, registerCount = 1, smaliCode = "return-void")
                        hookedPoints++
                    } catch (_: Exception) {}
                }
                if (method.name == "GetGlViewContentDescription") {
                    try {
                        replaceMethod(method = method, registerCount = 2, smaliCode = "const-string v0, \"GameView\"\nreturn-object v0")
                        hookedPoints++
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // 7. Play Integrity API dialog bypass
    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("play/core/integrity") || tl.contains("play/integrity") || tl.contains("integritydialog")) {
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                if (method.name.lowercase().contains("showdialog")) {
                    try {
                        if (method.returnType == "Lcom/google/android/gms/tasks/Task;") {
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
                                """.trimIndent(),
                            )
                            hookedPoints++
                        } else if (method.returnType == "V") {
                            replaceMethod(method = method, registerCount = 3, smaliCode = "return-void")
                            hookedPoints++
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // 8. Game Unlock & Play Pass checks
    classDefForEach { classDef ->
        val tl = classDef.type.lowercase()
        if (tl.contains("androidx") || tl.contains("android/support") || tl.contains("com/google")) return@classDefForEach

        for (method in classDef.methods.toList()) {
            if (method.implementation == null) continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            val mName = method.name.lowercase()
            val retType = method.returnType
            val pTypes = method.parameterTypes

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
                    replaceMethod(method = method, registerCount = 3, smaliCode = "const/4 v0, 0x1\nreturn v0")
                    hookedPoints++
                    logger.info("[Silt Unlock] Unlocked full game check in: ${classDef.type}->${method.name}")
                } catch (_: Exception) {}
            }

            if (!isStatic && (
                mName == "istrial" ||
                mName == "isdemo" ||
                mName == "isfreeversion" ||
                mName == "islimitedversion" ||
                mName == "requiretrial"
            ) && retType == "Z" && pTypes.isEmpty()) {
                try {
                    replaceMethod(method = method, registerCount = 3, smaliCode = "const/4 v0, 0x0\nreturn v0")
                    hookedPoints++
                    logger.info("[Silt Unlock] Suppressed trial/demo flag in: ${classDef.type}->${method.name}")
                } catch (_: Exception) {}
            }

            if (!isStatic && (
                mName == "ischapterunlocked" ||
                mName == "islevelunlocked" ||
                mName == "canplaychapter"
            ) && retType == "Z" && pTypes.size <= 1) {
                try {
                    replaceMethod(method = method, registerCount = 4, smaliCode = "const/4 v0, 0x1\nreturn v0")
                    hookedPoints++
                    logger.info("[Silt Unlock] Unlocked chapter check in: ${classDef.type}->${method.name}")
                } catch (_: Exception) {}
            }
        }
    }

    // 9. Retain all PairIP classes as safe, neutralized stubs in DEX to prevent ClassNotFoundException / NoClassDefFoundError
    logger.info("[Silt PairIP] Retained all PairIP classes as safe neutralized stubs (preserving AndroidManifest components and AndroidX callers)")
    logger.info("[Silt Bytecode] Total bytecode hooks applied: $hookedPoints")
}

fun ResourcePatchContext.executeSiltNativeDrmNeutralization(logger: Logger) {
    logger.info("[Silt Native DRM] Inspecting APK entries for native PairIP binary encryption...")

    // Check for native libraries
    val libEntries = listApkEntries("lib/")
    val pairipCoreEntries = libEntries.filter { it.endsWith("libpairipcore.so") }

    if (pairipCoreEntries.isEmpty()) {
        logger.info("[Silt Native DRM] No libpairipcore.so found in APK (base APK only or clean build). Bytecode patch active.")
    } else {
        for (entry in pairipCoreEntries) {
            try {
                val soFile = get(entry, true)
                val bytes = soFile.readBytes()
                val modified = stubPairipCoreElf(bytes, logger, entry)
                if (modified) {
                    soFile.writeBytes(bytes)
                    logger.info("[Silt Native DRM] Successfully neutralized native anti-tamper in $entry")
                }
            } catch (e: Exception) {
                logger.warning("[Silt Native DRM] Failed to stub $entry: ${e.message}")
            }
        }
    }

    // Delete stamp-cert-sha256 if present to avoid Play signature checks
    val stampEntries = listApkEntries("stamp-cert-sha256")
    for (stamp in stampEntries) {
        try {
            delete(stamp)
            logger.info("[Silt Native DRM] Removed signature stamp: $stamp")
        } catch (_: Exception) {}
    }
}

/**
 * Dynamically parses ELF32 / ELF64 shared libraries and neutralizes:
 * - ExecuteProgram (instant return 0)
 * - JNI_OnLoad (instant return JNI_VERSION_1_6 = 0x10006)
 * - JNI_OnUnload (instant return)
 * - DT_INIT_ARRAY function pointers (instant return)
 */
private fun stubPairipCoreElf(data: ByteArray, logger: Logger, entryName: String): Boolean {
    if (data.size < 52 || data[0] != 0x7f.toByte() || data[1] != 'E'.code.toByte() || data[2] != 'L'.code.toByte() || data[3] != 'F'.code.toByte()) {
        return false
    }
    val eiClass = data[4].toInt() // 1 = 32-bit, 2 = 64-bit
    val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    if (eiClass == 1) { // ELF32 (armeabi-v7a / x86)
        val phOff = buf.getInt(0x1c)
        val phNum = buf.getShort(0x2c).toInt()
        val phList = ArrayList<IntArray>() // [p_type, p_offset, p_vaddr, p_filesz]
        for (i in 0 until phNum) {
            val base = phOff + i * 32
            val pType = buf.getInt(base)
            val pOff = buf.getInt(base + 4)
            val pVaddr = buf.getInt(base + 8)
            val pFilesz = buf.getInt(base + 16)
            phList.add(intArrayOf(pType, pOff, pVaddr, pFilesz))
        }

        fun va2off(va: Int): Int {
            for (p in phList) {
                if (p[0] == 1 && va >= p[2] && va < p[2] + p[3]) { // PT_LOAD
                    return p[1] + (va - p[2])
                }
            }
            return va
        }

        val dynPh = phList.firstOrNull { it[0] == 2 } ?: return false // PT_DYNAMIC
        val dynOff = dynPh[1]
        val dynSz = dynPh[3]

        var symtabVa = 0
        var strtabVa = 0
        var initArrayVa = 0
        var initArraySz = 0

        var dOff = 0
        while (dOff < dynSz) {
            val tag = buf.getInt(dynOff + dOff)
            val dVal = buf.getInt(dynOff + dOff + 4)
            if (tag == 0) break
            when (tag) {
                6 -> symtabVa = dVal // DT_SYMTAB
                5 -> strtabVa = dVal // DT_STRTAB
                0x19 -> initArrayVa = dVal // DT_INIT_ARRAY
                0x1b -> initArraySz = dVal // DT_INIT_ARRAYSZ
            }
            dOff += 8
        }

        val symtabOff = va2off(symtabVa)
        val strtabOff = va2off(strtabVa)
        if (symtabOff == 0 || strtabOff == 0) return false

        var idx = 0
        while (symtabOff + idx * 16 + 16 <= data.size && idx < 3000) {
            val base = symtabOff + idx * 16
            val stName = buf.getInt(base)
            val stValue = buf.getInt(base + 4)
            if (strtabOff + stName < data.size) {
                var end = strtabOff + stName
                while (end < data.size && data[end] != 0.toByte()) end++
                val sName = String(data, strtabOff + stName, end - (strtabOff + stName), Charsets.UTF_8)
                val targetOff = va2off(stValue)

                if (sName == "ExecuteProgram" && targetOff + 8 <= data.size) {
                    // ARM32: mov r0, #0; bx lr (00 00 a0 e3 1e ff 2f e1)
                    val armMov0BxLr = byteArrayOf(0x00, 0x00, 0xa0.toByte(), 0xe3.toByte(), 0x1e, 0xff.toByte(), 0x2f, 0xe1.toByte())
                    System.arraycopy(armMov0BxLr, 0, data, targetOff, armMov0BxLr.size)
                    logger.info("  [ARM32] Neutralized ExecuteProgram at offset 0x${targetOff.toString(16)}")
                } else if (sName == "JNI_OnLoad" && targetOff + 12 <= data.size) {
                    // ARM32: movw r0, #6; movt r0, #1; bx lr
                    val armJniOnLoad = byteArrayOf(0x06, 0x00, 0x00, 0xe3.toByte(), 0x01, 0x00, 0x40, 0xe3.toByte(), 0x1e, 0xff.toByte(), 0x2f, 0xe1.toByte())
                    System.arraycopy(armJniOnLoad, 0, data, targetOff, armJniOnLoad.size)
                    logger.info("  [ARM32] Neutralized JNI_OnLoad at offset 0x${targetOff.toString(16)}")
                } else if (sName == "JNI_OnUnload" && targetOff + 4 <= data.size) {
                    // ARM32: bx lr
                    val armBxLr = byteArrayOf(0x1e, 0xff.toByte(), 0x2f, 0xe1.toByte())
                    System.arraycopy(armBxLr, 0, data, targetOff, armBxLr.size)
                    logger.info("  [ARM32] Neutralized JNI_OnUnload at offset 0x${targetOff.toString(16)}")
                }
            }
            idx++
        }

        if (initArrayVa != 0 && initArraySz > 0) {
            val initArrayOff = va2off(initArrayVa)
            for (i in 0 until initArraySz step 4) {
                if (initArrayOff + i + 4 <= data.size) {
                    val fnVa = buf.getInt(initArrayOff + i)
                    val fnOff = va2off(fnVa)
                    if (fnOff + 4 <= data.size) {
                        val armBxLr = byteArrayOf(0x1e, 0xff.toByte(), 0x2f, 0xe1.toByte())
                        System.arraycopy(armBxLr, 0, data, fnOff, armBxLr.size)
                        logger.fine("  [ARM32] Neutralized .init_array function at offset 0x${fnOff.toString(16)}")
                    }
                }
            }
        }
        return true
    } else if (eiClass == 2) { // ELF64 (arm64-v8a / x86_64)
        val phOff = buf.getLong(0x20).toInt()
        val phNum = buf.getShort(0x38).toInt()
        val phList = ArrayList<LongArray>() // [p_type, p_offset, p_vaddr, p_filesz]
        for (i in 0 until phNum) {
            val base = phOff + i * 56
            val pType = buf.getInt(base).toLong()
            val pOff = buf.getLong(base + 8)
            val pVaddr = buf.getLong(base + 16)
            val pFilesz = buf.getLong(base + 32)
            phList.add(longArrayOf(pType, pOff, pVaddr, pFilesz))
        }

        fun va2off64(va: Long): Int {
            for (p in phList) {
                if (p[0] == 1L && va >= p[2] && va < p[2] + p[3]) {
                    return (p[1] + (va - p[2])).toInt()
                }
            }
            return va.toInt()
        }

        val dynPh = phList.firstOrNull { it[0] == 2L } ?: return false
        val dynOff = dynPh[1].toInt()
        val dynSz = dynPh[3].toInt()

        var symtabVa = 0L
        var strtabVa = 0L
        var initArrayVa = 0L
        var initArraySz = 0L

        var dOff = 0
        while (dOff < dynSz) {
            val tag = buf.getLong(dynOff + dOff)
            val dVal = buf.getLong(dynOff + dOff + 8)
            if (tag == 0L) break
            when (tag) {
                6L -> symtabVa = dVal
                5L -> strtabVa = dVal
                0x19L -> initArrayVa = dVal
                0x1bL -> initArraySz = dVal
            }
            dOff += 16
        }

        val symtabOff = va2off64(symtabVa)
        val strtabOff = va2off64(strtabVa)
        if (symtabOff == 0 || strtabOff == 0) return false

        var idx = 0
        while (symtabOff + idx * 24 + 24 <= data.size && idx < 3000) {
            val base = symtabOff + idx * 24
            val stName = buf.getInt(base)
            val stValue = buf.getLong(base + 8)
            if (strtabOff + stName < data.size) {
                var end = strtabOff + stName
                while (end < data.size && data[end] != 0.toByte()) end++
                val sName = String(data, strtabOff + stName, end - (strtabOff + stName), Charsets.UTF_8)
                val targetOff = va2off64(stValue)

                if (sName == "ExecuteProgram" && targetOff + 8 <= data.size) {
                    // ARM64: mov x0, #0; ret (00 00 80 d2 c0 03 5f d6)
                    val arm64Mov0Ret = byteArrayOf(0x00, 0x00, 0x80.toByte(), 0xd2.toByte(), 0xc0.toByte(), 0x03, 0x5f, 0xd6.toByte())
                    System.arraycopy(arm64Mov0Ret, 0, data, targetOff, arm64Mov0Ret.size)
                    logger.info("  [ARM64] Neutralized ExecuteProgram at offset 0x${targetOff.toString(16)}")
                } else if (sName == "JNI_OnLoad" && targetOff + 12 <= data.size) {
                    // ARM64: mov x0, #6; movk x0, #1, lsl 16; ret
                    val arm64JniOnLoad = byteArrayOf(0xc0.toByte(), 0x00, 0x80.toByte(), 0xd2.toByte(), 0x20, 0x00, 0xa0.toByte(), 0xf2.toByte(), 0xc0.toByte(), 0x03, 0x5f, 0xd6.toByte())
                    System.arraycopy(arm64JniOnLoad, 0, data, targetOff, arm64JniOnLoad.size)
                    logger.info("  [ARM64] Neutralized JNI_OnLoad at offset 0x${targetOff.toString(16)}")
                } else if (sName == "JNI_OnUnload" && targetOff + 4 <= data.size) {
                    // ARM64: ret
                    val arm64Ret = byteArrayOf(0xc0.toByte(), 0x03, 0x5f, 0xd6.toByte())
                    System.arraycopy(arm64Ret, 0, data, targetOff, arm64Ret.size)
                    logger.info("  [ARM64] Neutralized JNI_OnUnload at offset 0x${targetOff.toString(16)}")
                }
            }
            idx++
        }

        if (initArrayVa != 0L && initArraySz > 0L) {
            val initArrayOff = va2off64(initArrayVa)
            for (i in 0 until initArraySz.toInt() step 8) {
                if (initArrayOff + i + 8 <= data.size) {
                    val fnVa = buf.getLong(initArrayOff + i)
                    val fnOff = va2off64(fnVa)
                    if (fnOff + 4 <= data.size) {
                        val arm64Ret = byteArrayOf(0xc0.toByte(), 0x03, 0x5f, 0xd6.toByte())
                        System.arraycopy(arm64Ret, 0, data, fnOff, arm64Ret.size)
                        logger.fine("  [ARM64] Neutralized .init_array function at offset 0x${fnOff.toString(16)}")
                    }
                }
            }
        }
        return true
    }
    return false
}
