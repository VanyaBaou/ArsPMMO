package com.vanyabaou.arspmmo;

import harmonised.pmmo.config.AutoValues;
import harmonised.pmmo.config.JType;
import harmonised.pmmo.network.MessageDoubleTranslation;
import harmonised.pmmo.network.NetworkHandler;
import harmonised.pmmo.skills.Skill;
import harmonised.pmmo.util.NBTHelper;
import harmonised.pmmo.util.Util;
import harmonised.pmmo.util.XP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = ArsPMMO.MOD_ID)
public class PMMOEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handleAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntityLiving();
        Entity source = event.getSource().getTrueSource();
        if (target != null && source instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) source;
            if (XP.isPlayerSurvival(player)) {
                ItemStack mainItemStack = player.getHeldItemMainhand();
                ResourceLocation mainResLoc = player.getHeldItemMainhand().getItem().getRegistryName();
                ResourceLocation offResLoc = player.getHeldItemOffhand().getItem().getRegistryName();
                Map<String, Double> weaponReq = XP.getXpBypass(mainResLoc, JType.REQ_WEAPON);
                NBTHelper.maxDoubleMaps(weaponReq, XP.getXpBypass(offResLoc, JType.REQ_WEAPON));
                String skill;
                String itemSpecificSkill = AutoValues.getItemSpecificSkill(mainResLoc.toString());
                boolean swordInMainHand = mainItemStack.getItem() instanceof SwordItem;
                if (itemSpecificSkill != null)
                    skill = itemSpecificSkill;
                else {
                    if (event.getSource().damageType.equals("arrow"))
                        skill = Skill.ARCHERY.toString();
                    else {
                        skill = Skill.COMBAT.toString();
                        if(Util.getDistance(player.getPositionVec(), target.getPositionVec()) > 4.20 + target.getWidth() + (swordInMainHand ? 1.523 : 0))
                            skill = Skill.MAGIC.toString(); //Magically far melee damage
                    }
                }
                int weaponGap;
                if (harmonised.pmmo.config.Config.getConfig("weaponReqEnabled") != 0) {
                    if (harmonised.pmmo.config.Config.getConfig("autoGenerateValuesEnabled") != 0 && harmonised.pmmo.config.Config.getConfig("autoGenerateWeaponReqDynamicallyEnabled") != 0)
                        weaponReq.put(skill, weaponReq.getOrDefault(skill, AutoValues.getWeaponReqFromStack(mainItemStack)));
                    weaponGap = XP.getSkillReqGap(player, weaponReq);
                    int enchantGap = XP.getSkillReqGap(player, XP.getEnchantsUseReq(player.getHeldItemMainhand()));
                    int gap = Math.max(weaponGap, enchantGap);
                    if (gap > 0) {
                        if (enchantGap < gap)
                            NetworkHandler.sendToPlayer(new MessageDoubleTranslation("pmmo.notSkilledEnoughToUseAsWeapon", player.getHeldItemMainhand().getTranslationKey(), "", true, 2), player);
                        if (harmonised.pmmo.config.Config.forgeConfig.strictReqWeapon.get()) {
                            event.setCanceled(true);
                        }
                    }
                }
            }
        }
    }

}
