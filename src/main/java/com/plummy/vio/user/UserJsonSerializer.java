package com.plummy.vio.user;

import com.google.gson.JsonObject;

public class UserJsonSerializer implements UserVisitor<JsonObject> {

    private static final UserJsonSerializer INSTANCE = new UserJsonSerializer();

    public static UserJsonSerializer getInstance() {
        return INSTANCE;
    }

    private UserJsonSerializer() {
    }

    @Override
    public JsonObject visitPlayerUser(PlayerUser playerUser) {
        JsonObject json = new JsonObject();

        json.addProperty("name", playerUser.getName());
        json.addProperty("access_level", playerUser.getAccessLevel().getValue());

        return json;
    }
}
