package com.plummy.vio.core.storage;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import com.plummy.vio.core.conversation.Conversation;
import com.plummy.vio.core.conversation.ConversationJsonDeserializer;
import com.plummy.vio.core.conversation.ConversationJsonSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.UUID;

public class ConversationStorage extends JsonDirectoryStorage<UUID, Conversation> {

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
    protected String getFileName(UUID key) {
        return key + ".json";
    }
}
