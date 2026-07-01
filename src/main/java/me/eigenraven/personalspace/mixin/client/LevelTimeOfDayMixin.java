package me.eigenraven.personalspace.mixin.client;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.client.ClientPersonalSpaceSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionType.class)
public abstract class LevelTimeOfDayMixin {

    @Inject(method = "timeOfDay", at = @At("HEAD"), cancellable = true)
    private void personalspace$fixedVisualTime(long dayTime, CallbackInfoReturnable<Float> cir) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        ResourceLocation levelId = minecraft.level.dimension().location();

        if (!levelId.getNamespace().equals(PersonalSpace.MODID)) {
            return;
        }

        long fixedTime = ClientPersonalSpaceSettings.get(levelId).timeOfDay();

        cir.setReturnValue(personalspace$calculateTimeOfDay(fixedTime));
    }

    @Unique
    private static float personalspace$calculateTimeOfDay(long time) {
        double dayProgress = Mth.frac((double) time / 24000.0D - 0.25D);
        double curve = 0.5D - Math.cos(dayProgress * Math.PI) / 2.0D;
        return (float) (dayProgress * 2.0D + curve) / 3.0F;
    }
}
