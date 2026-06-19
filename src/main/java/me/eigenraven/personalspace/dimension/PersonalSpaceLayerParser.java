package me.eigenraven.personalspace.dimension;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class PersonalSpaceLayerParser {
    private PersonalSpaceLayerParser() {
    }

    public record Layer(BlockState state, int count) {
    }

    public static List<Layer> parse(String preset) {
        List<Layer> layers = new ArrayList<>();

        if (preset == null || preset.isBlank()) {
            return layers;
        }

        String[] parts = preset.split(";");

        for (String rawPart : parts) {
            String part = rawPart.trim();

            if (part.isEmpty()) {
                continue;
            }

            String blockId = part;
            int count = 1;

            int starIndex = part.lastIndexOf('*');

            if (starIndex >= 0) {
                blockId = part.substring(0, starIndex).trim();

                try {
                    count = Integer.parseInt(part.substring(starIndex + 1).trim());
                } catch (NumberFormatException ignored) {
                    count = 1;
                }
            }

            if (count <= 0) {
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(blockId);

            if (id == null) {
                continue;
            }

            BlockState state = BuiltInRegistries.BLOCK
                    .getOptional(id)
                    .orElse(Blocks.AIR)
                    .defaultBlockState();

            if (state.isAir()) {
                continue;
            }

            layers.add(new Layer(state, Math.min(count, 256)));
        }

        return layers;
    }
}