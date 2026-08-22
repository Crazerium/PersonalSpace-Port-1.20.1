package me.eigenraven.personalspace.mixin;

import me.eigenraven.personalspace.PersonalSpace;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelDimensionForGTConditionMixin {

    @Shadow
    @Final
    private ResourceKey<Level> dimension;

    @Inject(method = "dimension", at = @At("HEAD"), cancellable = true)
    private void personalspace$dimensionForGTChecks(CallbackInfoReturnable<ResourceKey<Level>> cir) {
        if (!this.dimension.location().getNamespace().equals(PersonalSpace.MODID)) {
            return;
        }

        if (!personalspace$shouldPretendOverworld()) {
            return;
        }

        cir.setReturnValue(Level.OVERWORLD);
    }

    @Unique
    private static boolean personalspace$shouldPretendOverworld() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            String methodName = element.getMethodName();

            if (
                    "com.gregtechceu.gtceu.common.recipe.condition.DimensionCondition".equals(className) &&
                            "testCondition".equals(methodName)
            ) {
                return true;
            }

        }

        return false;
    }
}