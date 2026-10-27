package com.plummy.vio.tool.catalog.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.ParseResults;
import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.tool.ServerTool;
import com.plummy.vio.tool.executor.ExecutionResult;
import com.plummy.vio.tool.value.argument.ArgumentDefinition;
import com.plummy.vio.tool.value.argument.ArgumentType;
import com.plummy.vio.tool.value.argument.Arguments;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public class RunCommandTool implements ServerTool {

    private static final int MAX_OUTPUT_LINES = 50;

    @Override
    public String getName() {
        return "run_command";
    }

    @Override
    public String getDescription() {
        return "Run any command a permission-level-2 operator could run, as if the requesting player typed it at their position: ~ and @s refer to them. Level 3-4 commands (op, ban, kick, whitelist, stop, save-*, ...) are unavailable. Commands run in one tick and stay capped by vanilla limits, e.g. /fill's block limit; use the fill_* and other editing tools for large edits.";
    }

    @Override
    public SequencedMap<String, ArgumentDefinition> getArgumentSchema() {
        SequencedMap<String, ArgumentDefinition> schema = new LinkedHashMap<>();

        schema.put("status", ArgumentDefinition.status());
        schema.put("command", new ArgumentDefinition(ArgumentType.STRING, "Command text; a leading / is optional."));

        return schema;
    }

    @Override
    public AccessLevel getAccessLevel() {
        return AccessLevel.FULL;
    }

    public static boolean isToolSource(CommandSourceStack source) {
        return source.source instanceof OutputCollector;
    }

    @Override
    public ExecutionResult execute(Context context, Arguments arguments) {
        String command = arguments.getString("command").strip();

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        if (command.isEmpty()) {
            return ExecutionResult.invalidArguments("command is empty");
        }

        ServerPlayer player = context.getPlayer();
        OutputCollector output = new OutputCollector();
        boolean[] success = {false};
        int[] result = {0};

        CommandSourceStack source = player.createCommandSourceStack()
                .withSource(output)
                .withPermission(Commands.LEVEL_GAMEMASTERS)
                .withCallback((commandSuccess, commandResult) -> {
                    success[0] = commandSuccess;
                    result[0] = commandResult;
                });

        Commands commands = context.getServer().getCommands();
        ParseResults<CommandSourceStack> parseResults = commands.getDispatcher().parse(command, source);

        commands.performCommand(parseResults, command);

        List<String> errors = new ArrayList<>(output.getErrors());

        if (errors.stream().anyMatch(error -> error.contains("Unknown or incomplete command"))) {
            errors.add("Note: run_command is limited to permission level 2, so level 3-4 commands are unknown here.");
        }

        List<String> lines = output.getOutput();
        JsonObject json = new JsonObject();

        json.addProperty("success", success[0]);
        json.addProperty("result", result[0]);
        json.add("output", lines.subList(0, Math.min(lines.size(), MAX_OUTPUT_LINES)).stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        json.add("errors", errors.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));
        json.addProperty("truncated", lines.size() > MAX_OUTPUT_LINES);

        return ExecutionResult.success(json);
    }

    private static class OutputCollector implements CommandSource {

        private final List<String> output = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        @Override
        public void sendSystemMessage(Component component) {
            String text = component.getString();

            if (isFailure(component)) {
                errors.add(text);
            } else {
                output.add(text);
            }
        }

        private static boolean isFailure(Component component) {
            return component.getStyle().getColor() != null && "red".equals(component.getStyle().getColor().serialize());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }

        public List<String> getOutput() {
            return output;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
