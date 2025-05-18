package net.bandit.darkdoppelganger;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

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

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment("Dark Doppelganger Configuration").push("dark_doppelganger");

        DOPPELGANGER_HEALTH = BUILDER
                .comment("Health of the Dark Doppelganger")
                .defineInRange("health", 600.0, 1.0, 100000.0);

        DOPPELGANGER_ATTACK_DAMAGE = BUILDER
                .comment("Attack Damage of the Dark Doppelganger")
                .defineInRange("attack_damage", 10.0, 1.0, 100.0);

        DOPPELGANGER_MOVEMENT_SPEED = BUILDER
                .comment("Movement Speed of the Dark Doppelganger")
                .defineInRange("movement_speed", 0.20, 0.1, 1.0);

        DOPPELGANGER_KNOCKBACK_RESISTANCE = BUILDER
                .comment("Knockback Resistance of the Dark Doppelganger")
                .defineInRange("knockback_resistance", 0.6, 0.0, 1.0);

        DOPPELGANGER_ARMOR = BUILDER
                .comment("Armor of the Dark Doppelganger")
                .defineInRange("armor", 10.0, 0.0, 100.0);

        DOPPELGANGER_FOLLOW_RANGE = BUILDER
                .comment("Follow Range of the Dark Doppelganger")
                .defineInRange("follow_range", 64.0, 1.0, 128.0);

        DOPPELGANGER_HARD_MODE = BUILDER
                .comment("Hard mode of the Dark Doppelganger")
                .define("hard_mode", false);

        BUILDER.pop();
    }
    static {
        BUILDER.comment("Minion Configuration").push("minion");

        MINION_HEALTH = BUILDER.defineInRange("health", 40.0, 1.0, 1000.0);
        MINION_ATTACK_DAMAGE = BUILDER.defineInRange("attack_damage", 6.0, 0.1, 100.0);
        MINION_MOVEMENT_SPEED = BUILDER.defineInRange("movement_speed", 0.28, 0.01, 1.0);
        MINION_ARMOR = BUILDER.defineInRange("armor", 4.0, 0.0, 100.0);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
        }
    }
}