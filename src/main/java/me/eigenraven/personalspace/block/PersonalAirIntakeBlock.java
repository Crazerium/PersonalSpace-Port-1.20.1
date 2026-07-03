package me.eigenraven.personalspace.block;

import me.eigenraven.personalspace.registry.PSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.Nullable;

public final class PersonalAirIntakeBlock extends BaseEntityBlock {
    public static final BooleanProperty RETURN_PORTAL = BooleanProperty.create("return_portal");

    public PersonalAirIntakeBlock(Properties properties) {
        super(properties.noOcclusion().lightLevel(state -> 8));

        registerDefaultState(stateDefinition.any()
                .setValue(RETURN_PORTAL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder builder) {
        builder.add(RETURN_PORTAL);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PersonalAirIntakeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return createTickerHelper(
                type,
                PSBlockEntities.PERSONAL_AIR_INTAKE.get(),
                PersonalAirIntakeBlockEntity::serverTick
        );
    }

    @Override
    public void onRemove(
            BlockState oldState,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean moving
    ) {
        if (!oldState.is(newState.getBlock())) {
            level.removeBlockEntity(pos);
        }

        super.onRemove(oldState, level, pos, newState, moving);
    }
}