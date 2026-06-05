package com.example.reminder.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.LocalTime;

public class FlexibleLocalTimeDeserializer extends JsonDeserializer<LocalTime> {

    @Override
    public LocalTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            String value = parser.getValueAsString();
            return value == null || value.isBlank() ? null : LocalTime.parse(value);
        }
        if (token == JsonToken.START_OBJECT) {
            JsonNode node = parser.getCodec().readTree(parser);
            int hour = readInt(node, "hour", 0);
            int minute = readInt(node, "minute", 0);
            int second = readInt(node, "second", 0);
            int nano = readInt(node, "nano", 0);
            return LocalTime.of(hour, minute, second, nano);
        }
        return (LocalTime) context.handleUnexpectedToken(LocalTime.class, parser);
    }

    private int readInt(JsonNode node, String fieldName, int defaultValue) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? defaultValue : value.asInt();
    }
}
