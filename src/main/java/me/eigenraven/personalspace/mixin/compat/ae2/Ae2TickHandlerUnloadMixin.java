package me.eigenraven.personalspace.mixin.compat.ae2;

import me.eigenraven.personalspace.compat.ae2.Ae2LevelUnloadGuard;
import net.minecraftforge.event.level.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = "appeng.hooks.ticking.TickHandler",
        remap = false
)
public abstract class Ae2TickHandlerUnloadMixin {

    @Inject(
            method = "onUnloadLevel",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private void personalspace$beginAe2LevelUnload(
            LevelEvent.Unload event,
            CallbackInfo ci
    ) {
        Ae2LevelUnloadGuard.begin(event.getLevel());
    }

    @Inject(
            method = "onUnloadLevel",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private void personalspace$endAe2LevelUnload(
            LevelEvent.Unload event,
            CallbackInfo ci
    ) {
        Ae2LevelUnloadGuard.end(event.getLevel());
    }
}