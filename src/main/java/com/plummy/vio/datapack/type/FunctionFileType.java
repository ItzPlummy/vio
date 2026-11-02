package com.plummy.vio.datapack.type;

import com.plummy.vio.datapack.DatapackValidation;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class FunctionFileType implements DatapackFileType {

    @Override
    public String getFolder() {
        return "function";
    }

    @Override
    public String getExtension() {
        return "mcfunction";
    }

    @Override
    public boolean isReloadable() {
        return true;
    }

    @Override
    public DatapackValidation validate(MinecraftServer server, ResourceLocation id, String content) {
        CommandSourceStack source = new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, server.getFunctionCompilationLevel(), "", CommonComponents.EMPTY, null, null);

        try {
            CommandFunction.fromLines(id, server.getFunctions().getDispatcher(), source, content.lines().toList());
        } catch (IllegalArgumentException e) {
            return DatapackValidation.error(e.getMessage());
        }

        return DatapackValidation.warnings(List.of());
    }
}
