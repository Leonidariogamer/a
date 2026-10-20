package dev.recruitsfnp.compat;

import com.mojang.logging.LogUtils;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.recruitsfnp.config.CompatConfig;
import dev.recruitsfnp.mixin.MobAccessor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Drives one step of a Flint'N'Powder gun for a recruit by "right clicking" it the way a player
 * would: the gun stays in the main hand and the item for the step is lent to the offhand.
 *
 * <p>A step counts as accepted when the gun's NBT changed. That is how the mod stores its stage
 * flags, so this works for every gun and ammo variant without knowing their stage names.
 */
public final class FnpGunHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int OFFHAND = 1;
    private static boolean warned;

    /** True while a recruit is inside a gun procedure; read by {@link RecoilGuard}. */
    static volatile boolean running;

    public enum Result { NOTHING, LOADED, PRIMED, FIRED }

    private FnpGunHandler() {
    }

    /**
     * @param allowTrigger try the empty-hand step first (prime or fire); only true when aimed at a target
     */
    public static Result advance(AbstractRecruitEntity recruit, boolean allowTrigger) {
        ItemStack gun = recruit.getMainHandItem();
        Method execute = FnpGuns.procedure(gun.getItem());
        if (execute == null || recruit.level().isClientSide()) {
            return Result.NOTHING;
        }
        NonNullList<ItemStack> hands = ((MobAccessor) recruit).recruitsfnp$getHandItems();
        ItemStack savedOffhand = hands.get(OFFHAND);
        boolean wasSneaking = recruit.isShiftKeyDown();
        running = true;
        try {
            if (allowTrigger) {
                hands.set(OFFHAND, ItemStack.EMPTY);
                CompoundTag before = snapshot(gun);
                if (invoke(execute, recruit, gun)) {
                    return fired(recruit, before, gun) ? Result.FIRED : Result.PRIMED;
                }
            }
            for (Candidate candidate : candidates(recruit)) {
                hands.set(OFFHAND, candidate.stack);
                if (invoke(execute, recruit, gun)) {
                    candidate.consume(recruit);
                    return Result.LOADED;
                }
            }
            if (FnpGuns.isMagazineGun(gun)) {
                // Empty magazine: eject it so a fresh one can be inserted on the next step.
                hands.set(OFFHAND, ItemStack.EMPTY);
                recruit.setShiftKeyDown(true);
                if (invoke(execute, recruit, gun)) {
                    return Result.LOADED;
                }
            }
            return Result.NOTHING;
        } finally {
            running = false;
            recruit.setShiftKeyDown(wasSneaking);
            hands.set(OFFHAND, savedOffhand);
        }
    }

    /** A shot leaves a fresh projectile owned by the shooter; powder guns also clear their flag. */
    private static boolean fired(AbstractRecruitEntity recruit, CompoundTag before, ItemStack gun) {
        if (before != null && before.getBoolean("gunpowderloaded") && !gun.getOrCreateTag().getBoolean("gunpowderloaded")) {
            return true;
        }
        return !recruit.level().getEntitiesOfClass(Projectile.class, recruit.getBoundingBox().inflate(3.0D),
                p -> p.getOwner() == recruit && p.tickCount == 0).isEmpty();
    }

    private static boolean invoke(Method execute, AbstractRecruitEntity recruit, ItemStack gun) {
        CompoundTag before = snapshot(gun);
        try {
            execute.invoke(null, recruit.level(), recruit.getX(), recruit.getY(), recruit.getZ(), recruit, gun);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                LOGGER.warn("[recruits_flintnpowder] Gun procedure failed for {}", gun, e);
            }
            return false;
        }
        return !Objects.equals(before, snapshot(gun));
    }

    private static CompoundTag snapshot(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().copy() : null;
    }

    private static List<Candidate> candidates(AbstractRecruitEntity recruit) {
        SimpleContainer inventory = recruit.getInventory();
        Set<Item> seen = new LinkedHashSet<>();
        List<Candidate> list = new ArrayList<>();
        boolean consume = CompatConfig.CONSUME_AMMO.get();
        boolean hasRamrodItem = false;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty() || FnpGuns.isGun(slot) || !FnpGuns.isAmmoLike(slot)) {
                continue;
            }
            if (FnpGuns.isRamrod(slot)) {
                hasRamrodItem = true;
                continue;
            }
            if (seen.add(slot.getItem())) {
                list.add(new Candidate(single(slot), slot.getItem(), consume));
            }
        }
        // Gunpowder first: it is the first reload step on every supported gun.
        list.sort((a, b) -> Boolean.compare(b.item == Items.GUNPOWDER, a.item == Items.GUNPOWDER));

        if (hasRamrodItem || !CompatConfig.REQUIRE_RAMROD_ITEM.get()) {
            Item ramrod = ForgeRegistries.ITEMS.getValue(new ResourceLocation(FnpGuns.FNP, "ramrod"));
            if (ramrod != null && ramrod != Items.AIR) {
                list.add(new Candidate(new ItemStack(ramrod), ramrod, false));
            }
        }
        return list;
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private static final class Candidate {
        final ItemStack stack;
        final Item item;
        final boolean consumes;

        Candidate(ItemStack stack, Item item, boolean consumes) {
            this.stack = stack;
            this.item = item;
            this.consumes = consumes;
        }

        void consume(AbstractRecruitEntity recruit) {
            if (consumes) {
                recruit.getInventory().removeItemType(item, 1);
            }
        }
    }
}
