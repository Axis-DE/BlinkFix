package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventSlowdown;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;

@ModuleInfo(
   name = "NoSlow",
   description = "NoSlowDown",
   category = Category.MOVEMENT
)
public class NoSlow extends Module {
    @EventTarget
    public void onEnable() {
        super.onEnable();
    }

   @EventTarget
   public void onSlow(EventSlowdown eventSlowdown) {
      if (mc.player.getUseItemRemainingTicks() % 2 != 0 && mc.player.getUseItemRemainingTicks() <= 30) {
         eventSlowdown.setSlowdown(false);
         mc.player.setSprinting(true);
      }
   }
}
