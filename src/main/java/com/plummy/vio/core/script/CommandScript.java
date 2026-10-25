package com.plummy.vio.core.script;

import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.atomic.AtomicBoolean;

public class CommandScript extends AbstractToggleableScript {

    // Null while the script isn't active.
    private Registration registration;

    public CommandScript(ScriptFile file, CompiledScript compiled, String error) {
        super(file, compiled, error);
    }

    public int getPermissionLevel() {
        return getHeader().getPermissionLevel();
    }

    Registration getRegistration() {
        return registration;
    }

    void setRegistration(Registration registration) {
        this.registration = registration;
    }

    // The node Vio built and added to the dispatcher. Its requirement checks active, which hides the command even if
    // the node can't be removed.
    record Registration(LiteralCommandNode<CommandSourceStack> node, AtomicBoolean active) {
    }
}
