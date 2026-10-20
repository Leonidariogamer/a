package dev.recruitsfnp.mixin;

import com.talhanation.recruits.entities.CrossBowmanEntity;
import com.talhanation.recruits.entities.ai.RecruitRangedCrossbowAttackGoal;
import dev.recruitsfnp.compat.FnpGuns;
import dev.recruitsfnp.config.CompatConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The stock crossbow goal steps aside while the recruit is holding a Flint'N'Powder gun. */
@Mixin(RecruitRangedCrossbowAttackGoal.class)
public abstract class RecruitRangedCrossbowAttackGoalMixin {
    @Shadow(remap = false) @Final private CrossBowmanEntity crossBowman;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void recruitsfnp$yieldToGun(CallbackInfoReturnable<Boolean> ci) {
        if (yieldsToGun()) {
            ci.setReturnValue(false);
        }
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void recruitsfnp$stopForGun(CallbackInfoReturnable<Boolean> ci) {
        if (yieldsToGun()) {
            ci.setReturnValue(false);
        }
    }

    private boolean yieldsToGun() {
        return CompatConfig.ENABLED.get() && FnpGuns.isGun(crossBowman.getMainHandItem());
    }
}
