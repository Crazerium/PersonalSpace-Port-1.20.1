package me.eigenraven.personalspace.mixin.compat.ae2;

import me.eigenraven.personalspace.compat.ae2.Ae2LevelUnloadGuard;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = "appeng.blockentity.AEBaseBlockEntity",
        remap = false
)
public abstract class Ae2BaseBlockEntityUnloadMixin {

    @Inject(
            method = "markForUpdate",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void personalspace$skipUpdateWhileLevelUnloads(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();

        if (Ae2LevelUnloadGuard.isUnloading(level)) {
            ci.cancel();
        }
    }
}