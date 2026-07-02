package me.eigenraven.personalspace.compat.gtceu;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class PersonalSpaceGTCEuConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue PATCH_EXISTING_PERSONAL_SPACES_ON_STARTUP;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BEDROCK_FLUID_VEINS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("gtceu_bedrock_fluids");

        ENABLED = builder
                .comment("Enable GTCEu bedrock fluid veins in Personal Space dimensions.")
                .define("enabled", true);

        PATCH_EXISTING_PERSONAL_SPACES_ON_STARTUP = builder
                .comment(
                        "Patch all already existing Personal Space dimensions on server startup.",
                        "This scans all old ps_ and team_ dimension folders and adds them to GTCEu bedrock fluid vein data.",
                        "On servers with many Personal Space dimensions this can be very expensive and may cause a lot of dimensions/chunks to become active.",
                        "Recommended for normal servers: false.",
                        "Use true only once when you intentionally want to migrate/patch old Personal Space dimensions, then set it back to false and restart."
                )
                .define("patchExistingPersonalSpacesOnStartup", false);

        BEDROCK_FLUID_VEINS = builder
                .comment(
                        "GTCEu bedrock fluid veins for Personal Space dimensions.",
                        "",
                        "Entry format:",
                        "id|fluid|dimensions|weight|min_yield|max_yield|depletion_amount|depletion_chance|depleted_yield",
                        "",
                        "Dimensions can be:",
                        "- personalspace:ps_* = all Personal Space player dimensions",
                        "- personalspace:* = all Personal Space dimensions",
                        "- personalspace:ps_name = one exact dimension",
                        "",
                        "Multiple dimensions can be separated by comma:",
                        "personalspace:ps_crazer,personalspace:ps_player2",
                        "",
                        "Examples:",
                        "personalspace:void_oil_deposit|gtceu:oil|personalspace:ps_*|99|120|720|2|1|50",
                        "personalspace:void_natural_gas_deposit|gtceu:natural_gas|personalspace:ps_*|30|80|400|2|1|30",
                        "",
                        "Changes require a full game restart."
                )
                .defineListAllowEmpty(
                        List.of("veins"),
                        () -> List.of(
                                "personalspace:void_oil_deposit|gtceu:oil|personalspace:ps_*|99|120|720|2|1|50",
                                "personalspace:void_natural_gas_deposit|gtceu:natural_gas|personalspace:ps_*|30|80|400|2|1|30"
                        ),
                        value -> value instanceof String
                );

        builder.pop();

        SPEC = builder.build();
    }

    private PersonalSpaceGTCEuConfig() {
    }
}
