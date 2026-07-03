package me.eigenraven.personalspace.block;

import me.eigenraven.personalspace.PersonalSpace;
import me.eigenraven.personalspace.registry.PSBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PersonalAirIntakeBlockEntity extends BlockEntity {
    private static final ResourceLocation GTCEU_AIR_ID = new ResourceLocation("gtceu", "air");

    private static final int CAPACITY = 256_000;
    private static final int AIR_PER_SECOND = 32_000;
    private static final int PUSH_PER_SIDE = 32_000;

    private int tickCounter = 0;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return isAir(stack);
        }
    };

    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> tank);

    public PersonalAirIntakeBlockEntity(BlockPos pos, BlockState state) {
        super(PSBlockEntities.PERSONAL_AIR_INTAKE.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            PersonalAirIntakeBlockEntity blockEntity
    ) {
        if (level.isClientSide) {
            return;
        }

        if (!blockEntity.isInPersonalSpace(level)) {
            return;
        }

        blockEntity.tickCounter++;

        if (blockEntity.tickCounter < 20) {
            return;
        }

        blockEntity.tickCounter = 0;

        blockEntity.generateAir();
        blockEntity.pushAirToNeighbors();
        blockEntity.setChanged();
    }

    private void generateAir() {
        Fluid air = getAirFluid();

        if (air == Fluids.EMPTY) {
            return;
        }

        int freeSpace = tank.getCapacity() - tank.getFluidAmount();

        if (freeSpace <= 0) {
            return;
        }

        int amount = Math.min(AIR_PER_SECOND, freeSpace);

        tank.fill(
                new FluidStack(air, amount),
                IFluidHandler.FluidAction.EXECUTE
        );
    }

    private void pushAirToNeighbors() {
        if (level == null || tank.isEmpty()) {
            return;
        }

        for (Direction direction : Direction.values()) {
            if (tank.isEmpty()) {
                return;
            }

            BlockPos neighborPos = worldPosition.relative(direction);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            if (neighbor == null) {
                continue;
            }

            Direction neighborSide = direction.getOpposite();

            neighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, neighborSide).ifPresent(handler -> {
                if (tank.isEmpty()) {
                    return;
                }

                FluidStack simulatedDrain = tank.drain(
                        Math.min(PUSH_PER_SIDE, tank.getFluidAmount()),
                        IFluidHandler.FluidAction.SIMULATE
                );

                if (simulatedDrain.isEmpty()) {
                    return;
                }

                int accepted = handler.fill(
                        simulatedDrain,
                        IFluidHandler.FluidAction.EXECUTE
                );

                if (accepted > 0) {
                    tank.drain(
                            accepted,
                            IFluidHandler.FluidAction.EXECUTE
                    );
                }
            });
        }
    }

    private static Fluid getAirFluid() {
        Fluid fluid = BuiltInRegistries.FLUID.get(GTCEU_AIR_ID);
        ResourceLocation actualId = BuiltInRegistries.FLUID.getKey(fluid);

        if (!GTCEU_AIR_ID.equals(actualId)) {
            return Fluids.EMPTY;
        }

        return fluid;
    }

    private static boolean isAir(FluidStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        return GTCEU_AIR_ID.equals(id);
    }

    private boolean isInPersonalSpace(Level level) {
        ResourceLocation dimensionId = level.dimension().location();
        return PersonalSpace.MODID.equals(dimensionId.getNamespace());
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("Tank")) {
            tank.readFromNBT(tag.getCompound("Tank"));
        }

        tickCounter = tag.getInt("TickCounter");
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(
            @Nonnull Capability<T> capability,
            @Nullable Direction side
    ) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
        }

        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }
}