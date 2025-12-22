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

        // Otros tipos -> null
        return null;
    }

    private Promotion handleStringValue(JsonParser p) throws IOException {
        String s = p.getValueAsString();
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            Long id = Long.valueOf(s);
            Promotion pr = new Promotion();
            pr.setId(id);
            return pr;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Promotion handleNumberValue(JsonParser p) throws IOException {
        Long id = p.getLongValue();
        Promotion pr = new Promotion();
        pr.setId(id);
        return pr;
    }

    private Promotion handleObjectValue(JsonParser p) throws IOException {
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
}
