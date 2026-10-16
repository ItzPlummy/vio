package com.plummy.vio.provider;

import com.plummy.vio.access.AccessLevel;
import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.message.ConversationMessage;
import com.plummy.vio.message.SystemMessage;

public interface Provider {

    ConversationMessage request(AccessLevel accessLevel, SystemMessage systemMessage, Conversation conversation);
}
