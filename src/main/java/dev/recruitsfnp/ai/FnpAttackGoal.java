package dev.recruitsfnp.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.CrossBowmanEntity;
import dev.recruitsfnp.compat.FnpGunHandler;
import dev.recruitsfnp.compat.FnpGuns;
import dev.recruitsfnp.config.CompatConfig;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Combat goal for a crossbowman holding a Flint'N'Powder gun: close in, reload, aim, fire. */
public class FnpAttackGoal extends Goal {
    private final CrossBowmanEntity recruit;
    private final double speed;
    private int cooldown;

    public FnpAttackGoal(CrossBowmanEntity recruit, double speed) {
        this.recruit = recruit;
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!CompatConfig.ENABLED.get()) {
            return false;
        }
        LivingEntity target = recruit.getTarget();
        if (target == null || !target.isAlive() || !recruit.getShouldRanged()) {
            return false;
        }
        return holdsGun();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        recruit.setAggressive(true);
    }

    @Override
    public void stop() {
        recruit.setAggressive(false);
        recruit.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = recruit.getTarget();
        if (target == null) {
            return;
        }
        double range = CompatConfig.RANGE.get();
        double distSqr = recruit.distanceToSqr(target);
        boolean sees = recruit.getSensing().hasLineOfSight(target);
        boolean inRange = distSqr <= range * range;

        recruit.getLookControl().setLookAt(target, 30.0F, 30.0F);
        boolean mayMove = !recruit.getShouldHoldPos() && recruit.getFollowState() == 0;
        if (sees && inRange) {
            recruit.getNavigation().stop();
        } else if (mayMove) {
            recruit.getNavigation().moveTo(target, speed);
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        boolean trigger = false;
        if (sees && inRange) {
            aimAt(target);
            trigger = !(CompatConfig.AVOID_FRIENDLY_FIRE.get() && allyInLineOfFire(target));
        }
        cooldown = switch (FnpGunHandler.advance(recruit, trigger)) {
            case FIRED -> CompatConfig.FIRE_COOLDOWN_TICKS.get();
            case PRIMED -> CompatConfig.PRIME_TICKS.get();
            case LOADED -> CompatConfig.RELOAD_STEP_TICKS.get();
            case NOTHING -> 20;
        };
    }

    private boolean holdsGun() {
        if (FnpGuns.isGun(recruit.getMainHandItem())) {
            return true;
        }
        recruit.switchMainHandItem(FnpGuns::isGun);
        return FnpGuns.isGun(recruit.getMainHandItem());
    }

    /** The gun fires along the mob's look vector, so face the target right before the trigger. */
    private void aimAt(LivingEntity target) {
        recruit.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(target.getX(), target.getY(0.6D), target.getZ()));
    }

    private boolean allyInLineOfFire(LivingEntity target) {
        Vec3 from = recruit.getEyePosition();
        Vec3 to = new Vec3(target.getX(), target.getY(0.6D), target.getZ());
        double targetDist = from.distanceToSqr(to);
        AABB box = new AABB(from, to).inflate(1.0D);
        List<LivingEntity> nearby = recruit.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != recruit && e != target && e.isAlive());
        for (LivingEntity other : nearby) {
            if (isAlly(other) && other.getBoundingBox().inflate(0.3D).clip(from, to).isPresent()
                    && from.distanceToSqr(other.position()) < targetDist) {
                return true;
            }
        }
        return false;
    }

    private boolean isAlly(LivingEntity other) {
        if (recruit.isAlliedTo(other)) {
            return true;
        }
        if (other instanceof AbstractRecruitEntity otherRecruit) {
            UUID mine = recruit.getOwnerUUID();
            return mine != null && mine.equals(otherRecruit.getOwnerUUID());
        }
        return other.getUUID().equals(recruit.getOwnerUUID());
    }
}
