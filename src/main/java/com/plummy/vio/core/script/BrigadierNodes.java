package com.plummy.vio.core.script;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.plummy.vio.Vio;

import java.lang.reflect.Field;
import java.util.Map;

// Brigadier has no way to remove a command, so this edits the maps of the root node directly.
public final class BrigadierNodes {

    private static final Field CHILDREN = findField("children");
    private static final Field LITERALS = findField("literals");

    private BrigadierNodes() {
    }

    // Removes this exact node only, never another command with the same name. False when it's still there.
    public static <S> boolean remove(RootCommandNode<S> root, CommandNode<S> node) {
        if (root.getChild(node.getName()) != node) {
            return true;
        }

        if (CHILDREN == null || LITERALS == null) {
            return false;
        }

        try {
            ((Map<?, ?>) CHILDREN.get(root)).remove(node.getName());
            ((Map<?, ?>) LITERALS.get(root)).remove(node.getName());
            return true;
        } catch (IllegalAccessException e) {
            Vio.getLogger().warn("Failed to remove command /{}", node.getName(), e);
            return false;
        }
    }

    private static Field findField(String name) {
        try {
            Field field = CommandNode.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            Vio.getLogger().warn("Brigadier's CommandNode.{} isn't accessible, so script commands are only hidden until restart instead of removed", name, e);
            return null;
        }
    }
}
