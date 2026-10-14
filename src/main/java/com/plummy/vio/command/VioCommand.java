package com.plummy.vio.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plummy.vio.Vio;
import com.plummy.vio.agent.Agent;
import com.plummy.vio.agent.SubmitResult;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class VioCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vio")
                .then(Commands.literal("reset")
                        .executes(VioCommand::reset))
                .then(Commands.argument("request", StringArgumentType.greedyString())
                        .executes(VioCommand::submit)));
    }

    private static int submit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String request = StringArgumentType.getString(context, "request");
        Agent agent = Vio.getAgent();

        if (agent == null) {
            context.getSource().sendFailure(Component.literal("Vio is not running."));
            return 0;
        }

        if (agent.submit(player, request) == SubmitResult.BUSY) {
            context.getSource().sendFailure(Component.literal("Vio is still working on your previous request."));
            return 0;
        }

        // Commands are not echoed to chat, so show the player what they asked.
        context.getSource().sendSystemMessage(Component.literal("> " + request).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Agent agent = Vio.getAgent();

        if (agent == null) {
            context.getSource().sendFailure(Component.literal("Vio is not running."));
            return 0;
        }

        if (!agent.reset(player.getUUID())) {
            context.getSource().sendFailure(Component.literal("Vio is still working on your previous request."));
            return 0;
        }

        context.getSource().sendSystemMessage(Component.literal("Conversation reset.").withStyle(ChatFormatting.GRAY));
        return 1;
    }
}
