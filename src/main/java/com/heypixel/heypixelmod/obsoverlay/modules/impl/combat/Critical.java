package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventAttack;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateHeldItem;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TimeHelper;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantments;
import com.heypixel.heypixelmod.mixin.O.accessors.MultiPlayerGameModeAccessor;

@ModuleInfo(
   name = "Critical",
   category = Category.COMBAT,
   description = "more damage"
)
public class Critical extends Module {
   public static Critical instance;
   public final BooleanValue packet = ValueBuilder.create(this, "Packet (Danger)").setDefaultBooleanValue(false).build().getBooleanValue();
   public final BooleanValue silent = ValueBuilder.create(this, "Silent").setDefaultBooleanValue(false).build().getBooleanValue();
   int lastSlot = -1;
   private int previousSlot = -1;
   public static TimeHelper timer = new TimeHelper();

   public Critical() {
      instance = this;
   }

   @Override
   public void onEnable() {
      this.setSuffix("GrimFull");
      this.previousSlot = -1;
      this.lastSlot = -1;
      super.onEnable();
   }

   private boolean doTiger(ItemStack stack) {
      return stack.getItem() instanceof SwordItem || InventoryUtils.isSharpnessAxe(stack) && !InventoryUtils.isGodAxe(stack);
   }

   @EventTarget
   public void onWorld(EventRespawn event) {
      this.lastSlot = -1;
   }

   @EventTarget
   public void onAttack(EventAttack event) {
      if (event.isPost()) {
         if (timer.delay(300.0) && this.previousSlot != -1 && mc.player.getInventory().selected != this.previousSlot) {
            mc.player.getInventory().selected = this.previousSlot;
            ((MultiPlayerGameModeAccessor)mc.gameMode).invokeEnsureHasSentCarriedItem();
            this.previousSlot = -1;
         }
      } else if (this.overrideSwordSorting() && this.doTiger(mc.player.getMainHandItem())) {
         if (mc.player.getMainHandItem().getEnchantmentLevel(Enchantments.KNOCKBACK) == 0 && Aura.targets.isEmpty()) {
            for (int i = 36; i < 45; i++) {
               ItemStack curSlot = mc.player.inventoryMenu.getSlot(i).getItem();
               if (curSlot != mc.player.getMainHandItem()
                  && curSlot.getItem() instanceof SwordItem itemSword
                  && itemSword == Items.WOODEN_SWORD
                  && curSlot.getEnchantmentLevel(Enchantments.KNOCKBACK) > 0) {
                  mc.player.getInventory().selected = i - 36;
                  ((MultiPlayerGameModeAccessor)mc.gameMode).invokeEnsureHasSentCarriedItem();
                  timer.reset();
                  return;
               }
            }
         }

         if (Aura.targets.isEmpty()) {
            return;
         }

         int choice = -1;
         float score = 10000.0F;
         int worstChoice = -1;
         float worstScore = 0.0F;

          for (int ix = 36; ix < 45; ix++) {
              ItemStack curSlot = mc.player.inventoryMenu.getSlot(ix).getItem();
              if (curSlot != mc.player.getMainHandItem() && !curSlot.isEmpty() && (ix - 36 == 8 || ix - 36 == 0) && this.doTiger(curSlot)) {
                  float delta = (float)(InventoryUtils.getItemDamage(curSlot) - InventoryUtils.getItemDamage(mc.player.getMainHandItem()));
                  if (delta > 0.0F && delta < score) {
                      choice = ix;
                      score = delta;
                  }
                  if (delta < 0.0F && delta < worstScore) {
                      worstChoice = ix;
                      worstScore = delta;
                  }
              }
          }

         if (choice != -1) {
            int resultSlot = choice - 36;
            if (this.previousSlot == -1) {
               this.previousSlot = mc.player.getInventory().selected;
            }

            if (this.packet.getCurrentValue()) {
               mc.getConnection().send(new ServerboundSetCarriedItemPacket(resultSlot));
            } else {
               mc.player.getInventory().selected = resultSlot;
               ((MultiPlayerGameModeAccessor)mc.gameMode).invokeEnsureHasSentCarriedItem();
            }
         } else if (worstChoice != -1) {
            int resultSlotx = worstChoice - 36;
            if (this.previousSlot == -1) {
               this.previousSlot = mc.player.getInventory().selected;
            }

            if (this.packet.getCurrentValue()) {
               mc.getConnection().send(new ServerboundSetCarriedItemPacket(resultSlotx));
            } else {
               mc.player.getInventory().selected = resultSlotx;
               ((MultiPlayerGameModeAccessor)mc.gameMode).invokeEnsureHasSentCarriedItem();
            }
         }

         timer.reset();
      } else {
         this.lastSlot = -1;
      }
   }

   @EventTarget
   public void onUpdateHeldItem(EventUpdateHeldItem e) {
      if (this.overrideSwordSorting()
         && !Aura.targets.isEmpty()
         && !this.packet.getCurrentValue()
         && e.getHand() == InteractionHand.MAIN_HAND
         && this.previousSlot != -1
         && this.silent.getCurrentValue()) {
         e.setItem(mc.player.getInventory().getItem(this.previousSlot));
      }
   }

   @EventTarget
   public void onPacketEvent(EventPacket event) {
      if (this.packet.getCurrentValue() && event.getType() == EventType.SEND && event.getPacket() instanceof ServerboundSetCarriedItemPacket carriedItemPacket) {
         int slot = carriedItemPacket.getSlot();
         if (slot == this.lastSlot && slot != -1) {
            event.setCancelled(true);
         }

         this.lastSlot = carriedItemPacket.getSlot();
      }
   }

   public boolean overrideSwordSorting() {
      return this.isEnabled();
   }
}
