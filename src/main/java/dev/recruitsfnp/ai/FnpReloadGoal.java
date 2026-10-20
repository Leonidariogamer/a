package dev.recruitsfnp.ai;

import com.talhanation.recruits.entities.CrossBowmanEntity;
import dev.recruitsfnp.compat.FnpGunHandler;
import dev.recruitsfnp.compat.FnpGuns;
import dev.recruitsfnp.config.CompatConfig;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Reloads an empty Flint'N'Powder gun while the recruit has nothing to shoot at. */
public class FnpReloadGoal extends Goal {
    private final CrossBowmanEntity recruit;
    private int pause;
    private int cooldown;
    private boolean done;

    public FnpReloadGoal(CrossBowmanEntity recruit) {
        this.recruit = recruit;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!CompatConfig.ENABLED.get() || recruit.getTarget() != null
                || !FnpGuns.isGun(recruit.getMainHandItem())) {
            return false;
        }
        if (pause > 0) {
            pause--;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !done && recruit.getTarget() == null && FnpGuns.isGun(recruit.getMainHandItem());
    }

    @Override
    public void start() {
        done = false;
        cooldown = 0;
    }

    @Override
    public void stop() {
        pause = 100;
    }

    @Override
    public void tick() {
        if (cooldown-- > 0) {
            return;
        }
        if (FnpGunHandler.advance(recruit, false) == FnpGunHandler.Result.NOTHING) {
            done = true;
        } else {
            cooldown = CompatConfig.RELOAD_STEP_TICKS.get();
        }
    }
}
