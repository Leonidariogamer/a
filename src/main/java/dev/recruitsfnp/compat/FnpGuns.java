package dev.recruitsfnp.compat;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/**
 * Finds Flint'N'Powder guns and the procedure that implements their right click.
 *
 * <p>Flint'N'Powder is an MCreator mod: each gun's use() calls
 * {@code <Name>RightclickedProcedure.execute(level, x, y, z, entity, stack)}, and that procedure
 * reads the mob's hands generically (only cooldowns and ammo removal are Player-only). Calling it
 * reflectively with a recruit as the entity runs the mod's own reload/fire code, so there is no
 * compile-time dependency and no copy of its balance or projectile logic here.
 */
public final class FnpGuns {
    public static final String FNP = "flintnpowder";

    /** Muzzle loaders that share the powder -> shot -> ramrod -> (prime) -> fire flow. */
    private static final Set<String> GUN_IDS = Set.of(
            "musket", "bayonet_musket", "rifle", "pistol", "arquebus",
            "hand_gonne", "blunderbuss", "bruttbuss", "big_game", "flinter");

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Item, Optional<Method>> PROCEDURES = new ConcurrentHashMap<>();

    private FnpGuns() {
    }

    public static boolean isGun(ItemStack stack) {
        return stack != null && !stack.isEmpty() && procedure(stack.getItem()) != null;
    }

    /** Gunpowder and any non-gun Flint'N'Powder item (shot, buckshot, ramrod...). */
    public static boolean isAmmoLike(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item == Items.GUNPOWDER || (FNP.equals(namespace(item)) && procedure(item) == null);
    }

    public static boolean isRamrod(ItemStack stack) {
        return stack != null && !stack.isEmpty() && isId(stack.getItem(), "ramrod");
    }

    public static Method procedure(Item item) {
        return PROCEDURES.computeIfAbsent(item, FnpGuns::resolve).orElse(null);
    }

    private static Optional<Method> resolve(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null || !FNP.equals(key.getNamespace()) || !GUN_IDS.contains(key.getPath())) {
            return Optional.empty();
        }
        String itemClass = item.getClass().getName();
        if (!itemClass.endsWith("Item") || !itemClass.contains(".item.")) {
            return Optional.empty();
        }
        String base = itemClass.substring(0, itemClass.length() - "Item".length());
        String procedureClass = base.replace(".item.", ".procedures.") + "RightclickedProcedure";
        try {
            Class<?> type = Class.forName(procedureClass, true, item.getClass().getClassLoader());
            return Optional.of(type.getMethod("execute",
                    LevelAccessor.class, double.class, double.class, double.class,
                    Entity.class, ItemStack.class));
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[recruits_flintnpowder] {} has no usable procedure {}: {}",
                    key, procedureClass, e.toString());
            return Optional.empty();
        }
    }

    private static String namespace(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key == null ? "" : key.getNamespace();
    }

    private static boolean isId(Item item, String path) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key != null && FNP.equals(key.getNamespace()) && path.equals(key.getPath());
    }
}
