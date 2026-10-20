package dev.recruitsfnp.compat;

import dev.recruitsfnp.RecruitsFlintnpowder;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Every Flint'N'Powder gun kicks the shooter's view with {@code /execute as @p run teleport @p ...}.
 * "@p" is the nearest player, so when a recruit fires, that command would jolt the camera of
 * whichever player is standing closest. Cancel it while a recruit is the one shooting.
 */
@Mod.EventBusSubscriber(modid = RecruitsFlintnpowder.MOD_ID)
public final class RecoilGuard {
    private RecoilGuard() {
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (FnpGunHandler.running
                && event.getParseResults().getReader().getString().contains("teleport @p")) {
            event.setCanceled(true);
        }
    }
}
