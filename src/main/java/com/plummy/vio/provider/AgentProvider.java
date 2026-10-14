package com.plummy.vio.provider;

import com.plummy.vio.conversation.Conversation;
import com.plummy.vio.message.ConversationMessage;
import com.plummy.vio.message.SystemMessage;
import com.plummy.vio.tool.Tool;

import java.util.Collection;

public interface AgentProvider {

    ConversationMessage request(SystemMessage systemMessage, Conversation conversation, Collection<Tool> tools);
}
