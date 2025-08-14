package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import static com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura.*;

@ModuleInfo(
   name = "AimAssist",
   description = "Automatically aims at the nearest entity",
   category = Category.COMBAT
)
public class AimAssist extends Module {
    private static final float[] targetColorRed = new float[]{0.78431374F, 0.0F, 0.0F, 0.23529412F};
    private static final float[] targetColorGreen = new float[]{1.0F, 1.0F, 1.0F, 0.5F};
    private float smoothRotateToYaw(float speed, float currentYaw, float targetYaw) {
        float delta = RotationUtils.getAngleDifference(targetYaw, currentYaw);
        float maxChange = speed; // 每次旋转的最大角度
        delta = Math.max(-maxChange, Math.min(maxChange, delta));
        return currentYaw + delta;
    }

    // 平滑旋转到目标Pitch
    private float smoothRotateToPitch(float speed, float currentPitch, float targetPitch) {
        float delta = targetPitch - currentPitch;
        float maxChange = speed; // 每次旋转的最大角度
        delta = Math.max(-maxChange, Math.min(maxChange, delta));
        return currentPitch + delta;
    }
   BooleanValue attackPlayer = ValueBuilder.create(this, "Attack Player").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackInvisible = ValueBuilder.create(this, "Attack Invisible").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue targetHud = ValueBuilder.create(this, "Target HUD").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue attackAnimals = ValueBuilder.create(this, "Attack Animals").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue attackMobs = ValueBuilder.create(this, "Attack Mobs").setDefaultBooleanValue(false).build().getBooleanValue();
   BooleanValue clickonly = ValueBuilder.create(this, "Click Only").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue slient = ValueBuilder.create(this, "Slient Aim").setDefaultBooleanValue(true).build().getBooleanValue();
   BooleanValue Lockperspective = ValueBuilder.create(this, "Lockperspective").setDefaultBooleanValue(true).build().getBooleanValue();
   FloatValue rotateSpeed = ValueBuilder.create(this, "Rotation Speed")
      .setDefaultFloatValue(10.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(90.0F)
      .build()
      .getFloatValue();
   FloatValue aimRange = ValueBuilder.create(this, "Aim Range")
      .setDefaultFloatValue(5.0F)
      .setFloatStep(0.1F)
      .setMinFloatValue(1.0F)
      .setMaxFloatValue(6.0F)
      .build()
      .getFloatValue();
   FloatValue fov = ValueBuilder.create(this, "FoV")
      .setDefaultFloatValue(360.0F)
      .setFloatStep(1.0F)
      .setMinFloatValue(10.0F)
      .setMaxFloatValue(360.0F)
      .build()
      .getFloatValue();
   ModeValue priority = ValueBuilder.create(this, "Priority").setModes("Health", "FoV", "Range", "None").build().getModeValue();
   public Vector2f targetRotation = new Vector2f();
   public boolean working = false;
   public boolean slientaim = this.slient.currentValue;
   private Vector4f blurMatrix;

    @EventTarget
    public void onShader(EventShader e) {
        if (this.blurMatrix != null && this.targetHud.getCurrentValue()) {
            RenderUtils.drawRoundedRect(e.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), 3.0F, 1073741824);
        }
    }
    @EventTarget
    public void onRender(EventRender2D e) {
        this.blurMatrix = null;
        if (target instanceof LivingEntity && this.targetHud.getCurrentValue()) {
            LivingEntity living = (LivingEntity)target;
            e.getStack().pushPose();
            float x = (float)mc.getWindow().getGuiScaledWidth() / 2.0F + 10.0F;
            float y = (float)mc.getWindow().getGuiScaledHeight() / 2.0F + 10.0F;
            String targetName = target.getName().getString() + (living.isBaby() ? " (Baby)" : "");
            float width = Math.max(Fonts.harmony.getWidth(targetName, 0.4F) + 10.0F, 60.0F);
            this.blurMatrix = new Vector4f(x, y, width, 30.0F);
            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(e.getStack(), x, y, width, 30.0F, 5.0F, HUD.headerColor);
            StencilUtils.erase(true);
            RenderUtils.fillBound(e.getStack(), x, y, width, 30.0F, HUD.bodyColor);
            RenderUtils.fillBound(e.getStack(), x, y, width * (living.getHealth() / living.getMaxHealth()), 3.0F, HUD.headerColor);
            StencilUtils.dispose();
            Fonts.harmony.render(e.getStack(), targetName, (double)(x + 5.0F), (double)(y + 6.0F), Color.WHITE, true, 0.35F);
            Fonts.harmony
                    .render(
                            e.getStack(),
                            "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : ""),
                            (double)(x + 5.0F),
                            (double)(y + 17.0F),
                            Color.WHITE,
                            true,
                            0.35F
                    );
            e.getStack().popPose();
        }
    }
   @EventTarget
     public void onEnable() {
    super.onEnable();
}
    @EventTarget
    public void onMotion(EventRunTicks e) {
        if (e.getType() == EventType.PRE && mc.player != null) {
            if (this.clickonly.currentValue && !mc.options.keyAttack.isDown()) {
                this.working = false;
                return;
            }
            Entity target = this.getTarget();
            if (target != null && mc.player.distanceTo(target) > this.aimRange.getCurrentValue()) {
                target = null;
            }
            if (target != null) {
                Vector2f rotations = RotationUtils.getRotations(target);
                float smoothedYaw = smoothRotateToYaw(
                        this.rotateSpeed.getCurrentValue(),
                        mc.player.getYRot(),
                        rotations.getX()
                );
                float smoothedPitch = smoothRotateToPitch(
                        this.rotateSpeed.getCurrentValue(),
                        mc.player.getXRot(),
                        rotations.getY()
                );
                this.targetRotation.setX(smoothedYaw);
                this.targetRotation.setY(smoothedPitch);
                this.working = true;
                if (this.Lockperspective.getCurrentValue()) {
                    mc.player.setYRot(smoothedYaw);
                    mc.player.setXRot(smoothedPitch);
                    mc.player.yHeadRot = smoothedYaw;
                }
                if (this.slient.getCurrentValue() && !this.Lockperspective.getCurrentValue()) {
                    RotationManager.setServerRotation(smoothedYaw, smoothedPitch);
                }

            } else {
                this.working = false;
            }
        }
    }

    public boolean isValidTarget(Entity entity) {
      if (entity == mc.player) {
         return false;
      } else if (entity instanceof LivingEntity living) {
         if (living instanceof BlinkingPlayer) {
            return false;
         } else {
            AntiBots module = (AntiBots) Naven.getInstance().getModuleManager().getModule(AntiBots.class);
            if (module == null || !module.isEnabled() || !AntiBots.isBot(entity) && !AntiBots.isBedWarsBot(entity)) {
               if (Teams.isSameTeam(living)) {
                  return false;
               } else if (FriendManager.isFriend(living)) {
                  return false;
               } else if (living.isDeadOrDying() || living.getHealth() <= 0.0F) {
                  return false;
               } else if (entity instanceof ArmorStand) {
                  return false;
               } else if (entity.isInvisible() && !this.attackInvisible.getCurrentValue()) {
                  return false;
               } else if (entity instanceof Player && !this.attackPlayer.getCurrentValue()) {
                  return false;
               } else if (!(entity instanceof Player) || !((double)entity.getBbWidth() < 0.5) && !living.isSleeping()) {
                  if ((entity instanceof Mob || entity instanceof Slime || entity instanceof Bat || entity instanceof AbstractGolem)
                     && !this.attackMobs.getCurrentValue()) {
                     return false;
                  } else if ((entity instanceof Animal || entity instanceof Squid) && !this.attackAnimals.getCurrentValue()) {
                     return false;
                  } else {
                     return entity instanceof Villager && !this.attackAnimals.getCurrentValue() ? false : !(entity instanceof Player) || !entity.isSpectator();
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean isValidAttack(Entity entity) {
      if (!this.isValidTarget(entity)) {
         return false;
      } else {
         Vec3 closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePosition(), entity.getBoundingBox());
         if (closestPoint.distanceTo(mc.player.getEyePosition()) > (double)this.aimRange.getCurrentValue()) {
            return false;
         } else {
            boolean b = RotationUtils.inFoV(entity, this.fov.getCurrentValue() / 2.0F);
            if (entity.getName().getString().equals("Standing")) {
               System.out.println(b);
            }

            return b;
         }
      }
   }

   private Entity getTarget() {
      Stream<Entity> stream = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
         .filter(entity -> entity instanceof Entity)
         .filter(this::isValidAttack);
      List<Entity> possibleTargets = stream.collect(Collectors.toList());
      if (this.priority.isCurrentMode("Range")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> (double)o.distanceTo(mc.player)));
      } else if (this.priority.isCurrentMode("FoV")) {
         possibleTargets.sort(
            Comparator.comparingDouble(o -> (double)RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x))
         );
      } else if (this.priority.isCurrentMode("Health")) {
         possibleTargets.sort(Comparator.comparingDouble(o -> o instanceof LivingEntity living ? (double)living.getHealth() : 0.0));
      }

      return possibleTargets.isEmpty() ? null : possibleTargets.get(0);
   }
}
