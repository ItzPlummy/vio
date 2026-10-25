package com.plummy.vio.core.conversation;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import com.plummy.vio.core.storage.JsonDirectoryStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public class ConversationStorage extends JsonDirectoryStorage<String, Conversation> {

    public ConversationStorage(Path directory) {
        super(directory);
    }

    public static ConversationStorage fromServer(MinecraftServer server) {
        return new ConversationStorage(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("conversations"));
    }

    @Override
    protected JsonObject serialize(Conversation value) {
        return value.accept(ConversationJsonSerializer.getInstance());
    }

    @Override
    protected Conversation deserialize(JsonObject json) {
        return ConversationJsonDeserializer.getInstance().deserialize(json);
    }

    @Override
    protected String getFileName(String key) {
        return key + ".json";
    }
}
