package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.mixin.O.accessors.LocalPlayerAccessor;
import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.PacketEvent;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.*;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.LongJump;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.BlockUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ISkipTicks;
import com.heypixel.heypixelmod.obsoverlay.utils.PlayerUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.Rotation;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import java.util.concurrent.LinkedBlockingDeque;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FletchingTableBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.MovementInputEvent;



@ModuleInfo(
        name = "Velocity",
        description = "Reduces knockback.",
        category = Category.MOVEMENT)
public class Velocity extends Module {
    private int skipTicks = 2; // 反击退时跳过多少tick
    private boolean checkUse = true; // 是否检测使用物品状态
    private boolean checkFall = true; // 是否检测摔落伤害
    private boolean autoJump = true;  // 是否自动跳跃

    private boolean canCancel = false;
    private boolean canJump = false;
    private boolean canSend = false;
    LinkedBlockingDeque<Packet<ClientGamePacketListener>> inBound = new LinkedBlockingDeque<>();
    public static Velocity.Stage stage = Velocity.Stage.IDLE;
    public static int grimTick = -1;
    public static int debugTick = 10;
    public BooleanValue log = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();
    public BooleanValue GrimFull = ValueBuilder.create(this, "GrimFull").setDefaultBooleanValue(false).build().getBooleanValue();
    public BooleanValue JumpReast = ValueBuilder.create(this, "JumpReast").setDefaultBooleanValue(false).build().getBooleanValue();
    public BooleanValue sbMenghuan = ValueBuilder.create(this, "sbMenghuan").setDefaultBooleanValue(false).build().getBooleanValue();
    public BooleanValue sbfanxing = ValueBuilder.create(this, "sbfanxing").setDefaultBooleanValue(false).build().getBooleanValue();
    public BooleanValue oniqboost = ValueBuilder.create(this, "oniqboost").setDefaultBooleanValue(false).build().getBooleanValue();
    Packet velocityPacket;
    private BlockHitResult result = null;
    Scaffold.BlockPosWithFacing pos;

    private boolean shouldAvoidInteraction(Block block) {
        return block instanceof ChestBlock || block instanceof CraftingTableBlock || block instanceof FurnaceBlock || block instanceof EnderChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock || block instanceof AnvilBlock || block instanceof EnchantmentTableBlock || block instanceof BrewingStandBlock || block instanceof BeaconBlock || block instanceof HopperBlock || block instanceof DispenserBlock || block instanceof DropperBlock || block instanceof LecternBlock || block instanceof CartographyTableBlock || block instanceof FletchingTableBlock || block instanceof SmithingTableBlock || block instanceof StonecutterBlock || block instanceof LoomBlock || block instanceof GrindstoneBlock || block instanceof ComposterBlock || block instanceof CauldronBlock || block instanceof BedBlock || block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock || block instanceof ButtonBlock || block instanceof LeverBlock || block instanceof NoteBlock;
    }

    public void reset() {
        if (mc.getConnection() != null) {
            stage = Velocity.Stage.IDLE;
            grimTick = -1;
            debugTick = 0;
            this.processPackets();
        }
    }

    public void processPackets() {
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) {
            this.inBound.clear();
        } else {
            Packet<ClientGamePacketListener> packet;
            while ((packet = this.inBound.poll()) != null) {
                try {
                    packet.handle(connection);
                } catch (Exception var4) {
                    var4.printStackTrace();
                    this.inBound.clear();
                    break;
                }
            }
        }
    }

    public Direction checkBlock(Vec3 baseVec, BlockPos bp) {
        if (!(mc.level.getBlockState(bp).getBlock() instanceof AirBlock)) {
            return null;
        } else {
            Vec3 center = new Vec3((double)bp.getX() + 0.5, (double)((float)bp.getY() + 0.5F), (double)bp.getZ() + 0.5);
            Direction sbface = Direction.DOWN;
            Vec3 hit = center.add(
                    new Vec3((double)sbface.getNormal().getX() * 0.5, (double)sbface.getNormal().getY() * 0.5, (double)sbface.getNormal().getZ() * 0.5)
            );
            Vec3i baseBlock = bp.offset(sbface.getNormal());
            BlockPos po = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
            if (!mc.level.getBlockState(po).entityCanStandOnFace(mc.level, po, mc.player, sbface)) {
                return null;
            } else {
                Vec3 relevant = hit.subtract(baseVec);
                if (relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(sbface.getNormal()).normalize()) >= 0.0) {
                    this.pos = new Scaffold.BlockPosWithFacing(new BlockPos(baseBlock), sbface.getOpposite());
                    return sbface.getOpposite();
                } else {
                    return null;
                }
            }
        }
    }

    @Override
    public void onEnable() {
        this.setSuffix("Grim");
        this.reset();
    }

    @Override
    public void onDisable() {
        canCancel = false;
        canJump = false;
        canSend = false;
        this.reset();
    }

    private void log(String message) {
        if (this.log.getCurrentValue()) {
            ChatUtils.addChatMessage(message);
        }
    }

    @EventTarget
    public void onWorld(EventRespawn eventRespawn) {
        this.reset();
    }

    @EventTarget
    public void onTick(EventRunTicks eventRunTicks) {
        if (mc.player != null && mc.getConnection() != null && mc.gameMode != null && eventRunTicks.getType() != EventType.POST) {
            if (!Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) {
                if (mc.player.isDeadOrDying()
                        || !mc.player.isAlive()
                        || mc.player.getHealth() <= 0.0F
                        || mc.screen instanceof ProgressScreen
                        || mc.screen instanceof DeathScreen) {
                    this.reset();
                }

                if (debugTick > 0) {
                    debugTick--;
                    if (debugTick == 0) {
                        this.processPackets();
                        stage = Velocity.Stage.IDLE;
                    }
                } else {
                    stage = Velocity.Stage.IDLE;
                }

                if (grimTick > 0) {
                    grimTick--;
                }

                float yaw = RotationManager.rotations.getX();
                float pitch = 89.79F;
                BlockHitResult blockRayTraceResult = (BlockHitResult) PlayerUtils.pickCustom(3.7F, yaw, pitch);
                if (stage == Velocity.Stage.TRANSACTION
                        && grimTick == 0
                        && blockRayTraceResult != null
                        && !BlockUtils.isAirBlock(blockRayTraceResult.getBlockPos())
                        && mc.player.getBoundingBox().intersects(new AABB(blockRayTraceResult.getBlockPos().above()))) {
                    Block targetBlock = mc.level.getBlockState(blockRayTraceResult.getBlockPos()).getBlock();
                    if (targetBlock instanceof ChestBlock
                            || targetBlock instanceof CraftingTableBlock
                            || targetBlock instanceof FurnaceBlock
                            || targetBlock instanceof EnchantmentTableBlock
                            || targetBlock instanceof AnvilBlock
                            || targetBlock instanceof BarrelBlock
                            || targetBlock instanceof ShulkerBoxBlock) {
                        return;
                    }

                    this.result = new BlockHitResult(blockRayTraceResult.getLocation(), blockRayTraceResult.getDirection(), blockRayTraceResult.getBlockPos(), false);
                    ((LocalPlayerAccessor) mc.player).setYRotLast(yaw);
                    ((LocalPlayerAccessor) mc.player).setXRotLast(pitch);
                    RotationManager.setRotations(new Rotation(yaw, pitch).toVec2f());
                    if (Aura.rotation != null) {
                        Aura.rotation = new Rotation(yaw, pitch).toVec2f();
                    }

                    this.processPackets();
                    mc.player.connection.send(new Rot(yaw, pitch, mc.player.onGround()));
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, this.result);
                    Naven.skipTasks.add(() -> {
                    });

                    for (int i = 2; i <= 100; i++) {
                        Naven.skipTasks
                                .add(
                                        () -> {
                                            EventMotion event1 = new EventMotion(
                                                    EventType.PRE, mc.player.position().x, mc.player.position().y, mc.player.position().z, yaw, pitch, mc.player.onGround()
                                            );
                                            Naven.getInstance().getRotationManager().onPre(event1);
                                            if (event1.getYaw() != yaw || event1.getPitch() != pitch) {
                                                mc.player.connection.send(new Rot(event1.getYaw(), event1.getPitch(), mc.player.onGround()));
                                            }
                                        }
                                );
                    }

                    debugTick = 20;
                    stage = Velocity.Stage.BLOCK;
                    grimTick = 0;
                }
            }
        }
    }
    @EventTarget
    public void onPacketReceive(PacketEvent event) {
        if (!this.GrimFull.getCurrentValue()) return;

        Object packet = event.getPacket();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 玩家被攻击
        if (packet instanceof ClientboundDamageEventPacket damagePacket &&
                damagePacket.entityId() == player.getId()) {
            canCancel = true;
        }

        // 玩家收到击退
        if (packet instanceof ClientboundSetEntityMotionPacket velPacket &&
                velPacket.getId() == player.getId() &&
                canCancel) {

            float yaw = player.getYRot() - random(0.002f, 0.004f);
            float pitch = 90 - random(0.002f, 0.004f);
            canCancel = false;

            double raycastRange = player.getEyeHeight() + 1.0;
            BlockHitResult hit = raycast(yaw, pitch, raycastRange);

            var blockState = mc.level.getBlockState(hit.getBlockPos());

            if (blockState.isAir() ||
                    blockState.liquid() ||
                    (player.isUsingItem() && checkUse) ||
                    (isFallDamage(velPacket) && checkFall)) {
                return;
            }

            ((ISkipTicks) player).setSkipTicks(skipTicks);
            canJump = true;
            event.cancelEvent();
            canSend = true;
        }
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        if (!this.GrimFull.getCurrentValue()) return;

        LocalPlayer player = mc.player;
        if (player == null) return;

        if (canSend) {
            float yaw = player.getYRot() - random(0.002f, 0.004f);
            float pitch = 90 - random(0.002f, 0.004f);

            mc.getConnection().send(
                    new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround())
            );

            BlockHitResult hit = raycast(yaw, pitch, 5.0);
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);

            canSend = false;
        }
    }

    @EventTarget
    public void onMoveInput(MovementInputEvent event) {
        if (!this.GrimFull.getCurrentValue()) return;
        if (canJump && autoJump) {
            event.setJumping(true);
            canJump = false;
        }
    }

    public boolean isFallDamage(ClientboundSetEntityMotionPacket packet) {
        double velocityX = packet.getXa() / 8000.0;
        double velocityY = packet.getYa() / 8000.0;
        double velocityZ = packet.getZa() / 8000.0;

        return velocityX == 0.0 && velocityZ == 0.0 && velocityY == -0.078375;
    }

    private static float random(float min, float max) {
        return (float) (Math.random() * (max - min) + min);
    }

    private BlockHitResult raycast(float yaw, float pitch, double range) {
        return (BlockHitResult) mc.player.pick(range, 0.0f, false);
    }



    @EventTarget
    public void onPacket(EventHandlePacket e) {
        if (mc.player != null && mc.getConnection() != null && mc.gameMode != null && !mc.player.isUsingItem()) {
            if (!Naven.getInstance().getModuleManager().getModule(LongJump.class).isEnabled()) {
                if (mc.player.tickCount < 20) {
                    this.reset();
                } else if (!mc.player.isDeadOrDying()
                        && mc.player.isAlive()
                        && !(mc.player.getHealth() <= 0.0F)
                        && !(mc.screen instanceof ProgressScreen)
                        && !(mc.screen instanceof DeathScreen)) {
                    Packet<?> packet = e.getPacket();
                    if (packet instanceof ClientboundLoginPacket) {
                        this.reset();
                    } else {
                        if (debugTick > 0 && mc.player.tickCount > 20) {
                            if (stage == Velocity.Stage.BLOCK
                                    && packet instanceof ClientboundBlockUpdatePacket cbu
                                    && this.result != null
                                    && this.result.getBlockPos().equals(cbu.getPos())) {
                                this.processPackets();
                                Naven.skipTasks.clear();
                                debugTick = 0;
                                this.result = null;
                                return;
                            }

                            if (!(packet instanceof ClientboundSystemChatPacket) && !(packet instanceof ClientboundSetTimePacket)) {
                                e.setCancelled(true);
                                this.inBound.add((Packet<ClientGamePacketListener>)packet);
                                return;
                            }
                        }

                        if (packet instanceof ClientboundSetEntityMotionPacket packetEntityVelocity) {
                            if (packetEntityVelocity.getId() != mc.player.getId()) {
                                return;
                            }

                            if (packetEntityVelocity.getYa() < 0 || mc.player.getMainHandItem().getItem() instanceof EnderpearlItem) {
                                e.setCancelled(false);
                                return;
                            }

                            grimTick = 2;
                            debugTick = 100;
                            stage = Velocity.Stage.TRANSACTION;
                            e.setCancelled(true);
                        }
                    }
                } else {
                    this.reset();
                }
            }
        }
    }

    public static enum Stage {
        TRANSACTION,
        ROTATION,
        BLOCK,
        IDLE;
    }
}