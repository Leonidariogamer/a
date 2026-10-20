package dev.recruitsfnp.mixin;

import com.talhanation.recruits.entities.CrossBowmanEntity;
import dev.recruitsfnp.ai.FnpAttackGoal;
import dev.recruitsfnp.ai.FnpReloadGoal;
import dev.recruitsfnp.compat.FnpGuns;
import dev.recruitsfnp.config.CompatConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossBowmanEntity.class)
public abstract class CrossBowmanEntityMixin {
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void recruitsfnp$addGoals(CallbackInfo ci) {
        CrossBowmanEntity recruit = (CrossBowmanEntity) (Object) this;
        recruit.goalSelector.addGoal(0, new FnpAttackGoal(recruit, 1.0D));
        recruit.goalSelector.addGoal(1, new FnpReloadGoal(recruit));
    }

    @Inject(method = "canHoldItem", at = @At("HEAD"), cancellable = true)
    private void recruitsfnp$holdGuns(ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (CompatConfig.ENABLED.get() && FnpGuns.isGun(stack)) {
            ci.setReturnValue(true);
        }
    }

    @Inject(method = "wantsToPickUp", at = @At("HEAD"), cancellable = true)
    private void recruitsfnp$pickUpGunsAndAmmo(ItemStack stack, CallbackInfoReturnable<Boolean> ci) {
        if (CompatConfig.ENABLED.get() && (FnpGuns.isGun(stack) || FnpGuns.isAmmoLike(stack))) {
            ci.setReturnValue(true);
        }
    }
}
