package me.eigenraven.personalspace.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PSConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        SERVER = new Server(builder);
        SERVER_SPEC = builder.build();
    }

    public static final class Server {
        public final ForgeConfigSpec.IntValue maxPersonalSpaceSizeBlocks;
        public final ForgeConfigSpec.IntValue maxPersonalDimensionsPerPlayer;
        public final ForgeConfigSpec.BooleanValue autoUnloadPersonalSpaces;
        public final ForgeConfigSpec.IntValue autoUnloadDelaySeconds;
        public final ForgeConfigSpec.BooleanValue privacyEnabled;
        public final ForgeConfigSpec.BooleanValue privacyProtectExisting;
        public final ForgeConfigSpec.BooleanValue privacyAllowTeamMembers;
        public final ForgeConfigSpec.BooleanValue privacyAllowOpBypass;
        public final ForgeConfigSpec.BooleanValue privacyProtectContainers;
        public final ForgeConfigSpec.BooleanValue privacyProtectEntities;
        public final ForgeConfigSpec.BooleanValue privacyProtectFromPlayerExplosions;

        private Server(ForgeConfigSpec.Builder builder) {
            builder.push("personal_space_limits");

            maxPersonalSpaceSizeBlocks = builder
                    .comment(
                            "Maximum size of each Personal Space in blocks.",
                            "This is the full width/length of the allowed area.",
                            "Example: 10000 means the Personal Space is limited to 10000 x 10000 blocks.",
                            "Players may travel about 5000 blocks from the center in each direction.",
                            "Set to 0 to disable the limit."
                    )
                    .defineInRange("maxPersonalSpaceSizeBlocks", 10000, 0, 100000);

            maxPersonalDimensionsPerPlayer = builder
                    .comment(
                            "Maximum number of personal dimensions each player may create.",
                            "This limit applies only to player-owned dimensions: ps_<player>, ps_<player>_2, etc.",
                            "FTB Teams dimensions are handled separately: one team can have only one team dimension.",
                            "Set to 0 to disable the personal dimension count limit."
                    )
                    .defineInRange("maxPersonalDimensionsPerPlayer", 1, 0, 1000);

            builder.pop();

            builder.push("lazy_loading");

            autoUnloadPersonalSpaces = builder
                    .comment(
                            "Automatically unload idle Personal Space dimensions.",
                            "A dimension is kept loaded while it has online players, Forge/FTB forced chunks,",
                            "or an offline player who logged out inside it."
                    )
                    .define("autoUnloadPersonalSpaces", true);

            autoUnloadDelaySeconds = builder
                    .comment(
                            "How long a Personal Space must stay idle before it is unloaded.",
                            "The unload is performed one dimension at a time."
                    )
                    .defineInRange("autoUnloadDelaySeconds", 300, 30, 86400);

            builder.pop();

            builder.push("privacy");

            privacyEnabled = builder
                    .comment(
                            "Protect the whole Personal Space dimension without creating FTB Chunks claims.",
                            "The protection therefore does not consume claimed-chunk limits."
                    )
                    .define("enabled", true);

            privacyProtectExisting = builder
                    .comment(
                            "Automatically infer and store owners for old ps_ and team_ dimensions.",
                            "Migration is lazy and disk-based; dimensions are not loaded just for this."
                    )
                    .define("protectExistingPersonalSpaces", true);

            privacyAllowTeamMembers = builder
                    .comment("Allow members of the owner's current FTB Team to build and interact.")
                    .define("allowTeamMembers", true);

            privacyAllowOpBypass = builder
                    .comment("Allow operators with permission level 2 or higher to bypass protection.")
                    .define("allowOpBypass", true);

            privacyProtectContainers = builder
                    .comment("Block unauthorized use of blocks, machines, inventories, buckets and similar interactions.")
                    .define("protectContainersAndMachines", true);

            privacyProtectEntities = builder
                    .comment("Block unauthorized entity interaction and attacks inside Personal Spaces.")
                    .define("protectEntities", true);

            privacyProtectFromPlayerExplosions = builder
                    .comment(
                            "Prevent block damage from explosions directly caused by unauthorized players.",
                            "Automatic machine explosions without a player source are not changed."
                    )
                    .define("protectFromUnauthorizedPlayerExplosions", true);

            builder.pop();
        }
    }

    private PSConfig() {
    }
}