package me.eigenraven.personalspace.mixin;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceRuntimeSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelTimeOfDayServerMixin {

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void personalspace$getDayTime(CallbackInfoReturnable<Long> cir) {
        Level level = (Level) (Object) this;

        if (!level.dimension().location().getNamespace().equals(PersonalSpace.MODID)) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long fixedTime = PersonalSpaceRuntimeSettings.getTimeOfDay(serverLevel) % 24000L;

        if (fixedTime < 0L) {
            fixedTime += 24000L;
        }

        cir.setReturnValue(fixedTime);
    }
}
