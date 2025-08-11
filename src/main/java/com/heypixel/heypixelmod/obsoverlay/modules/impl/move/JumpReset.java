package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

@ModuleInfo(
        name = "JumpReset",
        description = "Reset your jump.",
        category = Category.MOVEMENT
)

public class JumpReset extends Module {
    private static final Minecraft mc = Minecraft.getInstance();
    private Vec3 lastVelocity = Vec3.ZERO;
    private int cooldownTicks = 0;

    public void JumpResetMod() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(50); // 1 tick = 50ms
                    } catch (InterruptedException ignored) {}
                    mc.execute(this::onTick);
                }
            }, "JumpReset-Tick").start();
        }
    }

    private void onTick() {
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            lastVelocity = player.getDeltaMovement();
            return;
        }

        Vec3 currentVel = player.getDeltaMovement();
        double velDiffX = Math.abs(currentVel.x - lastVelocity.x);
        double velDiffZ = Math.abs(currentVel.z - lastVelocity.z);

        if ((velDiffX > 0.15 || velDiffZ > 0.15)
                && !player.onGround()
                && !player.isFallFlying()
                && !player.isInWater()) {
            player.jumpFromGround();
            cooldownTicks = 10;
        }

        lastVelocity = currentVel;
    }
}