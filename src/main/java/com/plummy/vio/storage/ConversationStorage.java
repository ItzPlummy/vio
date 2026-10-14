package com.plummy.vio.storage;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.registry.DeserializerRegistry;
import com.plummy.vio.registry.VisitorRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.UUID;

public class ConversationStorage extends JsonStorage<UUID, Conversation> {

    public ConversationStorage(Path directory) {
        super(directory);
    }

    public static ConversationStorage fromServer(MinecraftServer server) {
        return new ConversationStorage(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("conversations"));
    }

    @Override
    protected JsonObject serialize(Conversation value) {
        return value.accept(VisitorRegistry.CONVERSATION_JSON_SERIALIZER);
    }

    @Override
    protected Conversation deserialize(JsonObject json) {
        return DeserializerRegistry.CONVERSATION_JSON_DESERIALIZER.deserialize(json);
    }

    @Override
    protected String getFileName(UUID key) {
        return key + ".json";
    }
}
