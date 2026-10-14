package com.plummy.vio.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plummy.vio.Vio;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.access.AccessManager;
import com.plummy.vio.access.PlayerAccess;
import com.plummy.vio.agent.Agent;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class VioCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        RequiredArgumentBuilder<CommandSourceStack, GameProfileArgument.Result> player = Commands.argument("player", GameProfileArgument.gameProfile())
                .executes(VioCommand::queryAccess);

        for (AccessLevel level : AccessLevel.values()) {
            player.then(Commands.literal(level.getValue())
                    .executes(context -> setAccess(context, level)));
        }

        player.then(Commands.literal("reset")
                .executes(VioCommand::resetAccess));

        dispatcher.register(Commands.literal("vio")
                .then(Commands.literal("reset")
                        .executes(VioCommand::reset))
                .then(Commands.literal("access")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(player))
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

        switch (agent.submit(player, request)) {
            case BUSY -> {
                context.getSource().sendFailure(Component.literal("Vio is still working on your previous request."));
                return 0;
            }
            case DENIED -> {
                context.getSource().sendFailure(Component.literal("You don't have access to Vio."));
                return 0;
            }
            case ACCEPTED -> {
                // Commands are not echoed to chat, so show the player what they asked.
                context.getSource().sendSystemMessage(Component.literal("> " + request).withStyle(ChatFormatting.GRAY));
                return 1;
            }
        }

        return 0;
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

    private static int queryAccess(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
        AccessManager accessManager = Vio.getAccessManager();

        if (accessManager == null) {
            context.getSource().sendFailure(Component.literal("Vio is not running."));
            return 0;
        }

        for (GameProfile profile : profiles) {
            PlayerAccess playerAccess = accessManager.getPlayerAccess(profile.getId());
            String text = playerAccess == null
                    ? profile.getName() + " has access level " + accessManager.getServerAccessLevel() + " (server default)."
                    : profile.getName() + " has access level " + playerAccess.getLevel() + ".";

            context.getSource().sendSystemMessage(Component.literal(text));
        }

        return profiles.size();
    }

    private static int setAccess(CommandContext<CommandSourceStack> context, AccessLevel level) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
        AccessManager accessManager = Vio.getAccessManager();

        if (accessManager == null) {
            context.getSource().sendFailure(Component.literal("Vio is not running."));
            return 0;
        }

        for (GameProfile profile : profiles) {
            accessManager.setPlayerAccessLevel(profile.getId(), profile.getName(), level);
            context.getSource().sendSuccess(() -> Component.literal("Set " + profile.getName() + "'s Vio access level to " + level + "."), true);
        }

        return profiles.size();
    }

    private static int resetAccess(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
        AccessManager accessManager = Vio.getAccessManager();

        if (accessManager == null) {
            context.getSource().sendFailure(Component.literal("Vio is not running."));
            return 0;
        }

        for (GameProfile profile : profiles) {
            accessManager.resetPlayerAccessLevel(profile.getId());
            context.getSource().sendSuccess(() -> Component.literal("Reset " + profile.getName() + "'s Vio access level to the server default (" + accessManager.getServerAccessLevel() + ")."), true);
        }

        return profiles.size();
    }
}
