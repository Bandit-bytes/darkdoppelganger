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
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> DOPPELGANGER_FINAL_PHASE_SPELLS;

    public static ForgeConfigSpec.DoubleValue MINION_HEALTH;
    public static ForgeConfigSpec.DoubleValue MINION_ATTACK_DAMAGE;
    public static ForgeConfigSpec.DoubleValue MINION_MOVEMENT_SPEED;
    public static ForgeConfigSpec.DoubleValue MINION_ARMOR;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // Dark Doppelganger Settings
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
                .comment("List of banned armor items (e.g., modid:item_name or modid:*) that should not be copied to the Dark Doppelganger")
                .defineListAllowEmpty(
                        "banned_armor",
                        () -> List.of(
                                "cataclysm:cursium_helmet",
                                "cataclysm:cursium_chestplate",
                                "cataclysm:cursium_leggings",
                                "cataclysm:cursium_boots",
                                "mycoolmod:*"
                        ),
                        obj -> obj instanceof String
                );

        DOPPELGANGER_FINAL_PHASE_SPELLS = builder
                .comment("List of all spells the Dark Doppelganger can use in the final phase (Hard mode must be enabled)")
                .defineListAllowEmpty(
                        "final_phase_spells",
                        () -> List.of(
                                "irons_spellbooks:eldritch_blast",
                                "irons_spellbooks:ray_of_frost",
                                "traveloptics:tidal_grasp",
                                "irons_spellbooks:abyssal_shroud",
                                "traveloptics:shadowed_miasma"
                        ),
                        obj -> obj instanceof String
                );

        builder.pop();

        // Minion Settings
        builder.comment("Minion Configuration").push("minion");

        MINION_HEALTH = builder.defineInRange("health", 40.0, 1.0, 1000.0);
        MINION_ATTACK_DAMAGE = builder.defineInRange("attack_damage", 6.0, 0.1, 100.0);
        MINION_MOVEMENT_SPEED = builder.defineInRange("movement_speed", 0.28, 0.01, 1.0);
        MINION_ARMOR = builder.defineInRange("armor", 4.0, 0.0, 100.0);

        builder.pop();

        COMMON_CONFIG = builder.build();
    }
}
