package com.plummy.vio.registry;

import com.google.gson.JsonObject;
import com.plummy.vio.conversation.ConversationJsonSerializer;
import com.plummy.vio.conversation.ConversationVisitor;
import com.plummy.vio.message.MessageJsonSerializer;
import com.plummy.vio.message.MessageVisitor;
import com.plummy.vio.provider.OpenAIMessageJsonSerializer;
import com.plummy.vio.section.SectionContentSerializer;
import com.plummy.vio.section.SectionJsonSerializer;
import com.plummy.vio.section.SectionVisitor;

import java.util.List;

public class VisitorRegistry {

    public static final ConversationVisitor<JsonObject> CONVERSATION_JSON_SERIALIZER = new ConversationJsonSerializer();

    public static final MessageVisitor<JsonObject> MESSAGE_JSON_SERIALIZER = new MessageJsonSerializer();
    public static final MessageVisitor<List<JsonObject>> OPENAI_MESSAGE_JSON_SERIALIZER = new OpenAIMessageJsonSerializer();

    public static final SectionVisitor<JsonObject> SECTION_JSON_SERIALIZER = new SectionJsonSerializer();
    public static final SectionVisitor<String> SECTION_CONTENT_SERIALIZER = new SectionContentSerializer();
}
