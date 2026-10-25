package com.plummy.vio.provider;

import com.plummy.vio.core.access.AccessLevel;
import com.plummy.vio.core.conversation.Conversation;
import com.plummy.vio.core.message.Message;

public interface Provider {

    Message request(AccessLevel accessLevel, String instructions, Conversation conversation);
}
