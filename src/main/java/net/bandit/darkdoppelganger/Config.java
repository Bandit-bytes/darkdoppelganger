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

    public static ForgeConfigSpec.DoubleValue SPELL_MINION_HEALTH;

    public static ForgeConfigSpec.DoubleValue MINION_HEALTH;
    public static ForgeConfigSpec.DoubleValue MINION_ATTACK_DAMAGE;
    public static ForgeConfigSpec.DoubleValue MINION_MOVEMENT_SPEED;
    public static ForgeConfigSpec.DoubleValue MINION_ARMOR;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> MINION_SPELLS;
    public static ForgeConfigSpec.ConfigValue<String> MINION_BARRAGE_SPELL;
    public static ForgeConfigSpec.DoubleValue DOPPELGANGER_DAMAGE_CAP;
    public static ForgeConfigSpec.DoubleValue DOPPELGANGER_SPELL_POWER_MULTIPLIER;
    public static ForgeConfigSpec.BooleanValue ALTAR_END_ONLY;


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

        DOPPELGANGER_DAMAGE_CAP = builder
                .comment(
                        "Maximum damage the Dark Doppelganger can take from a single hit.",
                        "Set to 0 to disable the cap."
                )
                .defineInRange("damage_cap_per_hit", 150.0, 0.0, 100000.0);

        DOPPELGANGER_SPELL_POWER_MULTIPLIER = builder
                .comment(
                        "Multiplier applied to all of the Dark Doppelganger's spell power attributes",
                        "after copying them from the summoner.",
                        "1.0 = same as player, 1.5 = +50%, 2.0 = double, etc."
                )
                .defineInRange("spell_power_multiplier", 1.25, 0.0, 10.0);


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
                                "irons_spellbooks:shadow_slash",
                                "irons_spellbooks:oakskin",
                                "traveloptics:tidal_grasp",
                                "traveloptics:spectral_blink",
                                "traveloptics:shadowed_miasma"
                        ),
                        obj -> obj instanceof String
                );

        builder.pop();

        builder.comment("Summoning Configuration").push("summoning");

        ALTAR_END_ONLY = builder
                .comment("When true, the Shadow Altar can only summon the Dark Doppelganger in The End")
                .define("altar_end_only", true);

        builder.pop();

        // Minion Settings
        builder.comment("Minion Configuration").push("minion");

        MINION_HEALTH = builder.defineInRange("health", 100.0, 1.0, 1000.0);
        SPELL_MINION_HEALTH = builder.comment("Health of spell-created minion summon").defineInRange("spell_minion_health", 200.0, 1.0, 1000.0);
        MINION_ATTACK_DAMAGE = builder.defineInRange("attack_damage", 8.0, 0.1, 100.0);
        MINION_MOVEMENT_SPEED = builder.defineInRange("movement_speed", 0.28, 0.01, 1.0);
        MINION_ARMOR = builder.defineInRange("armor", 5.0, 0.0, 100.0);
        MINION_BARRAGE_SPELL = builder
                .comment("Spell used for the minion's SpellBarrageGoal (string id)")
                .define("barrage_spell", "irons_spellbooks:devour");

        MINION_SPELLS = builder
                .comment(
                        "List of spells the minion can use (string ids).",
                        "These will be shuffled and split into groups for the Warlock attack goal."
                )
                .defineListAllowEmpty(
                        "spells",
                        () -> List.of(
                                "traveloptics:spectral_blink",
                                "irons_spellbooks:burning_dash",
                                "irons_spellbooks:charge",
                                "irons_spellbooks:oakskin",
                                "irons_spellbooks:shadow_slash",
                                "irons_spellbooks:invisibility"
                        ),
                        obj -> obj instanceof String
                );


        builder.pop();

        COMMON_CONFIG = builder.build();
    }
}
