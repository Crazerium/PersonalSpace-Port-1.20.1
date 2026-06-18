package me.eigenraven.personalspace.network;

import me.eigenraven.personalspace.block.PortalBlockEntity;
import me.eigenraven.personalspace.data.PersonalSpaceData;
import me.eigenraven.personalspace.dimension.PSDimensions;
import me.eigenraven.personalspace.registry.PSBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CreateDimensionPacket {
    private final PersonalSpaceData.WorldType type;
    private final int height;
    private final BlockPos overworldPortalPos;

    public CreateDimensionPacket(PersonalSpaceData.WorldType type, int height, BlockPos overworldPortalPos) {
        this.type = type;
        this.height = height;
        this.overworldPortalPos = overworldPortalPos;
    }

    public static void encode(CreateDimensionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeInt(msg.height);
        buf.writeBlockPos(msg.overworldPortalPos);
    }

    public static CreateDimensionPacket decode(FriendlyByteBuf buf) {
        return new CreateDimensionPacket(
                buf.readEnum(PersonalSpaceData.WorldType.class),
                buf.readInt(),
                buf.readBlockPos()
        );
    }

    public static void handle(CreateDimensionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;


            ResourceKey<Level> levelKey = PSDimensions.randomPersonalKey();
            ServerLevel newLevel = PSDimensions.createPersonalDimension(player.server, levelKey, msg.type, msg.height);


            PersonalSpaceData data = PersonalSpaceData.load(newLevel);
            data.setReturnLevel(player.level().dimension().location().toString());
            data.setReturnPos(player.blockPosition());
            PersonalSpaceData.save(newLevel, data);


            newLevel.getChunkSource().getChunkFuture(0, 0, ChunkStatus.FULL, true).join();


            BlockPos portalPos = new BlockPos(7, msg.height + 1, 7);
            BlockState portalState = PSBlocks.PERSONAL_PORTAL.get().defaultBlockState();
            newLevel.setBlock(portalPos, portalState, 3);

            PortalBlockEntity portalBE = (PortalBlockEntity) newLevel.getBlockEntity(portalPos);
            if (portalBE == null) {
                portalBE = new PortalBlockEntity(portalPos, portalState);
                newLevel.setBlockEntity(portalBE);
            }
            ResourceKey<Level> returnKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(data.getReturnLevel()));
            portalBE.setTarget(returnKey, data.getReturnPos());


            ServerLevel overworld = player.server.overworld();
            BlockEntity overworldBE = overworld.getBlockEntity(msg.overworldPortalPos);
            if (overworldBE instanceof PortalBlockEntity overworldPortal) {
                overworldPortal.setTarget(levelKey, portalPos);
            }


            player.teleportTo(newLevel, 7.5, msg.height + 1, 7.5, 0, 0);
        });
        ctx.get().setPacketHandled(true);
    }
}