package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import net.minecraft.client.Minecraft;

@ModuleInfo(
        name = "SafeMode",
        description = "Be like this name.",
        category = Category.MISC
)
public class SafeMode extends Module {

    private static final Minecraft mc = Minecraft.getInstance();

    @EventTarget
    public void onTick(EventRunTicks e) {
        if (e.getType() == EventType.PRE) {
            if (mc.player != null || mc.level != null) {
                if (this.isEnabled()) {
                    if (Naven.getInstance().moduleManager.getModule(Aura.class).isEnabled()) {
                        Naven.getInstance().moduleManager.getModule(Aura.class).setEnabled(false);
                        Notification notification = new Notification(NotificationLevel.WARNING,
                                "You have turned on safe mode and the danger module has been turned off!", 4000L);
                        Naven.getInstance().getNotificationManager().addNotification(notification);
                        ChatUtils.addChatMessage("雷静提醒您:开挂千万条，演技第一条。演技不规范，亲人两座坟。");
                    }

                    if (Naven.getInstance().moduleManager.getModule(Scaffold.class).isEnabled()) {
                        Naven.getInstance().moduleManager.getModule(Scaffold.class).setEnabled(false);
                        Notification notification = new Notification(NotificationLevel.WARNING,
                                "You have turned on safe mode and the danger module has been turned off!", 4000L);
                        Naven.getInstance().getNotificationManager().addNotification(notification);
                        ChatUtils.addChatMessage("雷静提醒您:开挂千万条，演技第一条。演技不规范，亲人两座坟。");
                    }

                    if (Naven.getInstance().moduleManager.getModule(com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Velocity.class).isEnabled()) {
                        Naven.getInstance().moduleManager.getModule(com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Velocity.class).setEnabled(false);
                        Notification notification = new Notification(NotificationLevel.WARNING,
                                "You have turned on safe mode and the danger module has been turned off!", 4000L);
                        Naven.getInstance().getNotificationManager().addNotification(notification);
                        ChatUtils.addChatMessage("雷静提醒您:开挂千万条，演技第一条。演技不规范，亲人两座坟。");
                    }

                    // 这里 Aura 重复了一次，我保持原样
                    if (Naven.getInstance().moduleManager.getModule(Aura.class).isEnabled()) {
                        Naven.getInstance().moduleManager.getModule(Aura.class).setEnabled(false);
                        Notification notification = new Notification(NotificationLevel.WARNING,
                                "You have turned on safe mode and the danger module has been turned off!", 4000L);
                        Naven.getInstance().getNotificationManager().addNotification(notification);
                        ChatUtils.addChatMessage("雷静提醒您:开挂千万条，演技第一条。演技不规范，亲人两座坟。");
                    }
                }
            }
        }
    }
}