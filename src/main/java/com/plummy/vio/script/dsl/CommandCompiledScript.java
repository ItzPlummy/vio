package com.plummy.vio.script.dsl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public abstract class CommandCompiledScript extends CompiledScript {

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
