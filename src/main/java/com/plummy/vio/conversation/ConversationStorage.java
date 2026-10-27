package com.plummy.vio.conversation;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import com.plummy.vio.storage.JsonDirectoryStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public class ConversationStorage extends JsonDirectoryStorage<String, Conversation> {

    private final Path directory;

    public ConversationStorage(Path directory) {
        this.directory = directory;
    }

    public static ConversationStorage fromServer(MinecraftServer server) {
        return new ConversationStorage(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("conversations"));
    }

    @Override
    public Path getPath() {
        return directory;
    }

    @Override
    protected String serializeKey(String key) {
        return key;
    }

    @Override
    protected String deserializeKey(String value) {
        return value;
    }

    @Override
    protected JsonObject serializeItemToJson(Conversation item) {
        return item.accept(ConversationJsonSerializer.getInstance());
    }

    @Override
    protected Conversation deserializeItemFromJson(String key, JsonObject json) {
        return ConversationJsonDeserializer.getInstance().deserialize(json);
    }
}
