package me.eigenraven.personalspace.compat.gtceu;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class PersonalSpaceGTCEuConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BEDROCK_FLUID_VEINS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("gtceu_bedrock_fluids");

        ENABLED = builder
                .comment("Enable GTCEu bedrock fluid veins in Personal Space dimensions.")
                .define("enabled", true);

        BEDROCK_FLUID_VEINS = builder
                .comment(
                        "GTCEu bedrock fluid veins for Personal Space dimensions.",
                        "",
                        "Entry format:",
                        "id|fluid|dimensions|weight|min_yield|max_yield|depletion_amount|depletion_chance|depleted_yield",
                        "",
                        "Dimensions can be:",
                        "- personalspace:ps_*  = all Personal Space player dimensions",
                        "- personalspace:*     = all Personal Space dimensions",
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