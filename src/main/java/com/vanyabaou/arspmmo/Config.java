package com.vanyabaou.arspmmo;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class Config {

    public static ForgeConfigSpec SERVER_CONFIG;

    public static ForgeConfigSpec.ConfigValue<Double> XP_BONUS;
    public static ForgeConfigSpec.ConfigValue<Double> MAX_MANA_BONUS;
    public static ForgeConfigSpec.ConfigValue<Double> MANA_REGEN_BONUS;
    public static ForgeConfigSpec.ConfigValue<Boolean> TIER_CHECKING;
    public static ForgeConfigSpec.ConfigValue<Boolean> PMMO_ATTACK_HANDLER;

    static {
        ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();

        XP_BONUS = SERVER_BUILDER.comment("XP per Mana Spent").define("xp_amount", 0.1d);
        MAX_MANA_BONUS = SERVER_BUILDER.comment("% Max Mana per level").define("max_mana", .01d);
        MANA_REGEN_BONUS = SERVER_BUILDER.comment("% Mana Regen per level").define("mana_regen", .005d);
        TIER_CHECKING = SERVER_BUILDER.comment("Checks the tier of Glyphs and the requirements for the matching Spell Book").define("tier_checking", true);
        PMMO_ATTACK_HANDLER = SERVER_BUILDER.comment("Completely prevents PMMO Attacks for weapons that don't meet weapon requirements. Normally the attack registers but the damage doesn't, this will prevent on-hit effects happening.").define("pmmo_attack_handler", true);

        SERVER_CONFIG = SERVER_BUILDER.build();
    }

}
