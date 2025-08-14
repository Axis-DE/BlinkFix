package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

@ModuleInfo(
        name = "JumpReset",
        description = "Jump when receiving velocity packet to reset fall or bypass checks",
        category = Category.COMBAT
)
public class JumpReset extends Module {
    private static final Minecraft mc = Minecraft.getInstance();

    private int cooldown = 0;
    private static final int COOLDOWN_TICKS = 10;

    public BooleanValue log = ValueBuilder.create(this, "Logging")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        this.cooldown = 0;
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (this.cooldown > 0) {
            --this.cooldown;
        }
    }

    @EventTarget
    public void onPacket(EventHandlePacket event) {
        if (!this.isEnabled()) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        if (this.cooldown > 0) {
            return;
        }

        Packet<ClientGamePacketListener> packet = event.getPacket();
        if (packet instanceof ClientboundSetEntityMotionPacket velocityPacket) {
            if (velocityPacket.getId() != player.getId()) {
                return;
            }

            // player.jumpFromGround();
            player.jumpFromGround();
            player.setOnGround(true);

            if (this.log.getCurrentValue()) {
                ChatUtils.addChatMessage("§a[JumpReset] Received velocity!");
            }

            this.cooldown = COOLDOWN_TICKS;
        }
    }
}