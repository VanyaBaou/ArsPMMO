package com.vanyabaou.arspmmo;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(ArsPMMO.MOD_ID)
public class ArsPMMO {

    public static final String MOD_ID = "arspmmo";

    public ArsPMMO() {
        MinecraftForge.EVENT_BUS.register(EventHandler.class);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SERVER_CONFIG);
        MinecraftForge.EVENT_BUS.register(this);
    }

}
