package com.plummy.vio.user;

import com.google.gson.JsonObject;
import com.plummy.vio.access.AccessLevel;

import java.util.UUID;

public class UserJsonDeserializer {

    private static final UserJsonDeserializer INSTANCE = new UserJsonDeserializer();

    public static UserJsonDeserializer getInstance() {
        return INSTANCE;
    }

    private UserJsonDeserializer() {
    }

    public PlayerUser deserializePlayerUser(UUID id, JsonObject json) {
        return new PlayerUser(id, json.get("name").getAsString(), AccessLevel.fromValue(json.get("access_level").getAsString()));
    }
}
