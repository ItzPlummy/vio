package com.plummy.vio.tool.argument;

import com.google.gson.JsonObject;

import java.util.List;

// limit/offset pagination, read with Arguments.getPage. Callers sort with a stable order (ties broken by id, UUID or coordinates), so pages never overlap.
public class Page {

    private final int limit;
    private final int offset;

    public Page(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public <T> List<T> apply(List<T> items) {
        if (getOffset() >= items.size()) {
            return List.of();
        }

        return items.subList(getOffset(), Math.min(items.size(), getLimit() + getOffset()));
    }

    public void write(JsonObject json, int total) {
        json.addProperty("total", total);
        json.addProperty("offset", getOffset());

        if (getOffset() + getLimit() < total) {
            json.addProperty("next_offset", getLimit() + getOffset());
        } else {
            json.add("next_offset", null);
        }
    }
}
