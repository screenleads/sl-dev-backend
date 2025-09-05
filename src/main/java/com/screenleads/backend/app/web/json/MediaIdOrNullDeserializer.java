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

        // null -> null
        if (t == JsonToken.VALUE_NULL)
            return null;

        // "" -> null | "2" -> id=2
        if (t == JsonToken.VALUE_STRING) {
            String s = p.getValueAsString();
            if (s == null || s.isBlank())
                return null;
            try {
                Long id = Long.valueOf(s);
                Media m = new Media();
                m.setId(id);
                return m;
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        // 2 -> id=2
        if (t == JsonToken.VALUE_NUMBER_INT) {
            Long id = p.getLongValue();
            Media m = new Media();
            m.setId(id);
            return m;
        }

        // { "id": 2 } | { "id": "2" }
        if (t == JsonToken.START_OBJECT) {
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

        // Cualquier otro tipo -> null (para ser tolerantes)
        return null;
    }
}
