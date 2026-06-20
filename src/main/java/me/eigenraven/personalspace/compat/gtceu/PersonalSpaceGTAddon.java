package me.eigenraven.personalspace.compat.gtceu;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import me.eigenraven.personalspace.PersonalSpace;

@GTAddon
public final class PersonalSpaceGTAddon implements IGTAddon {
    public static final GTRegistrate REGISTRATE = GTRegistrate.create(PersonalSpace.MODID);

    @Override
    public String addonModId() {
        return PersonalSpace.MODID;
    }

    @Override
    public GTRegistrate getRegistrate() {
        return REGISTRATE;
    }

    @Override
    public void initializeAddon() {
    }

    @Override
    public void registerFluidVeins() {
        PersonalSpace.LOGGER.info("GTCEu addon registerFluidVeins() called.");
        PersonalSpaceBedrockFluidVeins.init();
    }
}