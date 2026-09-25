package com.dmoniak.patches.shadowfight

import app.morphe.patcher.patch.bytecodePatch
import com.dmoniak.patches.hungryshark.executeBypassRewardedAdsLogic
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SHADOW_FIGHT_2
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SHADOW_FIGHT_2_SE
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SHADOW_FIGHT_3
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SHADOW_FIGHT_4
import com.dmoniak.patches.shared.Constants.COMPATIBILITY_SHADOW_FIGHT_SHADES
import java.util.logging.Logger

@Suppress("unused")
val shadowFightBypassAdsPatch = bytecodePatch(
    name = "Bypass Rewarded Ads - Shadow Fight (Experimental)",
    description = "⚠️ [En cours de développement / Non testé] Bypasses rewarded video ads in Shadow Fight 2, 3, Shades & Arena for free energy refills and reward multipliers without watching ads. (Experimental - Not yet tested on device).",
) {
    compatibleWith(COMPATIBILITY_SHADOW_FIGHT_2)
    compatibleWith(COMPATIBILITY_SHADOW_FIGHT_2_SE)
    compatibleWith(COMPATIBILITY_SHADOW_FIGHT_3)
    compatibleWith(COMPATIBILITY_SHADOW_FIGHT_SHADES)
    compatibleWith(COMPATIBILITY_SHADOW_FIGHT_4)

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        executeBypassRewardedAdsLogic(logger, "Shadow Fight Saga")
    }
}
