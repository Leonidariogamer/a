package dev.recruitsfnp.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Recruits overrides setItemSlot to mirror hand items into its inventory, so temporarily lending the
 * offhand to a reload step through it would duplicate items. Writing the raw hand list avoids that.
 */
@Mixin(Mob.class)
public interface MobAccessor {
    @Accessor("handItems")
    NonNullList<ItemStack> recruitsfnp$getHandItems();
}
