package com.plummy.vio.script;

import java.time.Instant;
import java.util.UUID;

public interface Toggleable extends Script {

    boolean isEnabled();

    void setEnabled(boolean enabled, UUID toggledBy, Instant toggledAt);

    UUID getToggledBy();

    Instant getToggledAt();
}
