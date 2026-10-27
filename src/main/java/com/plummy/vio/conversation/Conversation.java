package com.plummy.vio.conversation;

import com.plummy.vio.message.Message;
import com.plummy.vio.storage.StorageItem;

import java.util.List;

public interface Conversation extends StorageItem<String> {

    ConversationType getType();

    List<Message> getMessages();

    void addMessage(Message message);

    <T> T accept(ConversationVisitor<T> visitor);
}
