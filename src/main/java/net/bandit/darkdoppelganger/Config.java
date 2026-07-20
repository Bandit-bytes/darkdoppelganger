package net.bandit.darkdoppelganger;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

@EventBusSubscriber(modid = DarkDoppelgangerMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.DoubleValue DOPPELGANGER_HEALTH;
    public static final ModConfigSpec.DoubleValue DOPPELGANGER_ATTACK_DAMAGE;
    public static final ModConfigSpec.DoubleValue DOPPELGANGER_MOVEMENT_SPEED;
    public static final ModConfigSpec.DoubleValue DOPPELGANGER_KNOCKBACK_RESISTANCE;
    public static final ModConfigSpec.DoubleValue DOPPELGANGER_ARMOR;
    public static final ModConfigSpec.DoubleValue DOPPELGANGER_FOLLOW_RANGE;
    public static final ModConfigSpec.BooleanValue DOPPELGANGER_HARD_MODE;
    public static final ModConfigSpec.DoubleValue MINION_HEALTH;
    public static final ModConfigSpec.DoubleValue MINION_ATTACK_DAMAGE;
    public static final ModConfigSpec.DoubleValue MINION_MOVEMENT_SPEED;
    public static final ModConfigSpec.DoubleValue MINION_ARMOR;
    public static final ModConfigSpec.DoubleValue DOPPELGANGER_SPELL_POWER_MULTIPLIER;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MINION_SPELLS;
    public static final ModConfigSpec.ConfigValue<String> MINION_BARRAGE_SPELL;
    public static final ModConfigSpec.ConfigValue<String> DOPPELGANGER_DEFAULT_MAINHAND;
    public static final ModConfigSpec.ConfigValue<String> DOPPELGANGER_DEFAULT_HELMET;
    public static final ModConfigSpec.ConfigValue<String> DOPPELGANGER_DEFAULT_CHESTPLATE;
    public static final ModConfigSpec.ConfigValue<String> DOPPELGANGER_DEFAULT_LEGGINGS;
    public static final ModConfigSpec.ConfigValue<String> DOPPELGANGER_DEFAULT_BOOTS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DOPPELGANGER_BANNED_ARMOR;
    public static final ModConfigSpec.BooleanValue DOPPELGANGER_COPY_PLAYER_ARMOR;
    public static final ModConfigSpec.BooleanValue DOPPELGANGER_COPY_PLAYER_MAINHAND;
    public static final ModConfigSpec.BooleanValue ALTAR_END_ONLY;

    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_PHASE1_SPELLS_A;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_PHASE1_SPELLS_B;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_PHASE1_SPELLS_C;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_PHASE1_SPELLS_D;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_PHASE2_SPELLS_A;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_PHASE2_SPELLS_B;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_PHASE2_SPELLS_C;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_PHASE2_SPELLS_D;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_PHASE3_SPELLS_A;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_PHASE3_SPELLS_B;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_PHASE3_SPELLS_C;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_PHASE3_SPELLS_D;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_FINAL_SPELLS_A;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_FINAL_SPELLS_B;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_FINAL_SPELLS_C;
    public static ModConfigSpec.ConfigValue<List<? extends String>> DOPPEL_FINAL_SPELLS_D;




    public static final ModConfigSpec SPEC;
    static {
        BUILDER.comment("Dark Doppelganger Configuration").push("dark_doppelganger");

        DOPPELGANGER_HEALTH = BUILDER
                .comment("Health of the Dark Doppelganger")
                .defineInRange("health", 600.0, 1.0, 100000.0);

        DOPPELGANGER_ATTACK_DAMAGE = BUILDER
                .comment("Attack Damage of the Dark Doppelganger")
                .defineInRange("attack_damage", 10.0, 0.1, 1000.0);

        DOPPELGANGER_MOVEMENT_SPEED = BUILDER
                .comment("Movement Speed of the Dark Doppelganger")
                .defineInRange("movement_speed", 0.20, 0.01, 2.0);

        DOPPELGANGER_KNOCKBACK_RESISTANCE = BUILDER
                .comment("Knockback Resistance of the Dark Doppelganger")
                .defineInRange("knockback_resistance", 0.6, 0.0, 1.0);

        DOPPELGANGER_ARMOR = BUILDER
                .comment("Armor of the Dark Doppelganger")
                .defineInRange("armor", 10.0, 0.0, 100.0);

        DOPPELGANGER_FOLLOW_RANGE = BUILDER
                .comment("Follow Range of the Dark Doppelganger")
                .defineInRange("follow_range", 64.0, 1.0, 256.0);

        DOPPELGANGER_HARD_MODE = BUILDER
                .comment("Hard mode of the Dark Doppelganger")
                .define("hard_mode", false);

        DOPPELGANGER_COPY_PLAYER_ARMOR = BUILDER
                .comment("If true, the boss will copy the player's armor unless the armor is banned or missing. If false, the boss will always use the configured default armor set.")
                .define("copy_player_armor", true);

        DOPPELGANGER_COPY_PLAYER_MAINHAND = BUILDER
                .comment("If true, the boss will copy the player's main hand item. If false, it will always use the configured default main hand item.")
                .define("copy_player_mainhand", true);

        DOPPEL_PHASE1_SPELLS_A = BUILDER.defineListAllowEmpty("phase1.spells_a",
                List.of(
                        "irons_spellbooks:guiding_bolt",
                        "irons_spellbooks:blood_needles",
                        "irons_spellbooks:blood_slash"
                ),
                o -> o instanceof String);

        DOPPEL_PHASE1_SPELLS_B = BUILDER.defineListAllowEmpty("phase1.spells_b",
                List.of(
                        "irons_spellbooks:fang_ward",
                        "irons_spellbooks:gust"
                ),
                o -> o instanceof String);

        DOPPEL_PHASE1_SPELLS_C = BUILDER.defineListAllowEmpty("phase1.spells_c",
                List.of("irons_spellbooks:burning_dash"),
                o -> o instanceof String);

        DOPPEL_PHASE1_SPELLS_D = BUILDER.defineListAllowEmpty("phase1.spells_d",
                List.of(
                        "irons_spellbooks:blight",
                        "irons_spellbooks:invisibility"
                ),
                o -> o instanceof String);

        DOPPEL_PHASE2_SPELLS_A = BUILDER.defineListAllowEmpty("phase2.spells_a",
                List.of(
                        "irons_spellbooks:magic_arrow",
                        "irons_spellbooks:poison_arrow",
                        "irons_spellbooks:magma_bomb"
                ),
                o -> o instanceof String);

        DOPPEL_PHASE2_SPELLS_B = BUILDER.defineListAllowEmpty("phase2.spells_b",
                List.of(
                        "irons_spellbooks:heat_surge",
                        "irons_spellbooks:flaming_strike"
                ),
                o -> o instanceof String);

        DOPPEL_PHASE2_SPELLS_C = BUILDER.defineListAllowEmpty("phase2.spells_c",
                List.of("irons_spellbooks:frost_step"),
                o -> o instanceof String);

        DOPPEL_PHASE2_SPELLS_D = BUILDER.defineListAllowEmpty("phase2.spells_d",
                List.of(
                        "irons_spellbooks:root",
                        "irons_spellbooks:thunderstorm"
                ),
                o -> o instanceof String);
        DOPPEL_PHASE3_SPELLS_A = BUILDER.defineListAllowEmpty("phase3.spells_a",
                List.of(
                        "irons_spellbooks:lightning_lance",
                        "irons_spellbooks:stomp"
                ),
                o -> o instanceof String);

        DOPPEL_PHASE3_SPELLS_B = BUILDER.defineListAllowEmpty("phase3.spells_b",
                List.of(
                        "irons_spellbooks:shockwave",
                        "irons_spellbooks:ascension"
                ),
                o -> o instanceof String);

        DOPPEL_PHASE3_SPELLS_C = BUILDER.defineListAllowEmpty("phase3.spells_c",
                List.of("irons_spellbooks:blood_step"),
                o -> o instanceof String);

        DOPPEL_PHASE3_SPELLS_D = BUILDER.defineListAllowEmpty("phase3.spells_d",
                List.of(
                        "irons_spellbooks:evasion",
                        "irons_spellbooks:echoing_strikes"
                ),
                o -> o instanceof String);

        DOPPEL_FINAL_SPELLS_A = BUILDER.defineListAllowEmpty("final.spells_a",
                List.of(
                        "irons_spellbooks:eldritch_blast",
                        "irons_spellbooks:sonic_boom",
                        "irons_spellbooks:abyssal_shroud",
                        "irons_spellbooks:ray_of_frost",
                        "irons_spellbooks:sculk_tentacles"
                ),
                o -> o instanceof String);

        DOPPEL_FINAL_SPELLS_B = BUILDER.defineListAllowEmpty("final.spells_b",
                List.of(
                        "irons_spellbooks:ascension",
                        "irons_spellbooks:abyssal_shroud"
                ),
                o -> o instanceof String);

        DOPPEL_FINAL_SPELLS_C = BUILDER.defineListAllowEmpty("final.spells_c",
                List.of("irons_spellbooks:blood_step"),
                o -> o instanceof String);

        DOPPEL_FINAL_SPELLS_D = BUILDER.defineListAllowEmpty("final.spells_d",
                List.of(
                        "irons_spellbooks:abyssal_shroud",
                        "irons_spellbooks:echoing_strikes",
                        "irons_spellbooks:root",
                        "irons_spellbooks:blight"
                ),
                o -> o instanceof String);

        DOPPELGANGER_DEFAULT_MAINHAND = BUILDER
                .comment("Default weapon item ID used when the boss should not copy the player's main hand item.")
                .define("default_mainhand", "minecraft:netherite_sword");

        DOPPELGANGER_DEFAULT_HELMET = BUILDER
                .comment("Default helmet item ID used when the boss should not copy the player's helmet.")
                .define("default_helmet", "irons_spellbooks:netherite_mage_helmet");

        DOPPELGANGER_DEFAULT_CHESTPLATE = BUILDER
                .comment("Default chestplate item ID used when the boss should not copy the player's chestplate.")
                .define("default_chestplate", "irons_spellbooks:netherite_mage_chestplate");

        DOPPELGANGER_DEFAULT_LEGGINGS = BUILDER
                .comment("Default leggings item ID used when the boss should not copy the player's leggings.")
                .define("default_leggings", "irons_spellbooks:netherite_mage_leggings");

        DOPPELGANGER_DEFAULT_BOOTS = BUILDER
                .comment("Default boots item ID used when the boss should not copy the player's boots.")
                .define("default_boots", "irons_spellbooks:netherite_mage_boots");


        DOPPELGANGER_BANNED_ARMOR = BUILDER
                .comment("Armor item IDs the boss is NOT allowed to copy from the player. Supports wildcard with * suffix.",
                        "Examples:",
                        "minecraft:netherite_helmet",
                        "irons_spellbooks:*")
                .defineListAllowEmpty(
                        "banned_armor",
                        List.of(
                                "minecraft:netherite_helmet" // example default
                        ),
                        o -> o instanceof String
                );
        DOPPELGANGER_SPELL_POWER_MULTIPLIER = BUILDER
                .comment("Scales all Iron's Spellbooks spell power attributes copied from the summoner. 1.0 = no change.")
                .defineInRange("doppelgangerSpellPowerMultiplier", 1.0D, 0.0D, 100.0D);


        BUILDER.pop();

        BUILDER.comment("Summoning Configuration").push("summoning");

        ALTAR_END_ONLY = BUILDER
                .comment("When true, the Shadow Altar can only summon the Dark Doppelganger in The End")
                .define("altar_end_only", true);

        BUILDER.pop();

        BUILDER.comment("Minion Configuration").push("minion");

        MINION_HEALTH = BUILDER
                .comment("Base max health for minions")
                .defineInRange("health", 40.0, 1.0, 1000.0);

        MINION_ATTACK_DAMAGE = BUILDER
                .comment("Base attack damage for minions")
                .defineInRange("attack_damage", 6.0, 0.1, 100.0);

        MINION_MOVEMENT_SPEED = BUILDER
                .comment("Base movement speed for minions")
                .defineInRange("movement_speed", 0.28, 0.01, 2.0);

        MINION_ARMOR = BUILDER
                .comment("Base armor for minions")
                .defineInRange("armor", 4.0, 0.0, 100.0);

        MINION_BARRAGE_SPELL = BUILDER
                .comment(
                        "Spell used for the minion barrage goal. Must be a valid spell id.",
                        "Example: irons_spellbooks:devour"
                )
                .define("barrage_spell", "irons_spellbooks:devour");

        MINION_SPELLS = BUILDER
                .comment(
                        "List of spells minions can randomly use in their attack goal.",
                        "Invalid ids are ignored."
                )
                .defineListAllowEmpty(
                        "spells",
                        List.of(
                                "irons_spellbooks:guiding_bolt",
                                "irons_spellbooks:blood_needles",
                                "irons_spellbooks:blood_slash",
                                "irons_spellbooks:fang_ward",
                                "irons_spellbooks:gust",
                                "irons_spellbooks:burning_dash",
                                "irons_spellbooks:blight",
                                "irons_spellbooks:invisibility"
                        ),
                        o -> o instanceof String s && s.contains(":")
                );

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
        }
    }
}
