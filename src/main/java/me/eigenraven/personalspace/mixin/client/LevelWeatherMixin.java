package me.eigenraven.personalspace.mixin.client;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.client.ClientPersonalSpaceSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelWeatherMixin {

    @Unique
    private static boolean personalspace$loggedWeatherOnce = false;

    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void personalspace$disableVisualRain(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (personalspace$shouldDisableWeather()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void personalspace$disableVisualThunder(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (personalspace$shouldDisableWeather()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void personalspace$disableIsRaining(CallbackInfoReturnable<Boolean> cir) {
        if (personalspace$shouldDisableWeather()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
    private void personalspace$disableIsThundering(CallbackInfoReturnable<Boolean> cir) {
        if (personalspace$shouldDisableWeather()) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean personalspace$shouldDisableWeather() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return false;
        }

        ResourceLocation levelId = minecraft.level.dimension().location();

        if (!levelId.getNamespace().equals(PersonalSpace.MODID)) {
            return false;
        }

        boolean weatherEnabled = ClientPersonalSpaceSettings.get(levelId).weatherEnabled();

        if (weatherEnabled) {
            return false;
        }

        if (!personalspace$loggedWeatherOnce) {
            PersonalSpace.LOGGER.info("PersonalSpace visual weather mixin is active for {}", levelId);
            personalspace$loggedWeatherOnce = true;
        }

        return true;
    }
}