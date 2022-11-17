package com.vanyabaou.arspmmo;

import com.hollingsworth.arsnouveau.api.event.ManaRegenCalcEvent;
import com.hollingsworth.arsnouveau.api.event.MaxManaCalcEvent;
import com.hollingsworth.arsnouveau.api.event.SpellCastEvent;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.items.SpellArrow;
import com.hollingsworth.arsnouveau.common.items.SpellBow;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.setup.ItemsRegistry;
import harmonised.pmmo.config.JType;
import harmonised.pmmo.skills.Skill;
import harmonised.pmmo.util.XP;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ArsPMMO.MOD_ID)
public class ArsEventHandler {

    @SubscribeEvent
    public static void onSpellCast(SpellCastEvent event){
        if (event.getEntityLiving() == null)
            return;
        if (event.getEntityLiving().world.isRemote)
            return;
        if (!(event.getEntityLiving() instanceof PlayerEntity))
            return;
        LivingEntity entity = event.getEntityLiving();
        UUID uuid = entity.getUniqueID();
        ServerPlayerEntity player = XP.getPlayerByUUID(uuid);
        if (player == null)
            return;
        if (player.isCreative())
            return;
        Spell spell = event.spell;
        boolean touchAir = false;

        AbstractCastMethod castMethod = spell.getCastMethod();
        if (castMethod instanceof MethodTouch) {
            boolean isSensitive = spell.getBuffsAtIndex(0, player, AugmentSensitive.INSTANCE) > 0;
            RayTraceResult result = player.pick(5.0D, 0.0F, isSensitive);
            if (result.getType() != RayTraceResult.Type.BLOCK && (!isSensitive || !(result instanceof BlockRayTraceResult))) {
                //LOGGER.info("Cancelling Touch on Air");
                touchAir = true;
            }
        }

        Map<String,Object> spellData = checkRecipe(player, spell.recipe.toArray(new AbstractSpellPart[]{}));

        if (!(boolean)spellData.get("canCast")) {
            player.sendStatusMessage((new TranslationTextComponent("arspmmo.notSkilledEnoughToCastSpell", new TranslationTextComponent(((AbstractSpellPart)spellData.get("glyph")).getLocalizationKey()))).setStyle(Style.EMPTY.applyFormatting(TextFormatting.RED)), true);
            event.setCanceled(true);
            return;
        }

        if ((boolean)spellData.get("hasEffect") && !touchAir) {
            double xpAward = Config.XP_BONUS.get() * spell.getCastingCost();
            XP.awardXp(player, "magic", null, xpAward, false, false, false);
        }

    }

    @SubscribeEvent
    public static void calcMaxMana(MaxManaCalcEvent event) {
        if (event.getEntityLiving().world.isRemote)
            return;
        LivingEntity entity = event.getEntityLiving();
        UUID uuid = entity.getUniqueID();
        double magicLevel = Skill.getLevel("magic", uuid);
        int maxMana = event.getMax();
        double manaBonus = 1 + magicLevel * Config.MAX_MANA_BONUS.get();
        event.setMax((int) (maxMana * manaBonus));
    }

    @SubscribeEvent
    public static void calcManaRegen(ManaRegenCalcEvent event) {
        if (!event.getEntityLiving().world.isRemote)
            return;
        LivingEntity entity = event.getEntityLiving();
        UUID uuid = entity.getUniqueID();
        double magicLevel = Skill.getLevel("magic", uuid);
        double regen = event.getRegen();
        double manaBonus = 1 + magicLevel * Config.MANA_REGEN_BONUS.get();
        event.setRegen((regen * manaBonus));
    }

    @SubscribeEvent
    public static void itemRightClick(RightClickItem event) {
        ItemStack itemStack = event.getItemStack();
        if (itemStack.getItem() instanceof SpellBow) {
            PlayerEntity player = event.getPlayer();
            if (player.isCreative())
                return;
            ISpellCaster caster = SpellCaster.deserialize(itemStack);

            SpellBow spellBow = (SpellBow) itemStack.getItem();
            ItemStack arrowStack = spellBow.findAmmo(player, itemStack);

            if (!arrowStack.isEmpty() && arrowStack.getItem() instanceof SpellArrow) {
                SpellArrow spellArrow = (SpellArrow) arrowStack.getItem();
                Map<String,Object> arrowSpellData = checkRecipe(player, spellArrow.part);
                if (!(boolean)arrowSpellData.get("canCast")) {
                    player.sendStatusMessage((new TranslationTextComponent("arspmmo.notSkilledEnoughToCastSpell", new TranslationTextComponent(spellArrow.part.getLocalizationKey()))).setStyle(Style.EMPTY.applyFormatting(TextFormatting.RED)), true);
                    event.setCanceled(true);
                }
            }

            SpellResolver spellResolver = new SpellResolver((new SpellContext(caster.getSpell(), player)));

            Map<String,Object> spellData = checkRecipe(player, spellResolver.spell.recipe.toArray(new AbstractSpellPart[]{}));
            if (!(boolean)spellData.get("canCast")) {
                player.sendStatusMessage((new TranslationTextComponent("arspmmo.notSkilledEnoughToCastSpell", new TranslationTextComponent(((AbstractSpellPart)spellData.get("glyph")).getLocalizationKey()))).setStyle(Style.EMPTY.applyFormatting(TextFormatting.RED)), true);
                event.setCanceled(true);
            }
        }
    }

    private static Map<String,Object> checkRecipe(PlayerEntity player, AbstractSpellPart... spellParts) {
        boolean hasEffect = false;
//        String glyphName;
        String glyphRegistryName;
        boolean canCastTier;
        Map<String,Object> spellData = new HashMap<>();
        for (AbstractSpellPart spellPart : spellParts) {
//            LOGGER.info("RecipePart: " + spellPart.getItemID());
            if (spellPart instanceof AbstractEffect)
                hasEffect = true;
            if (XP.isPlayerSurvival(player) && harmonised.pmmo.config.Config.forgeConfig.useReqEnabled.get()) {
//                glyphName = spellPart.getLocaleName();
                glyphRegistryName = "ars_nouveau:" + spellPart.getItemID();
                canCastTier = true;
//                System.out.println(glyphName + " : " + spellPart.getTier().ordinal());
                switch (spellPart.getTier().ordinal()) {
                    case 0:
                        if (!XP.checkReq(player, ItemsRegistry.noviceSpellBook.getRegistryName().toString(), JType.REQ_USE))
                            canCastTier = false;
                        break;
                    case 1:
                        if (!XP.checkReq(player, ItemsRegistry.apprenticeSpellBook.getRegistryName().toString(), JType.REQ_USE))
                            canCastTier = false;
                        break;
                    case 2:
                        if (!XP.checkReq(player, ItemsRegistry.archmageSpellBook.getRegistryName().toString(), JType.REQ_USE))
                            canCastTier = false;
                        break;
                    default:
                        canCastTier = false;
                }
                if (!canCastTier) {
                    spellData.put("canCast", false);
                    spellData.put("glyph", spellPart);
                    return spellData;
                }
                //LOGGER.info("Checking requirements for " + glyphName + " (" + glyphRegistryName + ")");
                if (!XP.checkReq(player, glyphRegistryName, JType.REQ_USE)) {
                    //LOGGER.info(player.getDisplayName().getString() + " - Requirements not met for: " + glyphName + " (" + glyphRegistryName + ")");
                    spellData.put("canCast", false);
                    spellData.put("glyph", spellPart);
                    return spellData;
                }
            }
        }
        spellData.put("canCast", true);
        spellData.put("hasEffect", hasEffect);
        return spellData;
    }

}
