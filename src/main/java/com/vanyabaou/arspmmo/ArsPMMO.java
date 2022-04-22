package com.vanyabaou.arspmmo;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ArsPMMO.MOD_ID)
public class ArsPMMO {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "arspmmo";

    public ArsPMMO() {
        MinecraftForge.EVENT_BUS.register(ArsEventHandler.class);
        if (Config.PMMO_ATTACK_HANDLER.get()) {
            LOGGER.info("PMMO Attack Handler enabled.");
            MinecraftForge.EVENT_BUS.register(PMMOEventHandler.class);
        }
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SERVER_CONFIG);
        MinecraftForge.EVENT_BUS.register(this);
    }

}
