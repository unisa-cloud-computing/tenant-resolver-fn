package com.esamecloud.progetto.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TokenResponseBuilder {

    public static ObjectNode buildTenantIdResponse(ObjectMapper mapper, Long tenantId) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode data = mapper.createObjectNode();

        data.put("@odata.type", "microsoft.graph.onTokenIssuanceStartResponseData");

        ArrayNode actions = mapper.createArrayNode();
        ObjectNode action = mapper.createObjectNode();
        action.put("@odata.type", "microsoft.graph.tokenIssuanceStart.provideClaimsForToken");

        ObjectNode claims = mapper.createObjectNode();
        claims.put("tenantId", String.valueOf(tenantId));

        action.set("claims", claims);
        actions.add(action);

        data.set("actions", actions);
        root.set("data", data);

        return root;
    }
}

