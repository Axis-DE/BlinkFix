package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.annotations.FlowExclude;
import com.heypixel.heypixelmod.obsoverlay.annotations.ParameterObfuscationExclude;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.*;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.Font;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.platform.InputConstants;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.apache.commons.lang3.RandomUtils;
import org.joml.Vector4f;

@ModuleInfo(
        name = "Scaffold",
        description = "Automatically places blocks under you",
        category = Category.MOVEMENT
)
public class Scaffold extends Module {
    public static final List<Block> blacklistedBlocks = Arrays.asList(
            Blocks.AIR,
            Blocks.WATER,
            Blocks.LAVA,
            Blocks.ENCHANTING_TABLE,
            Blocks.GLASS_PANE,
            Blocks.GLASS_PANE,
            Blocks.IRON_BARS,
            Blocks.SNOW,
            Blocks.COAL_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.TORCH,
            Blocks.ANVIL,
            Blocks.TRAPPED_CHEST,
            Blocks.NOTE_BLOCK,
            Blocks.JUKEBOX,
            Blocks.TNT,
            Blocks.GOLD_ORE,
            Blocks.IRON_ORE,
            Blocks.LAPIS_ORE,
            Blocks.STONE_PRESSURE_PLATE,
            Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Blocks.STONE_BUTTON,
            Blocks.LEVER,
            Blocks.TALL_GRASS,
            Blocks.TRIPWIRE,
            Blocks.TRIPWIRE_HOOK,
            Blocks.RAIL,
            Blocks.CORNFLOWER,
            Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM,
            Blocks.VINE,
            Blocks.SUNFLOWER,
            Blocks.LADDER,
            Blocks.FURNACE,
            Blocks.SAND,
            Blocks.CACTUS,
            Blocks.DISPENSER,
            Blocks.DROPPER,
            Blocks.CRAFTING_TABLE,
            Blocks.COBWEB,
            Blocks.PUMPKIN,
            Blocks.COBBLESTONE_WALL,
            Blocks.OAK_FENCE,
            Blocks.REDSTONE_TORCH,
            Blocks.FLOWER_POT
    );
    public Vector2f correctRotation = new Vector2f();
    public Vector2f rots = new Vector2f();
    public Vector2f lastRots = new Vector2f();
    private int offGroundTicks = 0;
    public ModeValue mode = ValueBuilder.create(this, "Mode").setDefaultModeIndex(0).setModes("Normal", "Telly Bridge", "Keep Y").build().getModeValue();
    public BooleanValue eagle = ValueBuilder.create(this, "Eagle")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal"))
            .build()
            .getBooleanValue();
    public BooleanValue sneak = ValueBuilder.create(this, "Sneak").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue snap = ValueBuilder.create(this, "Snap")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal"))
            .build()
            .getBooleanValue();
    public BooleanValue hideSnap = ValueBuilder.create(this, "Hide Snap Rotation")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal") && this.snap.getCurrentValue())
            .build()
            .getBooleanValue();
    public BooleanValue renderItemSpoof = ValueBuilder.create(this, "Render Item Spoof").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue keepFoV = ValueBuilder.create(this, "Keep FoV").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue fov = ValueBuilder.create(this, "FoV")
            .setDefaultFloatValue(1.15F)
            .setMaxFloatValue(2.0F)
            .setMinFloatValue(1.0F)
            .setFloatStep(0.05F)
            .setVisibility(() -> this.keepFoV.getCurrentValue())
            .build()
            .getFloatValue();
    int oldSlot;
    private Scaffold.BlockPosWithFacing pos;
    private int lastSneakTicks;
    public int baseY = -1;
    private Vector4f blurMatrix;

    public static boolean isValidStack(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof BlockItem) || stack.getCount() <= 1) {
            return false;
        } else if (!InventoryUtils.isItemValid(stack)) {
            return false;
        } else {
            String string = stack.getDisplayName().getString();
            if (string.contains("Click") || string.contains("点击")) {
                return false;
            } else if (stack.getItem() instanceof ItemNameBlockItem) {
                return false;
            } else {
                Block block = ((BlockItem)stack.getItem()).getBlock();
                if (block instanceof FlowerBlock) {
                    return false;
                } else if (block instanceof BushBlock) {
                    return false;
                } else if (block instanceof FungusBlock) {
                    return false;
                } else if (block instanceof CropBlock) {
                    return false;
                } else {
                    return block instanceof SlabBlock ? false : !blacklistedBlocks.contains(block);
                }
            }
        }
    }

    public static boolean isOnBlockEdge(float sensitivity) {
        return !mc.level
                .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate((double)(-sensitivity), 0.0, (double)(-sensitivity)))
                .iterator()
                .hasNext();
    }

    @EventTarget
    public void onFoV(EventUpdateFoV e) {
        if (this.keepFoV.getCurrentValue() && MoveUtils.isMoving()) {
            e.setFov(this.fov.getCurrentValue() + (float)PlayerUtils.getMoveSpeedEffectAmplifier() * 0.13F);
        }
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.oldSlot = mc.player.getInventory().selected;
            this.rots.set(mc.player.getYRot() - 180.0F, mc.player.getXRot());
            this.lastRots.set(mc.player.yRotO - 180.0F, mc.player.xRotO);
            this.pos = null;
            this.baseY = 10000;
        }
    }

    @Override
    public void onDisable() {
        boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
        boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue());
        mc.options.keyJump.setDown(isHoldingJump);
        mc.options.keyShift.setDown(isHoldingShift);
        mc.options.keyUse.setDown(false);
        mc.player.getInventory().selected = this.oldSlot;
    }

    @EventTarget
    public void onUpdateHeldItem(EventUpdateHeldItem e) {
        if (this.renderItemSpoof.getCurrentValue() && e.getHand() == InteractionHand.MAIN_HAND) {
            e.setItem(mc.player.getInventory().getItem(this.oldSlot));
        }
    }

    @EventTarget(1)
    public void onEventEarlyTick(EventRunTicks e) {
        if (e.getType() == EventType.PRE && mc.screen == null && mc.player != null) {
            int slotID = -1;

            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.getItem() instanceof BlockItem && isValidStack(stack)) {
                    slotID = i;
                    break;
                }
            }

            if (mc.player.onGround()) {
                this.offGroundTicks = 0;
            } else {
                this.offGroundTicks++;
            }

            if (slotID != -1 && mc.player.getInventory().selected != slotID) {
                mc.player.getInventory().selected = slotID;
            }

            boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
            if (this.baseY == -1
                    || this.baseY > (int)Math.floor(mc.player.getY()) - 1
                    || mc.player.onGround()
                    || !PlayerUtils.movementInput()
                    || isHoldingJump
                    || this.mode.isCurrentMode("Normal")) {
                this.baseY = (int)Math.floor(mc.player.getY()) - 1;
            }

            this.getBlockPos();
            if (this.pos != null) {
                this.correctRotation = this.getPlayerYawRotation();
                if (this.mode.isCurrentMode("Normal") && this.snap.getCurrentValue()) {
                    this.rots.setX(this.correctRotation.getX());
                } else {
                    this.rots.setX(RotationUtils.rotateToYaw(180.0F, this.rots.getX(), this.correctRotation.getX()));
                }

                this.rots.setY(this.correctRotation.getY());
            }

            if (this.sneak.getCurrentValue()) {
                this.lastSneakTicks++;
                System.out.println(this.lastSneakTicks);
                if (this.lastSneakTicks == 18) {
                    if (mc.player.isSprinting()) {
                        mc.options.keySprint.setDown(false);
                        mc.player.setSprinting(false);
                    }

                    mc.options.keyShift.setDown(true);
                } else if (this.lastSneakTicks >= 21) {
                    mc.options.keyShift.setDown(false);
                    this.lastSneakTicks = 0;
                }
            }

            if (this.mode.isCurrentMode("Telly Bridge")) {
                mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
                if (this.offGroundTicks < 1 && PlayerUtils.movementInput()) {
                    this.rots.setX(RotationUtils.rotateToYaw(180.0F, this.rots.getX(), mc.player.getYRot()));
                    this.lastRots.set(this.rots.getX(), this.rots.getY());
                    return;
                }
            } else if (this.mode.isCurrentMode("Keep Y")) {
                mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
            } else {
                if (this.eagle.getCurrentValue()) {
                    mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
                }

                if (this.snap.getCurrentValue() && !isHoldingJump) {
                    this.doSnap();
                }
            }

            this.lastRots.set(this.rots.getX(), this.rots.getY());
        }
    }

    private void doSnap() {
        boolean shouldPlaceBlock = false;
        HitResult objectPosition = RayTraceUtils.rayCast(1.0F, this.rots);
        if (objectPosition.getType() == Type.BLOCK) {
            BlockHitResult position = (BlockHitResult)objectPosition;
            if (position.getBlockPos().equals(this.pos) && position.getDirection() != Direction.UP) {
                shouldPlaceBlock = true;
            }
        }

        if (!shouldPlaceBlock) {
            this.rots.setX(mc.player.getYRot() + RandomUtils.nextFloat(0.0F, 0.5F) - 0.25F);
        }
    }

    @EventTarget
    public void onClick(EventClick e) {
        e.setCancelled(true);
        if (mc.screen == null && mc.player != null && this.pos != null && (!this.mode.isCurrentMode("Telly Bridge") || this.offGroundTicks >= 1)) {
            if (!this.checkPlace(this.pos)) {
                return;
            }

            this.placeBlock();
        }
    }

    private boolean checkPlace(Scaffold.BlockPosWithFacing data) {
        Vec3 center = new Vec3((double)data.position.getX() + 0.5, (double)((float)data.position.getY() + 0.5F), (double)data.position.getZ() + 0.5);
        Vec3 hit = center.add(
                new Vec3((double)data.facing.getNormal().getX() * 0.5, (double)data.facing.getNormal().getY() * 0.5, (double)data.facing.getNormal().getZ() * 0.5)
        );
        Vec3 relevant = hit.subtract(mc.player.getEyePosition());
        return relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(data.facing.getNormal().multiply(-1)).normalize()) >= 0.0;
    }

    private void placeBlock() {
        if (this.pos != null && isValidStack(mc.player.getMainHandItem())) {
            Direction sbFace = this.pos.facing();
            boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
            if (sbFace != null
                    && (sbFace != Direction.UP || mc.player.onGround() || !PlayerUtils.movementInput() || isHoldingJump || this.mode.isCurrentMode("Normal"))
                    && this.shouldBuild()) {
                InteractionResult result = mc.gameMode
                        .useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(getVec3(this.pos.position(), sbFace), sbFace, this.pos.position(), false));
                if (result == InteractionResult.SUCCESS) {
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    this.pos = null;
                }
            }
        }
    }

    @FlowExclude
    @ParameterObfuscationExclude
    private Vector2f getPlayerYawRotation() {
        return mc.player != null && this.pos != null
                ? new Vector2f(RotationUtils.getRotations(this.pos.position(), 0.0F).getYaw(), RotationUtils.getRotations(this.pos.position(), 0.0F).getPitch())
                : new Vector2f(0.0F, 0.0F);
    }

    private boolean shouldBuild() {
        BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
        return mc.level.isEmptyBlock(playerPos) && isValidStack(mc.player.getMainHandItem());
    }

    @FlowExclude
    @ParameterObfuscationExclude
    private void getBlockPos() {
        Vec3 baseVec = mc.player.getEyePosition().add(mc.player.getDeltaMovement().multiply(2.0, 2.0, 2.0));
        if (mc.player.getDeltaMovement().y < 0.01) {
            FallingPlayer fallingPlayer = new FallingPlayer(mc.player);
            fallingPlayer.calculate(2);
            baseVec = new Vec3(baseVec.x, Math.max(fallingPlayer.y + (double)mc.player.getEyeHeight(), baseVec.y), baseVec.z);
        }

        BlockPos base = BlockPos.containing(baseVec.x, (double)((float)this.baseY + 0.1F), baseVec.z);
        int baseX = base.getX();
        int baseZ = base.getZ();
        if (!mc.level.getBlockState(base).entityCanStandOn(mc.level, base, mc.player)) {
            if (!this.checkBlock(baseVec, base)) {
                for (int d = 1; d <= 6; d++) {
                    if (this.checkBlock(baseVec, new BlockPos(baseX, this.baseY - d, baseZ))) {
                        return;
                    }

                    for (int x = 1; x <= d; x++) {
                        for (int z = 0; z <= d - x; z++) {
                            int y = d - x - z;

                            for (int rev1 = 0; rev1 <= 1; rev1++) {
                                for (int rev2 = 0; rev2 <= 1; rev2++) {
                                    if (this.checkBlock(baseVec, new BlockPos(baseX + (rev1 == 0 ? x : -x), this.baseY - y, baseZ + (rev2 == 0 ? z : -z)))) {
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean checkBlock(Vec3 baseVec, BlockPos bp) {
        if (!(mc.level.getBlockState(bp).getBlock() instanceof AirBlock)) {
            return false;
        } else {
            Vec3 center = new Vec3((double)bp.getX() + 0.5, (double)((float)bp.getY() + 0.5F), (double)bp.getZ() + 0.5);

            for (Direction sbface : Direction.values()) {
                Vec3 hit = center.add(
                        new Vec3((double)sbface.getNormal().getX() * 0.5, (double)sbface.getNormal().getY() * 0.5, (double)sbface.getNormal().getZ() * 0.5)
                );
                Vec3i baseBlock = bp.offset(sbface.getNormal());
                BlockPos po = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
                if (mc.level.getBlockState(po).entityCanStandOnFace(mc.level, po, mc.player, sbface)) {
                    Vec3 relevant = hit.subtract(baseVec);
                    if (relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(sbface.getNormal()).normalize()) >= 0.0) {
                        this.pos = new Scaffold.BlockPosWithFacing(new BlockPos(baseBlock), sbface.getOpposite());
                        return true;
                    }
                }
            }

            return false;
        }
    }

    @FlowExclude
    @ParameterObfuscationExclude
    public static Vec3 getVec3(BlockPos pos, Direction face) {
        double x = (double)pos.getX() + 0.5;
        double y = (double)pos.getY() + 0.5;
        double z = (double)pos.getZ() + 0.5;
        if (face != Direction.UP && face != Direction.DOWN) {
            y += 0.08;
        } else {
            x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
            z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
        }

        if (face == Direction.WEST || face == Direction.EAST) {
            z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
        }

        if (face == Direction.SOUTH || face == Direction.NORTH) {
            x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
        }

        return new Vec3(x, y, z);
    }

    public static record BlockPosWithFacing(BlockPos position, Direction facing) {
    }

    private final BooleanValue blockCounter = ValueBuilder.create(this, "Block Counter")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final FloatValue counterSize = ValueBuilder.create(this, "Counter Size")
            .setVisibility(this.blockCounter::getCurrentValue)
            .setDefaultFloatValue(0.4F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();

    private final FloatValue counterYOffset = ValueBuilder.create(this, "Y Offset")
            .setVisibility(this.blockCounter::getCurrentValue)
            .setDefaultFloatValue(20.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(100.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    private List<Vector4f> blurMatrices = new ArrayList<>();

    @EventTarget
    public void onShader(EventShader e) {
        if (this.blockCounter.getCurrentValue() && e.getType() == EventType.SHADOW) {
            for (Vector4f blurMatrix : this.blurMatrices) {
                RenderUtils.fillBound(e.getStack(), blurMatrix.x(), blurMatrix.y(), blurMatrix.z(), blurMatrix.w(), 1073741824);
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D e) {
        // 检查方块计数器是否启用，以及玩家和世界是否有效
        if (!blockCounter.getCurrentValue() || mc.player == null || mc.level == null) return;

        // 获取屏幕宽高和准心位置
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float crosshairX = screenWidth / 2f;
        float crosshairY = screenHeight / 2f;

        // 统计背包中有效的方块数量
        int totalBlocks = 0;
        for (ItemStack stack : mc.player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                totalBlocks += stack.getCount(); // 计算方块数量
            }
        }

        // 设置文本内容和字体
        String text = "Blocks: " + totalBlocks;
        CustomTextRenderer font = Fonts.opensans;  // 你可以选择其他字体
        float scale = this.counterSize.getCurrentValue();  // 字体大小
        float textWidth = font.getWidth(text, (double)scale);
        float textHeight = (float)font.getHeight(true, (double)scale);

        // 设置文本框位置和大小
        float rectX = crosshairX - textWidth / 2 - 4;
        float rectY = crosshairY + this.counterYOffset.getCurrentValue();
        float rectWidth = textWidth + 8;
        float rectHeight = textHeight + 6;

        // 清除模糊矩阵
        this.blurMatrices.clear();
        this.blurMatrices.add(new Vector4f(rectX, rectY, rectWidth, rectHeight));

        // 渲染背景和进度条
        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(e.getStack(), rectX, rectY, rectWidth, rectHeight, 5.0F, new Color(0, 0, 0, 120).getRGB());
        StencilUtils.erase(true);
        RenderUtils.fillBound(e.getStack(), rectX, rectY, rectWidth, rectHeight, new Color(0, 0, 0, 120).getRGB());

        // 绘制进度条（总方块数量的比例，最大64）
        RenderUtils.fillBound(e.getStack(), rectX, rectY, rectWidth * (totalBlocks / 64.0F), 3.0F, new Color(0, 0, 170, 255).getRGB());
        StencilUtils.dispose();

        // 渲染文本
        font.render(e.getStack(), text, rectX + 4, rectY + 3, Color.WHITE, true, (double)scale);
    }



    private int countValidBlocks() {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack != null && isValidStack(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}