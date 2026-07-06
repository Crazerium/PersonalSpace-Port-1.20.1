package me.eigenraven.personalspace.compat.gtceu;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import me.eigenraven.personalspace.PersonalSpace;

@GTAddon
public class PersonalSpaceGTAddon implements IGTAddon {
    public static final GTRegistrate REGISTRATE = GTRegistrate.create(PersonalSpace.MODID);

    public PersonalSpaceGTAddon() {
        PersonalSpace.LOGGER.warn("PersonalSpaceGTAddon CONSTRUCTOR called.");
    }

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
        PersonalSpace.LOGGER.warn("PersonalSpaceGTAddon.initializeAddon() called.");
    }

    @Override
    public void registerFluidVeins() {
        PersonalSpace.LOGGER.warn("PersonalSpaceGTAddon.registerFluidVeins() called.");
        PersonalSpaceBedrockFluidVeins.init();
    }
}