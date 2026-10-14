package com.plummy.vio.registry;

import com.plummy.vio.conversation.ConversationJsonDeserializer;
import com.plummy.vio.message.MessageJsonDeserializer;
import com.plummy.vio.section.SectionJsonDeserializer;

public class DeserializerRegistry {

    public static final ConversationJsonDeserializer CONVERSATION_JSON_DESERIALIZER = new ConversationJsonDeserializer();

    public static final MessageJsonDeserializer MESSAGE_JSON_DESERIALIZER = new MessageJsonDeserializer();

    public static final SectionJsonDeserializer SECTION_JSON_DESERIALIZER = new SectionJsonDeserializer();
}
