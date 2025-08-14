package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.mixin.O.accessors.LocalPlayerAccessor;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.LongJump;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.BlockUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.PlayerUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.LinkedBlockingDeque;

@ModuleInfo(
        name = "AntiKB",
        description = "Reduces knockback2Fix.",
        category = Category.COMBAT
)
public class AntiKB extends Module {

    private static final Minecraft mc = Minecraft.getInstance();

    LinkedBlockingDeque<Packet<ClientGamePacketListener>> inBound = new LinkedBlockingDeque<>();
    public static Stage stage = Stage.IDLE;
    public static int grimTick = -1;
    public static int debugTick = 10;

    public BooleanValue log = ValueBuilder.create(this, "Logging")
            .setDefaultBooleanValue(false)
            .build().getBooleanValue();
    public BooleanValue onlyGround = ValueBuilder.create(this, "OnlyGround")
            .setDefaultBooleanValue(true)
            .build().getBooleanValue();

    Packet<?> velocityPacket;
    private BlockHitResult result = null;
    Scaffold.BlockPosWithFacing pos;

    private boolean shouldAvoidInteraction(Block block) {
        return block instanceof ChestBlock
                || block instanceof CraftingTableBlock
                || block instanceof FurnaceBlock
                || block instanceof EnderChestBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof AnvilBlock
                || block instanceof EnchantmentTableBlock
                || block instanceof BrewingStandBlock
                || block instanceof BeaconBlock
                || block instanceof HopperBlock
                || block instanceof DispenserBlock
                || block instanceof DropperBlock
                || block instanceof LecternBlock
                || block instanceof CartographyTableBlock
                || block instanceof FletchingTableBlock
                || block instanceof SmithingTableBlock
                || block instanceof StonecutterBlock
                || block instanceof LoomBlock
                || block instanceof GrindstoneBlock
                || block instanceof ComposterBlock
                || block instanceof CauldronBlock
                || block instanceof BedBlock
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof NoteBlock;
    }

    public void reset() {
        if (mc.getConnection() != null) {
            stage = Stage.IDLE;
            grimTick = -1;
            debugTick = 0;
            processPackets();
        }
    }

    public void processPackets() {
        ClientPacketListener connection = mc.getConnection();
        Packet<ClientGamePacketListener> packet;
        if (connection == null) {
            inBound.clear();
        } else {
            while ((packet = inBound.poll()) != null) {
                try {
                    packet.handle(connection);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    inBound.clear();
                    break;
                }
            }
        }
    }

    public Direction checkBlock(Vec3 baseVec, BlockPos bp) {
        if (!(mc.level.getBlockState(bp).getBlock() instanceof AirBlock)) {
            return null;
        }
        Vec3 center = new Vec3(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5);
        Direction face = Direction.DOWN;
        Vec3 hit = center.add(
                face.getNormal().getX() * 0.5,
                face.getNormal().getY() * 0.5,
                face.getNormal().getZ() * 0.5
        );
        Vec3i baseBlock = bp.offset(face.getNormal());
        BlockPos targetPos = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
        if (!mc.level.getBlockState(targetPos).entityCanStandOnFace(mc.level, targetPos, mc.player, face)) {
            return null;
        }
        Vec3 relevant = hit.subtract(baseVec);
        if (relevant.lengthSqr() <= 20.25 &&
                relevant.normalize().dot(new Vec3(face.getNormal().getX(), face.getNormal().getY(), face.getNormal().getZ()).normalize()) >= 0.0) {
            this.pos = new Scaffold.BlockPosWithFacing(new BlockPos(baseBlock), face.getOpposite());
            return face.getOpposite();
        }
        return null;
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    private void log(String message) {
        if (log.getCurrentValue()) {
            ChatUtils.addChatMessage(message);
        }
    }

    @EventTarget
    public void onWorld(EventRespawn eventRespawn) {
        reset();
    }

    @EventTarget
    public void onTick(EventRunTicks eventRunTicks) {
        if (mc.player != null && mc.getConnection() != null && mc.gameMode != null &&
                eventRunTicks.getType() != EventType.POST &&
                !Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) {

            if (mc.player.isDeadOrDying()
                    || !mc.player.isAlive()
                    || mc.player.getHealth() <= 0.0F
                    || mc.screen instanceof ProgressScreen
                    || mc.screen instanceof DeathScreen) {
                reset();
            }

            if (debugTick > 0) {
                --debugTick;
                if (debugTick == 0) {
                    processPackets();
                    stage = Stage.IDLE;
                }
            } else {
                stage = Stage.IDLE;
            }

            if (grimTick > 0) {
                --grimTick;
            }

            float yaw = RotationManager.rotations.getX();
            float pitch = 89.79F;
            BlockHitResult rayTraceResult = (BlockHitResult) PlayerUtils.pickCustom(3.7, yaw, pitch);

            if (stage == Stage.TRANSACTION && grimTick == 0 && rayTraceResult != null
                    && !BlockUtils.isAirBlock(rayTraceResult.getBlockPos())
                    && mc.player.getBoundingBox().intersects(new AABB(rayTraceResult.getBlockPos()))) {

                Block targetBlock = mc.level.getBlockState(rayTraceResult.getBlockPos()).getBlock();
                if (shouldAvoidInteraction(targetBlock)) return;

                this.result = new BlockHitResult(
                        rayTraceResult.getLocation(),
                        rayTraceResult.getDirection(),
                        rayTraceResult.getBlockPos(),
                        false
                );

                ((LocalPlayerAccessor) mc.player).setYRotLast(yaw);
                ((LocalPlayerAccessor) mc.player).setXRotLast(pitch);
                RotationManager.setRotations(new Rotation(yaw, pitch).toVec2f());

                if (Aura.rotation != null) {
                    Aura.rotation = new Rotation(yaw, pitch).toVec2f();
                }

                processPackets();
                mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, mc.player.onGround()));
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, this.result);

                Naven.skipTasks.add(() -> {});
                for (int i = 2; i <= 100; ++i) {
                    Naven.skipTasks.add(() -> {
                        EventMotion event1 = new EventMotion(
                                EventType.PRE,
                                mc.player.position().x,
                                mc.player.position().y,
                                mc.player.position().z,
                                yaw, pitch, mc.player.onGround()
                        );
                        Naven.getInstance().getRotationManager().onPre(event1);
                        if (event1.getYaw() != yaw || event1.getPitch() != pitch) {
                            mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(event1.getYaw(), event1.getPitch(), mc.player.onGround()));
                        }
                    });
                }

                debugTick = 20;
                stage = Stage.BLOCK;
                grimTick = 0;
            }
        }
    }

    @EventTarget
    public void onPacket(EventHandlePacket e) {
        if (mc.player != null && mc.getConnection() != null && mc.gameMode != null &&
                !mc.player.isSpectator() &&
                !Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) {

            if (mc.player.tickCount < 20) {
                reset();
                return;
            }

            if (!mc.player.isDeadOrDying() && mc.player.isAlive() && mc.player.getHealth() > 0.0F
                    && !(mc.screen instanceof ProgressScreen)
                    && !(mc.screen instanceof DeathScreen)
                    && (!onlyGround.getCurrentValue() || mc.player.onGround())) {

                Packet<?> packet = e.getPacket();

                if (packet instanceof ClientboundLoginPacket) {
                    reset();
                    return;
                }

                if (debugTick > 0 && mc.player.tickCount > 20) {
                    if (stage == Stage.BLOCK && packet instanceof ClientboundBlockUpdatePacket cbu) {
                        if (result != null && result.getBlockPos().equals(cbu.getPos())) {
                            processPackets();
                            Naven.skipTasks.clear();
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

                if (packet instanceof ClientboundSetEntityMotionPacket velocityPacket) {
                    if (velocityPacket.getId() != mc.player.getId()) return;

                    if (velocityPacket.getXa() < 0 || mc.player.getMainHandItem().getItem() instanceof EnderpearlItem) {
                        e.setCancelled(false);
                        return;
                    }

                    grimTick = 2;
                    debugTick = 100;
                    stage = Stage.TRANSACTION;
                    e.setCancelled(true);
                }
            } else {
                reset();
            }
        }
    }

    public enum Stage {
        TRANSACTION,
        ROTATION,
        BLOCK,
        IDLE
    }
}
