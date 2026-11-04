package com.plummy.vio.core.provider;

import com.plummy.vio.core.conversation.Conversation;
import com.plummy.vio.core.user.User;

public interface Provider {

    <T extends User> void request(T user, Conversation<T> conversation);
}
