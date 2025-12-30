package net.bandit.darkdoppelganger.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.bandit.darkdoppelganger.entity.DarkDoppelgangerEntity;
import net.bandit.darkdoppelganger.event.SummonDoppelganger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public class ModCommands {

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("darkd")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("summon_doppelganger")
                        .executes(ctx -> SummonDoppelganger.summonDoppelganger(ctx.getSource())))
                .then(Commands.literal("kill_doppelganger")
                        .executes(ctx -> killDoppelgangers(ctx.getSource(), 256))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 4096))
                                .executes(ctx -> killDoppelgangers(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "radius")
                                ))
                        )
                )
        );
    }

    private static int killDoppelgangers(CommandSourceStack source, int radius) {
        ServerLevel level = source.getLevel();
        var pos = source.getPosition();

        var box = new net.minecraft.world.phys.AABB(
                pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius
        );

        List<DarkDoppelgangerEntity> found = level.getEntitiesOfClass(DarkDoppelgangerEntity.class, box);

        int bosses = 0;
        int clones = 0;

        for (DarkDoppelgangerEntity d : found) {
            boolean isBoss = d.getTags().contains("dark_doppelganger_boss");
            boolean isClone = d.getTags().contains("dark_doppelganger_clone") || d.isClone;

            if (!isBoss && !isClone) continue;
            d.kill();

            if (isBoss) bosses++;
            else clones++;
        }

        int total = bosses + clones;

        if (total == 0) {
            source.sendFailure(Component.literal("No Dark Doppelganger entities found in radius " + radius + "."));
        } else {
            int fb = bosses, fc = clones;
            source.sendSuccess(() ->
                    Component.literal("Removed " + total + " Dark Doppelganger entity(ies) in radius " + radius +
                            " (bosses: " + fb + ", clones/minions: " + fc + ")."), true);
        }

        return total;
    }
}
