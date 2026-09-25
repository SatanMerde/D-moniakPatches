package com.dmoniak.patches.shared

import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_HUNGRY_SHARK_WORLD = Compatibility(
        packageName = "com.ubisoft.hungrysharkworld",
        name = "Hungry Shark World",
        description = "Hungry Shark World by Ubisoft Entertainment / FGOL",
        appIconColor = 0x0088CC
    )

    val COMPATIBILITY_HUNGRY_SHARK_EVOLUTION = Compatibility(
        packageName = "com.fgol.HungrySharkEvolution",
        name = "Hungry Shark Evolution",
        description = "Hungry Shark Evolution by Ubisoft Entertainment / FGOL",
        appIconColor = 0x0077AA
    )

    val COMPATIBILITY_HUNGRY_SHARK_HEROES = Compatibility(
        packageName = "com.ubisoft.hungrysharkheroes",
        name = "Hungry Shark Heroes",
        description = "Hungry Shark Heroes by Ubisoft Entertainment / FGOL",
        appIconColor = 0x005588
    )

    val COMPATIBILITY_COINSNAP = Compatibility(
        packageName = "com.coinidentifyer.ai",
        name = "CoinSnap",
        description = "CoinSnap: Coin Identifier by Glority Global Group",
        appIconColor = 0xF5A623
    )
}
