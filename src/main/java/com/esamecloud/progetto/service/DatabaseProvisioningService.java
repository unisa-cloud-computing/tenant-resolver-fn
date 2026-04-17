package com.esamecloud.progetto.service;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.sql.SqlServerManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Logger;

public class DatabaseProvisioningService {
    private final SqlServerManager sqlManager;
    private final String resourceGroup = "rg-poultryfarm";
    private final String sqlServerName = "sql-saas-poultryfarm-dev";
    private final String sqlServerFqdn = sqlServerName + ".database.windows.net";  // per JDBC
    private final String sqlServerLocation = "Italy North";
    private final String elasticPoolName = "ep-saas-poultryfarm-dev";
    private final String defaultSchema = "dbo";

    private final String jdbcUrl = "jdbc:sqlserver://" + sqlServerFqdn + ":1433;"
            + "database=db-catalog;"
            + "user=" + System.getenv("SQL_ADMIN") + ";"
            + "password=" + System.getenv("SQL_ADMIN_PASSWORD") + ";"
            + "encrypt=true;"
            + "hostNameInCertificate=*.database.windows.net;"
            + "loginTimeout=30;";

    private final String subscription = System.getenv("AZURE_SUBSCRIPTION_ID");
    private final String tenantId = System.getenv("AZURE_TENANT_ID");
    private final Logger logger = Logger.getLogger(DatabaseProvisioningService.class.getName());

    public DatabaseProvisioningService() {
        AzureProfile profile = new AzureProfile(tenantId, subscription, AzureEnvironment.AZURE);
        this.sqlManager = SqlServerManager
                .authenticate(new DefaultAzureCredentialBuilder().build(), profile);
    }

    public void provisionTenantDatabaseInAzureSql(String dbName, String tenantName) throws SQLException, IOException {
        // 1. Crea il database nel server SQL e lo mette nell'Elastic Pool
        this.sqlManager
                .sqlServers()
                .databases()
                .define(dbName)
                .withExistingSqlServer(resourceGroup, sqlServerName, sqlServerLocation)
                .withExistingElasticPool(elasticPoolName)
                .create();

        // 2. Costruisci la connection string JDBC verso il nuovo DB tenant
        String tenantUrl = this.jdbcUrl.replace("database=db-catalog", "database=" + dbName);

        // 3. Esegui lo script DDL
        executeSqlScripts(tenantUrl);
    }

    private void executeSqlScripts(String tenantJdbcUrl) throws SQLException, IOException {
        this.logger.info("executing sql scripts");
        boolean committed = false;
        try (Connection conn = DriverManager.getConnection(tenantJdbcUrl)) {
            conn.setAutoCommit(false);

            try {
                String ddl = loadSqlScript("tenant-ddl.sql");
                Map<String, String> ddlPlaceHolders = new HashMap<>();
                ddlPlaceHolders.put("[SCHEMA_NAME]", this.defaultSchema);
                final List<String> sqlStatements = new ArrayList<>(splitSqlStatements(this.applyPlaceholders(ddl, ddlPlaceHolders)));

                String acaMiUserScript = loadSqlScript("create-aca-users.sql");
                Map<String, String> placeholdersAcaMiUsers = getStringStringMap();
                final String acaMiUserScriptWithValue = this.applyPlaceholders(acaMiUserScript, placeholdersAcaMiUsers);
                sqlStatements.add(acaMiUserScriptWithValue);

                this.logger.info("###### acaMiUserScript with value: " + acaMiUserScriptWithValue);

                this.logger.info("scripts loaded, executing statements...");

                for (String ddlStatement : sqlStatements) {
                    if (!ddlStatement.isBlank()) {
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(ddlStatement);
                        }
                    }
                }
                this.logger.info("All scripts executed successfully, committing transaction...");

                conn.commit();
                committed = true;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
            finally {
                if (!committed) { try { conn.rollback(); } catch (SQLException ignored) {} }
            }
        }
    }

    private static Map<String, String> getStringStringMap() {
        Map<String, String> placeholdersAcaMiUsers = new HashMap<>();
        placeholdersAcaMiUsers.put("__ACA1_SQL_USER__", "cliente-service");
        placeholdersAcaMiUsers.put("__ACA1_CLIENT_ID__", "5df6618d-fb78-4bcd-9c4f-f407db7b4f6e");

        placeholdersAcaMiUsers.put("__ACA2_SQL_USER__", "documento-service");
        placeholdersAcaMiUsers.put("__ACA2_CLIENT_ID__", "85c9877f-8211-48cf-80e6-9d740967b87a");

        placeholdersAcaMiUsers.put("__ACA3_SQL_USER__", "fornitore-service");
        placeholdersAcaMiUsers.put("__ACA3_CLIENT_ID__", "bc70f038-bd7e-4244-9e37-e8b66d61c855");

        placeholdersAcaMiUsers.put("__ACA4_SQL_USER__", "lotto-service");
        placeholdersAcaMiUsers.put("__ACA4_CLIENT_ID__", "7d002430-1420-4fce-9078-86ed760eaf09");

        placeholdersAcaMiUsers.put("__ACA5_SQL_USER__", "ordine-service");
        placeholdersAcaMiUsers.put("__ACA5_CLIENT_ID__", "130cf372-ad48-457b-9b1b-f0eb073638fe");
        return placeholdersAcaMiUsers;
    }

    private String loadSqlScript(String name) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(name)) {
            this.logger.info("Loading " + name + " script from database...");
            if (is == null) {
                throw new IOException(name + " not found on classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    private List<String> splitSqlStatements(String sql) {
        return Arrays.stream(sql.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String applyPlaceholders(String sql, Map<String, String> placeholders) {
        String result = sql;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
