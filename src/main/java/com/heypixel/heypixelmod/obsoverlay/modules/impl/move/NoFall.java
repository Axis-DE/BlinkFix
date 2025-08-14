package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.PacketEvent;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

@ModuleInfo(
        name = "NoFall",
        description = "Prevents fall damage on Grim servers.",
        category = Category.MOVEMENT
)
public class NoFall extends Module {
    private boolean lastGround = false;
    private float lastFallDis = 0F;

    @EventTarget
    public void onPacket(PacketEvent event) {
        Object packetObj = event.getPacket();

        if (packetObj instanceof ServerboundMovePlayerPacket movePacket) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;

            boolean onGround = movePacket.isOnGround();
            if (onGround && !lastGround && lastFallDis >= 5.0F) {
                // 取消原包
                event.cancelEvent();

                // 发送新的包（位移到奇怪坐标，且 onGround=false）
                ServerboundMovePlayerPacket.PosRot spoofedPacket =
                        new ServerboundMovePlayerPacket.PosRot(
                                player.getX() + 1337.0,
                                player.getY(),
                                player.getZ() + 1337.0,
                                player.getYRot(),
                                player.getXRot(),
                                false
                        );

                player.connection.send(spoofedPacket);
                player.resetFallDistance();
            }

            lastGround = onGround;
            lastFallDis = player.fallDistance;
        }
    }
}
