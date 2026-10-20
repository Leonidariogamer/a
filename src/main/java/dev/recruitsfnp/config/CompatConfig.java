package dev.recruitsfnp.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CompatConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.IntValue RANGE;
    public static final ForgeConfigSpec.IntValue RELOAD_STEP_TICKS;
    public static final ForgeConfigSpec.IntValue PRIME_TICKS;
    public static final ForgeConfigSpec.IntValue FIRE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue CONSUME_AMMO;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_RAMROD_ITEM;
    public static final ForgeConfigSpec.BooleanValue AVOID_FRIENDLY_FIRE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ENABLED = b.comment("Master switch. When false, recruits behave exactly as without this mod.")
                .define("enabled", true);
        RANGE = b.comment("Maximum distance (blocks) at which a recruit fires a Flint'N'Powder gun.")
                .defineInRange("range", 30, 5, 100);
        RELOAD_STEP_TICKS = b.comment("Ticks a recruit spends on each reload step (powder, ball, ramrod).")
                .defineInRange("reloadStepTicks", 25, 1, 400);
        PRIME_TICKS = b.comment("Ticks between priming/cocking the gun and pulling the trigger.")
                .defineInRange("primeTicks", 15, 1, 400);
        FIRE_COOLDOWN_TICKS = b.comment("Ticks a recruit waits after a shot before the next action.")
                .defineInRange("fireCooldownTicks", 40, 1, 400);
        CONSUME_AMMO = b.comment("Consume gunpowder and shot from the recruit's inventory while reloading.")
                .define("consumeAmmo", true);
        REQUIRE_RAMROD_ITEM = b.comment("If true the recruit must carry a Ramrod item. If false it ramrods for free.")
                .define("requireRamrodItem", false);
        AVOID_FRIENDLY_FIRE = b.comment("Do not pull the trigger when an ally is in the line of fire.")
                .define("avoidFriendlyFire", true);
        SPEC = b.build();
    }

    private CompatConfig() {
    }
}
