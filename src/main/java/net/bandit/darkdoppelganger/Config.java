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
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MINION_SPELLS;
    public static final ModConfigSpec.ConfigValue<String> MINION_BARRAGE_SPELL;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DOPPELGANGER_BANNED_ARMOR;


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
