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
        }
    }

    private PSConfig() {
    }
}