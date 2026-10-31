package com.plummy.vio.access;

import com.plummy.vio.user.PlayerUser;

import java.util.UUID;

// Each kind of user gets its own methods here, so console or HTTP users can be added next to players
public interface AccessManager {

    AccessLevel getDefaultAccessLevel();

    // The player's stored level, or the default one when they have none
    AccessLevel getPlayerAccessLevel(UUID playerId);

    // Null when the player has no stored level and uses the default one
    PlayerUser getPlayerUser(UUID playerId);

    void setPlayerAccessLevel(UUID playerId, AccessLevel accessLevel);

    void resetPlayerAccessLevel(UUID playerId);
}
