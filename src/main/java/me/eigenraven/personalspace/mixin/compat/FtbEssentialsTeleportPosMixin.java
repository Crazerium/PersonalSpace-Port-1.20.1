package me.eigenraven.personalspace.mixin.compat;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.dimension.PSDimensions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbessentials.util.TeleportPos", remap = false)
public abstract class FtbEssentialsTeleportPosMixin {
    @Shadow(remap = false)
    @Final
    private ResourceKey<Level> dimension;

    @Inject(
            method = "teleport(Lnet/minecraft/server/level/ServerPlayer;)Ldev/ftb/mods/ftbessentials/util/TeleportPos$TeleportResult;",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private void personalspace$loadLazyDimension(
            ServerPlayer player,
            CallbackInfoReturnable<Object> callback
    ) {
        if (player == null
                || player.getServer().getLevel(this.dimension) != null
                || !PSDimensions.isPersonalSpaceDimension(this.dimension.location())) {
            return;
        }

        try {
            PSDimensions.getOrCreate(player.getServer(), this.dimension);
        } catch (RuntimeException exception) {
            PersonalSpace.LOGGER.error(
                    "Failed to lazy-load Personal Space {} before FTB Essentials teleport",
                    this.dimension.location(),
                    exception
            );
        }
    }
}
