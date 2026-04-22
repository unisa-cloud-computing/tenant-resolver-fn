# tenant-resolver-fn

Azure Function Java che intercetta l'evento **OnTokenIssuanceStart** (Microsoft Entra External ID), valida il token ricevuto e risolve/provisiona il tenant applicativo, restituendo un claim custom `tenantId` da aggiungere al token in emissione.

## Panoramica

La funzione HTTP:

1. riceve una richiesta POST su `token/issuance/start`
2. valida il bearer token Entra (firma + issuer + audience)
3. legge dal body evento i dati utente (`oid`, `displayName`)
4. cerca il tenant associato nel catalogo (`TENANTS_USERS`)
5. se non esiste, crea tenant + mapping utente e provisiona un nuovo database tenant su Azure SQL
6. risponde con payload Microsoft Graph contenente il claim `tenantId`

Classe entrypoint: `src/main/java/com/esamecloud/progetto/OnTokenIssuanceStartFn.java`

---

## Stack Tecnologico

- Java 17
- Azure Functions Java Library (`com.microsoft.azure.functions:azure-functions-java-library`)
- Azure Functions Maven Plugin
- Azure SQL + SQL Server JDBC
- Azure Resource Manager SQL (`azure-resourcemanager-sql`)
- Azure Identity (`DefaultAzureCredential`)
- Nimbus JOSE JWT (validazione token)
- Jackson (JSON)
- Maven

Riferimento dipendenze: `pom.xml`

---

## Struttura Progetto

- `src/main/java/com/esamecloud/progetto/OnTokenIssuanceStartFn.java`  
  HTTP trigger principale
- `src/main/java/com/esamecloud/progetto/utils/JwtValidator.java`  
  Validazione JWT Entra (issuer/audience/JWKS)
- `src/main/java/com/esamecloud/progetto/service/CatalogService.java`  
  Lookup tenant su catalogo + creazione tenant utente
- `src/main/java/com/esamecloud/progetto/service/DatabaseProvisioningService.java`  
  Provisioning DB tenant in Azure SQL + esecuzione script SQL
- `src/main/java/com/esamecloud/progetto/utils/TokenResponseBuilder.java`  
  Costruzione payload risposta per aggiunta claim
- `src/main/resources/tenant-ddl.sql`  
  DDL schema applicativo tenant
- `src/main/resources/create-aca-users.sql`  
  Creazione utenti SQL esterni per ACA services
- `host.json`  
  Config host Azure Functions
- `local.settings.json`  
  Config locale Functions (non usare in produzione con segreti in chiaro)

---

## Endpoint

### HTTP Trigger

- **Metodo:** `POST`
- **Route Function:** `token/issuance/start`
- **Auth Level Function:** `ANONYMOUS` (la sicurezza è nel bearer token validato a runtime)

In locale con Azure Functions Core Tools, endpoint tipico:

- `http://localhost:7071/api/token/issuance/start`

---

## Flusso Dettagliato

1. Lettura header `authorization` (deve iniziare con `Bearer `)
2. Chiamata `JwtValidator.validate(accessToken)`
3. Parsing body JSON e lettura:
   - `data.authenticationContext.user.id`
   - `data.authenticationContext.user.displayName`
4. `CatalogService.resolveOrCreateTenantForUser(oid, displayName)`:
   - query su `dbo.TENANTS_USERS`
   - se assente:
     - insert su `dbo.TENANTS`
     - insert su `dbo.TENANTS_USERS`
     - provisioning DB tenant in Azure SQL
5. Costruzione risposta con:
   - `microsoft.graph.onTokenIssuanceStartResponseData`
   - action `provideClaimsForToken`
   - claim `tenantId`

---

## Prerequisiti

- JDK 17
- Maven 3.9+
- Azure Functions Core Tools v4
- Accesso Azure con permessi su SQL Server / resource group
- SQL Server catalogo già esistente (`db-catalog`) con tabelle:
  - `dbo.TENANTS`
  - `dbo.TENANTS_USERS`
- Identità/credenziali Azure valide per:
  - ARM API (creazione database)
  - connessione SQL admin (esecuzione script DDL)

---

## Configurazione

### Variabili d'ambiente usate dal codice

In particolare da `DatabaseProvisioningService`:

- `AZURE_SUBSCRIPTION_ID`
- `AZURE_TENANT_ID`
- `SQL_ADMIN`
- `SQL_ADMIN_PASSWORD`

Inoltre la funzione usa `DefaultAzureCredential`, quindi puoi autenticarti localmente con Azure CLI/VS Code/Azure Identity chain.

> Nota: nel codice sono presenti alcuni valori hardcoded (es. server SQL, resource group, elastic pool, tenant Entra, app audience). Se vuoi rendere il progetto portabile, conviene esternalizzarli in env vars o App Settings.

---

## Esecuzione Locale

### 1) Build

```bash
mvn clean package
```

### 2) Avvio funzione locale

```bash
mvn azure-functions:run
```

### 3) Test chiamata endpoint (esempio)

```bash
curl -X POST "http://localhost:7071/api/token/issuance/start" \
  -H "Authorization: Bearer <ACCESS_TOKEN_ENTRA_VALIDO>" \
  -H "Content-Type: application/json" \
  -d '{
    "data": {
      "authenticationContext": {
        "user": {
          "id": "11111111-2222-3333-4444-555555555555",
          "displayName": "MarioRossi"
        }
      }
    }
  }'
```

---

## Formato Risposta Atteso

La funzione restituisce un JSON nel formato richiesto da OnTokenIssuanceStart, ad esempio:

```json
{
  "data": {
    "@odata.type": "microsoft.graph.onTokenIssuanceStartResponseData",
    "actions": [
      {
        "@odata.type": "microsoft.graph.tokenIssuanceStart.provideClaimsForToken",
        "claims": {
          "tenantId": "123"
        }
      }
    ]
  }
}
```

---

## Deployment (Azure Functions)

Con plugin Maven già configurato in `pom.xml`:

```bash
mvn clean package
mvn azure-functions:deploy
```

Verifica proprietà in `pom.xml` prima del deploy:

- `functionAppName`
- `resourceGroup`
- `region`

---

## Test

Test presenti:

- `src/test/java/com/esamecloud/progetto/OnTokenIssuanceStartFnTest.java`
- `src/test/java/com/esamecloud/progetto/HttpResponseMessageMock.java`

Esecuzione:

```bash
mvn test
```

---

## Note Operative

- `CatalogService` usa connessione al catalogo con `ActiveDirectoryManagedIdentity`.
- `DatabaseProvisioningService` usa SQL admin user/password per eseguire script DDL sul nuovo DB tenant.
- Gli script SQL eseguiti in provisioning sono:
  - `tenant-ddl.sql`
  - `create-aca-users.sql`
- In caso di errore durante provisioning, il DB appena creato viene eliminato (best effort rollback a livello infrastrutturale).

---

## Troubleshooting Rapido

- **401 Unauthorized**: header `Authorization` mancante/non valido, token non valido in `JwtValidator`.
- **400 Bad Request**: body senza `data.authenticationContext.user.id` o `displayName`.
- **500 Internal Server Error**: errore SQL/ARM/provisioning; controllare log function.
- **Errore JWKS/issuer/audience**: verificare configurazione Entra in `JwtValidator`.

---

## Miglioramenti Consigliati

- Spostare valori hardcoded (`tenant id`, `issuer`, `audience`, SQL server, resource group) in variabili ambiente.
- Rimuovere segreti da `local.settings.json` e usare secret store.
- Rafforzare i test unitari/mocking su casi errore e flusso provisioning.
- Sanitizzare `displayName` prima di usarlo in nomi DB/schema.
```