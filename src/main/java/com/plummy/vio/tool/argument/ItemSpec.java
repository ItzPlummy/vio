package com.plummy.vio.tool.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

// An item written like /give: minecraft:diamond_sword[enchantments={levels:{"minecraft:sharpness":5}}].
// Parsing needs the registries, so the text is parsed on first use.
public class ItemSpec implements Argument {

    private final String text;
    private ItemParser.ItemResult item;

    public ItemSpec(String text) {
        this.text = text;
    }

    // A new stack of one each call, with the spec's data components.
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
                throw new Argument.ParseException("\"%s\" is not a valid item: %s".formatted(text, e.getMessage()));
            }
        }

        return item;
    }
}
