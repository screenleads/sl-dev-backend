package com.screenleads.backend.app.web.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.domain.model.Media;

public class MediaIdOrNullDeserializer extends JsonDeserializer<Media> {

    @Override
    public Media deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();

        if (t == JsonToken.VALUE_NULL) {
            return null;
        }

        if (t == JsonToken.VALUE_STRING) {
            return handleStringValue(p);
        }

        if (t == JsonToken.VALUE_NUMBER_INT) {
            return handleNumberValue(p);
        }

        if (t == JsonToken.START_OBJECT) {
            return handleObjectValue(p);
        }

        // Cualquier otro tipo -> null (para ser tolerantes)
        return null;
    }

    private Media handleStringValue(JsonParser p) throws IOException {
        String s = p.getValueAsString();
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            Long id = Long.valueOf(s);
            Media m = new Media();
            m.setId(id);
            return m;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Media handleNumberValue(JsonParser p) throws IOException {
        Long id = p.getLongValue();
        Media m = new Media();
        m.setId(id);
        return m;
    }

    private Media handleObjectValue(JsonParser p) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);
        JsonNode idNode = node.get("id");

        if (idNode == null || idNode.isNull()) {
            return null;
        }

        if (idNode.isNumber()) {
            Media m = new Media();
            m.setId(idNode.longValue());
            return m;
        }

        if (idNode.isTextual()) {
            try {
                Long id = Long.valueOf(idNode.asText());
                Media m = new Media();
                m.setId(id);
                return m;
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }
}
