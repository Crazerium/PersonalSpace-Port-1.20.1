package me.eigenraven.personalspace.mixin;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelWeatherServerMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void personalspace$getRainLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        Level level = (Level) (Object) this;

        if (!personalspace$isPersonalSpace(level)) {
            return;
        }

        cir.setReturnValue(0.0F);
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void personalspace$getThunderLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
        Level level = (Level) (Object) this;

        if (!personalspace$isPersonalSpace(level)) {
            return;
        }

        cir.setReturnValue(0.0F);
    }

    @Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
    private void personalspace$isRaining(CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;

        if (!personalspace$isPersonalSpace(level)) {
            return;
        }

        cir.setReturnValue(false);
    }

    @Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
    private void personalspace$isThundering(CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;

        if (!personalspace$isPersonalSpace(level)) {
            return;
        }

        cir.setReturnValue(false);
    }

    @Unique
    private static boolean personalspace$isPersonalSpace(Level level) {
        ResourceLocation levelId = level.dimension().location();
        return levelId.getNamespace().equals(PersonalSpace.MODID);
    }
}