package com.plummy.vio.script.dsl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

// Base class of command scripts. The body only declares one command with command(...); Vio registers it while the
// script is enabled, adding the script's permission level and wrapping every executes.
public abstract class CommandVioScript extends VioScript {

    private LiteralArgumentBuilder<CommandSourceStack> command;

    public void command(LiteralArgumentBuilder<CommandSourceStack> command) {
        if (this.command != null) {
            throw new IllegalStateException("A command script declares exactly one command");
        }

        this.command = command;
    }

    public LiteralArgumentBuilder<CommandSourceStack> getCommand() {
        return command;
    }
}
