package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@ModuleInfo(
        name = "NoFall",
        description = "NoFall using jump and a bad packet to prevent fall damage.",
        category = Category.MOVEMENT
)
public class NoFall extends Module {
    private static final Minecraft mc = Minecraft.getInstance();
    private boolean isFalling = false;
    private final Map<UUID, Double> lastYMap = new HashMap<>();
    private final Map<UUID, Double> fallDistanceMap = new HashMap<>();
    @Override
    public void onEnable() {
        super.onEnable();
    }
    @Override
    public void onDisable() {
        super.onDisable();
        this.lastYMap.clear();
        this.fallDistanceMap.clear();
    }
    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() == EventType.PRE) {
            Player player = mc.player;
            if (player != null && mc.level != null && this.isEnabled()) {
                UUID playerId = player.getUUID();
                double currentY = player.getY();
                Double lastY = this.lastYMap.put(playerId, currentY);
                if (lastY != null) {
                    double deltaY = lastY - currentY;
                    double totalFall = this.fallDistanceMap.getOrDefault(playerId, 0.0);
                    if (deltaY > 0.0) {
                        totalFall += deltaY;
                    } else {
                        totalFall = 0.0;
                    }
                    this.fallDistanceMap.put(playerId, totalFall);
                    if (player.onGround()) {
                        if (totalFall >= 3.0) {
                            sendBadPacket(player);
                            player.jumpFromGround();
                            Notification notification = new Notification(
                                    NotificationLevel.SUCCESS,
                                    "Successfully NoFall! Fall distance: " + totalFall,
                                    3000L
                            );
                            Naven.getInstance().getNotificationManager().addNotification(notification);
                        }

                        // 重置下落距离
                        this.fallDistanceMap.put(playerId, 0.0);
                    }
                }
            }
        }
    }
    private void sendBadPacket(Player player) {
        double badX = player.getX() + Math.random() * 1000.0;
        double badY = player.getY() + Math.random() * 1000.0;
        double badZ = player.getZ() + Math.random() * 1000.0;
        ServerboundMovePlayerPacket packet = new ServerboundMovePlayerPacket.PosRot(
                badX, badY, badZ, player.getYRot(), player.getXRot(), false
        );
        Objects.requireNonNull(mc.getConnection()).send(packet);
    }
}
