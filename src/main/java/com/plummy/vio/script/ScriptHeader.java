package com.plummy.vio.script;

import java.time.Instant;
import java.util.UUID;

// A script's metadata, saved as the comment header at the top of its file.
public class ScriptHeader {

    public static final int MIN_INTERVAL_TICKS = 1;
    public static final int MAX_INTERVAL_TICKS = 72000;
    public static final int MAX_PERMISSION_LEVEL = 4;

    private final ScriptType type;
    private final String description;
    // Only periodic scripts have an interval, only command scripts a permission level; null otherwise.
    private final Integer intervalTicks;
    private final Integer permissionLevel;
    // Always true for one-shot scripts, which can't be toggled.
    private final boolean enabled;
    private final UUID creator;
    private final Instant createdAt;
    // Null until the script is toggled for the first time.
    private final UUID toggledBy;
    private final Instant toggledAt;

    public ScriptHeader(ScriptType type, String description, Integer intervalTicks, Integer permissionLevel, boolean enabled, UUID creator, Instant createdAt, UUID toggledBy, Instant toggledAt) {
        this.type = type;
        this.description = description;
        this.intervalTicks = intervalTicks;
        this.permissionLevel = permissionLevel;
        this.enabled = enabled;
        this.creator = creator;
        this.createdAt = createdAt;
        this.toggledBy = toggledBy;
        this.toggledAt = toggledAt;
    }

    public ScriptHeader withEnabled(boolean enabled, UUID toggledBy, Instant toggledAt) {
        return new ScriptHeader(type, description, intervalTicks, permissionLevel, enabled, creator, createdAt, toggledBy, toggledAt);
    }

    // A replaced script keeps whether it was enabled and who toggled it last, if both versions can be toggled.
    public ScriptHeader withToggleStateOf(ScriptHeader previous) {
        if (!type.isToggleable() || !previous.getType().isToggleable()) {
            return this;
        }

        return withEnabled(previous.isEnabled(), previous.getToggledBy(), previous.getToggledAt());
    }

    public ScriptType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Integer getIntervalTicks() {
        return intervalTicks;
    }

    public Integer getPermissionLevel() {
        return permissionLevel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public UUID getCreator() {
        return creator;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getToggledBy() {
        return toggledBy;
    }

    public Instant getToggledAt() {
        return toggledAt;
    }
}
