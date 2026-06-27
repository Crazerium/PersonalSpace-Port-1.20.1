package me.eigenraven.personalspace.mixin;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelIsDayMixin {

    @Inject(method = "isDay", at = @At("HEAD"), cancellable = true)
    private void personalspace$isDay(CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;

        if (!level.dimension().location().getNamespace().equals(PersonalSpace.MODID)) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long time = PersonalSpaceData.load(serverLevel).getTimeOfDay();

        long dayTime = time % 24000L;
        if (dayTime < 0L) {
            dayTime += 24000L;
        }

        cir.setReturnValue(dayTime < 12000L);
    }
}