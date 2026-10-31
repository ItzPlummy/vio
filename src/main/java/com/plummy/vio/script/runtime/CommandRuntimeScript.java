package com.plummy.vio.script.runtime;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public abstract class CommandRuntimeScript extends RuntimeScript {

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
