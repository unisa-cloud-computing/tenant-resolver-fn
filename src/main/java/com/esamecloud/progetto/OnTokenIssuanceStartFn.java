package com.esamecloud.progetto;

import com.esamecloud.progetto.service.CatalogService;
import com.esamecloud.progetto.utils.JwtValidator;
import com.esamecloud.progetto.utils.TokenResponseBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Azure Functions with HTTP Trigger.
 */
public class OnTokenIssuanceStartFn {

    private final ObjectMapper objectMapper;
    private final CatalogService catalogService;

    public OnTokenIssuanceStartFn() {
        this.objectMapper = new ObjectMapper();
        this.catalogService = new CatalogService();
    }

    /**
     * This function listens at endpoint "token/issuance/start".
     */
    @FunctionName("OnTokenIssuanceStartFn")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                route = "token/issuance/start",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        final Logger log = context.getLogger();

        try {
            log.info("Java HTTP trigger processed a request");

            // 1. Valida Authorization header (Bearer token da Entra)
            String authHeader = request.getHeaders().get("authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warning("Missing or invalid Authorization header");
                return request.createResponseBuilder(HttpStatus.UNAUTHORIZED).build();
            }

            String accessToken = authHeader.substring("Bearer ".length());

            JwtValidator.validate(accessToken);

            // 2. Leggi il body JSON dell'evento
            final String body = request.getBody().orElse("");
            log.info("body received: " + body);
            final JsonNode root = objectMapper.readTree(body);

            // 3. Estrai user.id (oid) dal payload
            final JsonNode userNode = root.path("data")
                    .path("authenticationContext")
                    .path("user");

            final String oid = userNode.path("id").asText(null);
            final String displayName = userNode.path("displayName").asText(null);

            if (oid == null || oid.isBlank() || displayName == null || displayName.isBlank()) {
                log.warning("User id or displayName not found in request");
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).build();
            }

            // 4. Catalog lookup/provisioning
            Long tenantId = catalogService.resolveOrCreateTenantForUser(oid, displayName);

            log.info("tenant id retrieved: " + tenantId);

            // 5. Costruisci la risposta con il claim tenantId
            ObjectNode responseJson = TokenResponseBuilder.buildTenantIdResponse(objectMapper, tenantId);

            log.info("response " + responseJson);

            var response = request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(responseJson.toString())
                    .build();

            log.info("response " + response);

            return response;

        }
        catch (Exception ex) {
            log.severe("Error in OnTokenIssuanceStartFn: " + ex.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
