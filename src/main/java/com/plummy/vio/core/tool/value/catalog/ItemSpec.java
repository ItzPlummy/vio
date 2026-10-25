package com.plummy.vio.core.tool.value.catalog;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.plummy.vio.core.tool.value.argument.Argument;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class ItemSpec implements Argument {

    private final String text;
    private ItemParser.ItemResult item;

    public ItemSpec(String text) {
        this.text = text;
    }

    public ItemStack toItemStack(ServerLevel level) {
        ItemParser.ItemResult item = getItem(level);

        return new ItemStack(item.item(), 1, item.components());
    }

    public String getText() {
        return text;
    }

    private ItemParser.ItemResult getItem(ServerLevel level) {
        if (item == null) {
            try {
                item = new ItemParser(level.registryAccess()).parse(new StringReader(text));
            } catch (CommandSyntaxException e) {
                throw new Argument.ParseException("'%s' isn't a valid item: %s".formatted(text, e.getMessage()));
            }
        }

        return item;
    }
}
