package com.plummy.vio.core.script;

import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;

import java.time.Instant;
import java.util.UUID;

public class CommandScript extends AbstractToggleableScript {

    public static final int MAX_PERMISSION_LEVEL = 4;

    private final int permissionLevel;
    private LiteralCommandNode<CommandSourceStack> node;

    public CommandScript(String name, String description, UUID creator, Instant createdAt, int permissionLevel, boolean enabled, UUID toggledBy, Instant toggledAt, String source) {
        super(name, description, creator, createdAt, enabled, toggledBy, toggledAt, source);
        this.permissionLevel = permissionLevel;
    }

    @Override
    public ScriptType getType() {
        return ScriptType.COMMAND;
    }

    @Override
    public <T> T accept(ScriptVisitor<T> visitor) {
        return visitor.visitCommandScript(this);
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    LiteralCommandNode<CommandSourceStack> getNode() {
        return node;
    }

    void setNode(LiteralCommandNode<CommandSourceStack> node) {
        this.node = node;
    }
}
