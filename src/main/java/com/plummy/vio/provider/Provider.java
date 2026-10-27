package com.plummy.vio.provider;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.message.Message;

public interface Provider {

    Message request(AccessLevel accessLevel, String instructions, Conversation conversation);
}
