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

    /**
     * Gun registry path -> name of its right-click procedure class. Names are not uniform
     * (BayonetMusketRightClickedProcedure, GasOperatedShotgunRightClickedProcedure...), so they are
     * listed from the bytecode of each item's use() rather than guessed. Bayonet bolt action
     * rifles share the bolt action rifle's procedure. The Log Cannon is left out: it is
     * ignited with flint and steel and loaded from an inventory tick, not by hand.
     */
    private static final Map<String, String> GUN_PROCEDURES = Map.ofEntries(
            // powder -> shot -> ramrod muzzle loaders
            Map.entry("musket", "MusketRightclickedProcedure"),
            Map.entry("bayonet_musket", "BayonetMusketRightClickedProcedure"),
            Map.entry("rifle", "RifleRightclickedProcedure"),
            Map.entry("pistol", "PistolRightclickedProcedure"),
            Map.entry("arquebus", "ArquebusRightclickedProcedure"),
            Map.entry("hand_gonne", "HandGonneRightclickedProcedure"),
            Map.entry("blunderbuss", "BlunderbussRightclickedProcedure"),
            Map.entry("bruttbuss", "BruttbussRightclickedProcedure"),
            Map.entry("big_game", "BigGameRightclickedProcedure"),
            Map.entry("flinter", "FlinterRightclickedProcedure"),
            Map.entry("volleygun", "VolleygunRightClickedProcedure"),
            // cartridge guns
            Map.entry("bolt_action_rifle", "BoltActionRifleRightclickedProcedure"),
            Map.entry("bayonet_bolt_action_rifle", "BoltActionRifleRightclickedProcedure"),
            Map.entry("trapdoor_rifle", "TrapdoorRifleRightclickedProcedure"),
            Map.entry("shotgun", "ShotgunRightclickedProcedure"),
            Map.entry("break_action_coachgun", "BreakActionCoachgunRightclickedProcedure"),
            Map.entry("single_action_revolver", "SingleActionRevolverRightclickedProcedure"),
            // magazine guns
            Map.entry("closed_bolt_mag_rifle", "ClosedBoltMagRifleRightclickedProcedure"),
            Map.entry("blowback_handgun", "BlowbackHandgunRightclickedProcedure"),
            Map.entry("open_bolt_smg", "OpenBoltSMGRightclickedProcedure"),
            Map.entry("gas_operated_shotgun", "GasOperatedShotgunRightClickedProcedure"));

    /** Magazine guns eject an empty magazine with a sneak + empty-hand click. */
    private static final Set<String> MAGAZINE_GUNS = Set.of(
            "closed_bolt_mag_rifle", "blowback_handgun", "open_bolt_smg", "gas_operated_shotgun");

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
        if (key == null || !FNP.equals(key.getNamespace()) || !GUN_PROCEDURES.containsKey(key.getPath())) {
            return Optional.empty();
        }
        String itemClass = item.getClass().getName();
        int itemPackage = itemClass.lastIndexOf(".item.");
        if (itemPackage < 0) {
            return Optional.empty();
        }
        String procedureClass = itemClass.substring(0, itemPackage) + ".procedures."
                + GUN_PROCEDURES.get(key.getPath());
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

    public static boolean isMagazineGun(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && FNP.equals(key.getNamespace()) && MAGAZINE_GUNS.contains(key.getPath());
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
