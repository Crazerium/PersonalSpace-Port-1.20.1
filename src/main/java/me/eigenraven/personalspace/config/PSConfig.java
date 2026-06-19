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

            builder.pop();
        }
    }

    private PSConfig() {
    }
}