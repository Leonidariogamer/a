package dev.recruitsfnp;

import dev.recruitsfnp.config.CompatConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(RecruitsFlintnpowder.MOD_ID)
public class RecruitsFlintnpowder {
    public static final String MOD_ID = "recruits_flintnpowder";

    public RecruitsFlintnpowder() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CompatConfig.SPEC);
    }
}
