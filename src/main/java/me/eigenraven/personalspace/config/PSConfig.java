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
        }
    }

    private PSConfig() {
    }
}