package com.dmoniak.patches.shroomid

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SHROOMID
import com.dmoniak.patches.shared.replaceMethod
import java.util.logging.Logger

val shroomIDAllowScreenshotsPatch = bytecodePatch(
    name = "Allow Screenshots & Screen Recording - ShroomID",
    description = "Disables Android FLAG_SECURE window restrictions to allow screenshots and screen recording across all ShroomID mushroom identification and field guide screens.",
) {
    compatibleWith(COMPATIBILITY_SHROOMID)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeShroomIDAllowScreenshotsLogic(logger)
    }
}

fun BytecodePatchContext.executeShroomIDAllowScreenshotsLogic(logger: Logger) {
    logger.info("Executing Allow Screenshots patch for ShroomID (com.shroomid)...")
    var hookedPoints = 0

    classDefForEach { classDef ->
        val type = classDef.type
        val tl = type.lowercase()

        if (!tl.contains("androidx") && !tl.contains("com/google")) {
            val mutableClass by lazy { mutableClassDefBy(classDef) }

            // 1. Hook MainActivity.onCreate to clear FLAG_SECURE (0x2000)
            if (type == "Lcom/shroomid/MainActivity;") {
                for (method in classDef.methods.toList()) {
                    if (method.implementation == null) continue
                    val mName = method.name
                    val pTypes = method.parameterTypes
                    val retType = method.returnType

                    if (mName == "onCreate" && retType == "V" && pTypes.size == 1) {
                        try {
                            mutableClass.replaceMethod(
                                method,
                                registerCount = 4,
                                smaliCode = """
                                    const/4 v0, 0x0
                                    invoke-static {p0, v0}, Lexpo/modules/splashscreen/SplashScreenManager;->registerOnActivity(Landroid/app/Activity;Lexpo/modules/splashscreen/SplashScreenCustomOptions;)V
                                    invoke-super {p0, p1}, Lcom/facebook/react/ReactActivity;->onCreate(Landroid/os/Bundle;)V
                                    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
                                    move-result-object v0
                                    const/16 v1, 0x2000
                                    invoke-virtual {v0, v1}, Landroid/view/Window;->clearFlags(I)V
                                    return-void
                                """.trimIndent(),
                            )
                            hookedPoints++
                            logger.info("[ShroomID Screenshots] Cleared FLAG_SECURE on MainActivity.onCreate")
                        } catch (e: Exception) {
                            logger.warning("[ShroomID Screenshots] Failed to hook MainActivity.onCreate: ${e.message}")
                        }
                    }
                }
            }

            // 2. Neutralize any secure screen setters
            for (method in classDef.methods.toList()) {
                if (method.implementation == null) continue
                val mName = method.name.lowercase()
                val retType = method.returnType

                val isSecureMethod = (
                    mName == "setsecurescreen" ||
                    mName == "setupsecurescreen" ||
                    mName == "enforcesecurescreen" ||
                    mName == "applyflagsecure"
                ) && retType == "V"

                if (isSecureMethod) {
                    try {
                        mutableClass.replaceMethod(
                            method,
                            registerCount = 2,
                            smaliCode = "return-void",
                        )
                        hookedPoints++
                        logger.info("[ShroomID Screenshots] Neutralized secure window setter in ${classDef.type}->${method.name}")
                    } catch (e: Exception) {
                        logger.fine("[ShroomID Screenshots] Failed to hook ${method.name}: ${e.message}")
                    }
                }
            }
        }
    }

    logger.info("[ShroomID Screenshots] Total screenshot hooks applied: $hookedPoints")
}
