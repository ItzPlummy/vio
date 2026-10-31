package com.plummy.vio.user;

import com.google.gson.JsonObject;
import com.plummy.vio.storage.JsonFileStorage;

public abstract class UserStorage<K, V extends User<K>> extends JsonFileStorage<K, V> {

    @Override
    protected JsonObject serializeItemToJson(V item) {
        return item.accept(UserJsonSerializer.getInstance());
    }
}
