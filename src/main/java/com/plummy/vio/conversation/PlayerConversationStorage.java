package com.plummy.vio.conversation;

import com.google.gson.JsonObject;
import com.plummy.vio.Vio;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.UUID;

public class PlayerConversationStorage extends ConversationStorage<UUID, PlayerConversation> {

    private final Path path;

    public PlayerConversationStorage(Path path) {
        this.path = path;
    }

    public static PlayerConversationStorage fromServer(MinecraftServer server) {
        return new PlayerConversationStorage(server.getWorldPath(new LevelResource("serverconfig")).resolve(Vio.MODID).resolve("conversations").resolve("players"));
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    protected String serializeKey(UUID key) {
        return key.toString();
    }

    @Override
    protected UUID deserializeKey(String value) {
        return UUID.fromString(value);
    }

    @Override
    protected PlayerConversation deserializeItemFromJson(UUID key, JsonObject json) {
        return ConversationJsonDeserializer.getInstance().deserializePlayerConversation(key, json);
    }
}
