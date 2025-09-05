package com.screenleads.backend.app.web.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenleads.backend.app.domain.model.Promotion;

public class PromotionIdOrNullDeserializer extends JsonDeserializer<Promotion> {

    @Override
    public Promotion deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();

        // null -> null
        if (t == JsonToken.VALUE_NULL)
            return null;

        // "" -> null | "5" -> id=5
        if (t == JsonToken.VALUE_STRING) {
            String s = p.getValueAsString();
            if (s == null || s.isBlank())
                return null;
            try {
                Long id = Long.valueOf(s);
                Promotion pr = new Promotion();
                pr.setId(id);
                return pr;
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        // 5 -> id=5
        if (t == JsonToken.VALUE_NUMBER_INT) {
            Long id = p.getLongValue();
            Promotion pr = new Promotion();
            pr.setId(id);
            return pr;
        }

        // { "id": 5 } | { "id": "5" }
        if (t == JsonToken.START_OBJECT) {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode node = mapper.readTree(p);
            JsonNode idNode = node.get("id");

            if (idNode == null || idNode.isNull()) {
                return null;
            }

            if (idNode.isNumber()) {
                Promotion pr = new Promotion();
                pr.setId(idNode.longValue());
                return pr;
            }

            if (idNode.isTextual()) {
                try {
                    Long id = Long.valueOf(idNode.asText());
                    Promotion pr = new Promotion();
                    pr.setId(id);
                    return pr;
                } catch (NumberFormatException ex) {
                    return null;
                }
            }

            return null;
        }

        // Otros tipos -> null
        return null;
    }
}
