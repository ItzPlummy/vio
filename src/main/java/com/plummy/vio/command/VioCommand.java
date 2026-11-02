package com.plummy.vio.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plummy.vio.Vio;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.access.AccessManager;
import com.plummy.vio.user.PlayerUser;
import com.plummy.vio.tool.catalog.server.RunCommandTool;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;

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
                .then(Commands.literal("access")
                        // Never through run_command: it runs at level 2 for any FULL player, which would let them hand out access.
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS) && !RunCommandTool.isToolSource(source))
                        .then(player)));
    }

    private static int queryAccess(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
        AccessManager accessManager = Vio.getAccessManager();

        if (accessManager == null) {
            context.getSource().sendFailure(Component.literal("Vio isn't running."));
            return 0;
        }

        for (GameProfile profile : profiles) {
            PlayerUser playerUser = accessManager.getPlayerUser(profile.getId());
            String text = playerUser == null
                    ? "%s has access level %s (server default).".formatted(profile.getName(), accessManager.getDefaultAccessLevel())
                    : "%s has access level %s.".formatted(profile.getName(), playerUser.getAccessLevel());

            context.getSource().sendSystemMessage(Component.literal(text));
        }

        return profiles.size();
    }

    private static int setAccess(CommandContext<CommandSourceStack> context, AccessLevel level) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
        AccessManager accessManager = Vio.getAccessManager();

        if (accessManager == null) {
            context.getSource().sendFailure(Component.literal("Vio isn't running."));
            return 0;
        }

        for (GameProfile profile : profiles) {
            accessManager.setPlayerAccessLevel(profile.getId(), level);
            context.getSource().sendSuccess(() -> Component.literal("Set %s's Vio access level to %s.".formatted(profile.getName(), level)), true);
        }

        return profiles.size();
    }

    private static int resetAccess(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
        AccessManager accessManager = Vio.getAccessManager();

        if (accessManager == null) {
            context.getSource().sendFailure(Component.literal("Vio isn't running."));
            return 0;
        }

        for (GameProfile profile : profiles) {
            accessManager.resetPlayerAccessLevel(profile.getId());
            context.getSource().sendSuccess(() -> Component.literal("Reset %s's Vio access level to the server default (%s).".formatted(profile.getName(), accessManager.getDefaultAccessLevel())), true);
        }

        return profiles.size();
    }
}
