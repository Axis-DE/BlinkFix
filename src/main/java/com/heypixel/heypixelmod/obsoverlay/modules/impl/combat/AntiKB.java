package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.mixin.O.accessors.LocalPlayerAccessor;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventStrafe;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.LongJump;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.Packet;

import java.util.concurrent.LinkedBlockingDeque;

@ModuleInfo(name = "AntiKB", description = "Reduces knockback.", category = Category.MOVEMENT)

    public class AntiKB extends Module {
    LinkedBlockingDeque<Packet> inBound = new LinkedBlockingDeque<>();
    public static Stage stage = Stage.IDLE;
    public static int grimTick = -1;
    public static int debugTick = 10;


    public enum Stage {
        TRANSACTION,
        ROTATION,
        BLOCK,
        IDLE
    }

    public BooleanValue log = ValueBuilder.create(this, "Logging")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public void reset() {
        if (mc.getConnection() == null) return;

        stage = Stage.IDLE;
        grimTick = -1;
        debugTick = 0;
        processPackets(false);
    }

    Packet velocityPacket;

    public void processPackets(boolean succeed) {
        ClientPacketListener connection = mc.getConnection();

        if (connection == null) {
            inBound.clear();
            return;
        }

        Packet<ClientGamePacketListener> packet;
        if (!inBound.isEmpty() && succeed && inBound.getFirst() == velocityPacket) {
            inBound.pollFirst();
        }
        while ((packet = inBound.poll()) != null) {
            try {
                packet.handle(connection);
            } catch (Exception e) {
                e.printStackTrace();
                inBound.clear();
                break;
            }
        }
    }

    private BlockHitResult result = null;

    Scaffold.BlockPosWithFacing pos;

    public Direction checkBlock(Vec3 baseVec, BlockPos bp) {
        if (!(mc.level.getBlockState(bp).getBlock() instanceof AirBlock)) return null;
        Vec3 center = new Vec3(bp.getX() + 0.5, bp.getY() + 0.5f, bp.getZ() + 0.5);
        // for (Direction sbface : Direction.values()) {
        Direction sbface = Direction.DOWN;
        Vec3 hit = center.add(new Vec3(((double) sbface.getNormal().getX()) * 0.5, ((double) sbface.getNormal().getY()) * 0.5, ((double) sbface.getNormal().getZ()) * 0.5));
        Vec3i baseBlock = bp.offset(sbface.getNormal());
        BlockPos po = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
// if (!mc.level.getBlockState(po).getBlock().defaultBlockState().isCollisionShapeFullBlock(mc.level, po))
// continue;
        if (!mc.level.getBlockState(po).entityCanStandOnFace(mc.level, po, mc.player, sbface)) {
// continue;
            return null;
        }
        Vec3 relevant = hit.subtract(baseVec);
        if (relevant.lengthSqr() <= 4.5 * 4.5 && relevant.normalize().dot(
                Vec3.atLowerCornerOf(sbface.getNormal()).normalize()
        ) >= 0) {
            pos = new Scaffold.BlockPosWithFacing(new BlockPos(baseBlock), sbface.getOpposite());
// enumFacing = ;
            return sbface.getOpposite();
        }
// }
        return null;
    }
    public void onEnable() {
        reset();
    }

    public void onDisable() {
        reset();
    }

    private void log(String message) {
        if (log.getCurrentValue()) {
            ChatUtils.addChatMessage(message);
        }
    }

    @EventTarget
    public void onStrafe(EventStrafe event) {reset(); }

    @EventTarget
    public void onWorld(EventRespawn eventRespawn) {
        reset();
    }
    @EventTarget
    public void onTick(EventRunTicks eventRunTicks) { if (mc.player != null && mc.getConnection() != null && mc.gameMode != null) {

        if (mc.player.tickCount < 20) {
            reset();
            return;
        }
        if (mc.player == null || mc.getConnection() == null || mc.gameMode == null) return;

        if (eventRunTicks.getType() == EventType.POST)
            return;

        if (Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) return;

            if (mc.player.isDeadOrDying()
                    || !mc.player.isAlive()
                    || mc.player.getHealth() <= 0
                    || mc.screen instanceof ProgressScreen
                    || mc.screen instanceof DeathScreen) {
                reset();
                return;
            }
            if (debugTick > 0) {
                debugTick--;

                if (debugTick == 0) {
                    processPackets(false);
                    stage = Stage.IDLE;
                }
            } else {
                stage = Stage.IDLE;
            }

            if (grimTick > 0) {
                grimTick--;
            }

            if (mc.player.isUsingItem()) {
                grimTick = -1;
                processPackets(false);
            }

            if (grimTick < 9 && grimTick > 0) {
                Vec3 baseVec = mc.player.getEyePosition().add(
                        mc.player.getDeltaMovement()
                );
                BlockPos base = mc.player.blockPosition();
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        for (int y = -1; y <= 1; y++) {
                            BlockPos newPos = base.offset(x, y, z);
                            if (!mc.player.getBoundingBox().intersects(new AABB(newPos))) continue;
                            Direction direction = checkBlock(baseVec, newPos);
                            if (direction == null) continue;

                            Rotation rotation = RotationUtils.getRotations(newPos, 0F);
                            processPackets(true);
                            ((LocalPlayerAccessor) mc.player).setYRotLast(rotation.getYaw());
                            ((LocalPlayerAccessor) mc.player).setXRotLast(rotation.getPitch());
                            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(rotation.getYaw(), rotation.getPitch(), mc.player.onGround()));
                            result = new BlockHitResult(Scaffold.getVec3(pos.position(), direction), direction, pos.position(), false);
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, result);
                            // 有待考量
                            for (int i = 1; i <= 20; i++) {
                                Naven.skipTasks.add(() -> {
                                });
                            }
                            debugTick = 20;

                            stage = Stage.BLOCK;
                            grimTick = -1;
                            return;
                        }
                    }
                }

            }
        }

    }

    @EventTarget
    public void onPacket(EventHandlePacket e) {

        if (mc.player == null || mc.getConnection() == null || mc.gameMode == null) return;

        if (mc.player.tickCount < 20) {
            reset();
            return;
        }

        if (mc.player.isDeadOrDying()
                || !mc.player.isAlive()
                || mc.player.getHealth() <= 0
                || mc.screen instanceof ProgressScreen
                || mc.screen instanceof DeathScreen) {
            reset();
            return;
        }

        Packet<?> packet = e.getPacket();

        if (packet instanceof ClientboundLoginPacket) {
            reset();
            return;
        }

        if (debugTick > 0 && mc.player.tickCount > 20) {
            if (stage == Stage.BLOCK && packet instanceof ClientboundBlockUpdatePacket cbu) {
                if (result != null && result.getBlockPos().equals(cbu.getPos())) {
                    processPackets(false);
                    Naven.skipTasks.clear();
                    // ChatUtil.addChatMessage("debug tick reset at " + debugTick);
                    debugTick = 0;
                    result = null;
                    return;
                }
            }

            if (!(packet instanceof ClientboundSystemChatPacket)
                    && !(packet instanceof ClientboundSetTimePacket)) {
                e.setCancelled(true);
                inBound.add((Packet<ClientGamePacketListener>) packet);
                return;
            }
        }

        double strength = 0;
        if (packet instanceof ClientboundSetEntityMotionPacket velocity) {
            if (velocity.getId() != mc.player.getId()) return;
            if (mc.player.isInWater()) {
                e.setCancelled(true);
                // ChatUtil.addChatMessage("Ignore: Player in water!");
            } else {
                log("vel " + strength);
            }

        }
        // if (packet instanceof ClientboundExplodePacket explode) {
// if (explode.getKnockbackX() == 0.0F || explode.getKnockbackY() == 0.0F || explode.getKnockbackZ() == 0.0F) {
// return;
// }
//
// strength = new Vec2(explode.getKnockbackX(), explode.getKnockbackZ()).length() * 1000;
// ChatUtil.addChatMessage("explode " + strength);
// }
        if (strength >= 1000) {
            velocityPacket = packet;
            inBound.add((Packet) packet);
            grimTick = 10;
            debugTick = 10;
            stage = Stage.TRANSACTION;
            e.setCancelled(true);
            return;
        }
    }

}