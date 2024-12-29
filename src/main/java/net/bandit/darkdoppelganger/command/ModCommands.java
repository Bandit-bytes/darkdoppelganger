package net.bandit.darkdoppelganger.command;

import com.mojang.brigadier.CommandDispatcher;
import net.bandit.darkdoppelganger.event.SummonDoppelganger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ModCommands {
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("darkd")
                .then(Commands.literal("summon_doppelganger")
                        .executes(context -> SummonDoppelganger.summonDoppelganger(context.getSource()))));
    }
}
