package net.bandit.darkdoppelganger;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class Config {

    public static final ForgeConfigSpec COMMON_CONFIG;
    public static ForgeConfigSpec.DoubleValue DOPPELGANGER_HEALTH;
    public static ForgeConfigSpec.DoubleValue DOPPELGANGER_ATTACK_DAMAGE;
    public static ForgeConfigSpec.DoubleValue DOPPELGANGER_MOVEMENT_SPEED;
    public static ForgeConfigSpec.DoubleValue DOPPELGANGER_KNOCKBACK_RESISTANCE;
    public static ForgeConfigSpec.DoubleValue DOPPELGANGER_ARMOR;
    public static ForgeConfigSpec.DoubleValue DOPPELGANGER_FOLLOW_RANGE;
    public static ForgeConfigSpec.BooleanValue DOPPLEGANGER_HARD_MODE;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> DOPPELGANGER_BANNED_ARMOR;


    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Dark Doppelganger Configuration").push("dark_doppelganger");

        DOPPELGANGER_HEALTH = builder
                .comment("Health of the Dark Doppelganger")
                .defineInRange("health", 6000.0, 1.0, 100000.0);

        DOPPELGANGER_ATTACK_DAMAGE = builder
                .comment("Attack Damage of the Dark Doppelganger")
                .defineInRange("attack_damage", 20.0, 1.0, 100.0);

        DOPPELGANGER_MOVEMENT_SPEED = builder
                .comment("Movement Speed of the Dark Doppelganger")
                .defineInRange("movement_speed", 0.20, 0.1, 1.0);

        DOPPELGANGER_KNOCKBACK_RESISTANCE = builder
                .comment("Knockback Resistance of the Dark Doppelganger")
                .defineInRange("knockback_resistance", 0.6, 0.0, 1.0);

        DOPPELGANGER_ARMOR = builder
                .comment("Armor of the Dark Doppelganger")
                .defineInRange("armor", 20.0, 0.0, 100.0);

        DOPPELGANGER_FOLLOW_RANGE = builder
                .comment("Follow Range of the Dark Doppelganger")
                .defineInRange("follow_range", 64.0, 1.0, 128.0);

        DOPPLEGANGER_HARD_MODE = builder
                .comment("Hard mode of the Dark Doppelganger")
                .define("hard_mode", false);

        DOPPELGANGER_BANNED_ARMOR = builder
                .comment("List of banned armor items (format: modid:item_name) that should not be copied to the Dark Doppelganger")
                .defineListAllowEmpty(
                        "banned_armor",
                        () -> List.of(
                                "cataclysm:cursium_helmet",
                                "cataclysm:cursium_chestplate",
                                "cataclysm:cursium_leggings",
                                "cataclysm:cursium_boots"
                        ),
                        obj -> obj instanceof String
                );


        builder.pop();
        COMMON_CONFIG = builder.build();
    }
}
