package com.esamecloud.progetto.service;

import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CatalogService {

    private final String sqlServerName = "sql-saas-poultryfarm-dev.database.windows.net";
    // gestito con managed identity
    private final String jdbcUrl = "jdbc:sqlserver://" + sqlServerName + ":1433;"
            + "database=db-catalog;"
            + "authentication=ActiveDirectoryManagedIdentity;"
            + "encrypt=true;"
            + "hostNameInCertificate=*.database.windows.net;"
            + "loginTimeout=30;";

    private final Logger logger = Logger.getLogger(CatalogService.class.getName());

    private final DatabaseProvisioningService dbProvisioningService;

    public CatalogService() {
        this.dbProvisioningService = new DatabaseProvisioningService();
    }

    public Long resolveOrCreateTenantForUser(String oid, String displayName) throws SQLException, IOException, InterruptedException {
        this.logger.log(Level.INFO, "Resolving tenant");
        Long existing = findTenantIdByUserOid(oid);
        if (existing != null) {
            return existing;
        }
        try {
            return createTenantForUser(oid, displayName);
        } catch (SQLException ex) {
            // L'altra richiesta ha creato il tenant un attimo prima
            Thread.sleep(500);
            Long tenantId = this.findTenantIdByUserOid(oid);
            if (tenantId != null) return tenantId;
            throw ex; // solo se davvero non lo trovi
        }
    }

    public Long findTenantIdByUserOid(String oid) throws SQLException {
        final String sql = """
            SELECT u.id_tenant
            FROM dbo.TENANTS_USERS u
            WHERE u.external_oid = ?
            """;

        try (Connection conn = DriverManager.getConnection(this.jdbcUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            this.logger.log(Level.INFO, "Executing query: " + sql);

            ps.setString(1, oid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    this.logger.log(Level.INFO, "Found tenant with oid: " + oid);
                    return rs.getLong("id_tenant");
                }
                this.logger.log(Level.INFO, "No tenant found for OID: " + oid);
                return null;
            }
        }
    }

    public Long createTenantForUser(String oid, String displayName) throws SQLException, IOException {

        this.logger.log(Level.INFO, "Creating tenant");
        try (Connection conn =  DriverManager.getConnection(this.jdbcUrl);) {
            conn.setAutoCommit(false);

            final String databaseName = "db-tenant-" + displayName.toLowerCase();
            final String tenantName = "tenant_" + displayName.toLowerCase() + "_" + oid;
            final String tenantSchema = "tenant_" + displayName.toLowerCase();

            try {
                Long tenantId;
                // 1. Crea tenant
                String insertTenant = "INSERT INTO dbo.TENANTS (tenant_name, database_name, schema_name) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertTenant, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, tenantName);
                    ps.setString(2, databaseName);
                    ps.setString(3, tenantSchema);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            this.logger.log(Level.INFO, "Created tenant with oid: " + oid);
                            this.logger.log(Level.INFO, "Result Set" + rs);
                            tenantId = rs.getLong(1);
                            this.logger.log(Level.INFO, "tenantId" + tenantId);
                        } else {
                            throw new SQLException("No TenantId generated");
                        }
                    }
                }

                // 2. Associa utente
                String insertUser = "INSERT INTO dbo.TENANTS_USERS (id_tenant, external_oid) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertUser)) {
                    ps.setString(1, tenantId.toString());
                    ps.setString(2, oid);
                    ps.executeUpdate();
                }

                // 3. Provisiona database tenant in Azure SQL
                this.dbProvisioningService.provisionTenantDatabaseInAzureSql(databaseName, tenantSchema);

                conn.commit();
                return tenantId;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        }

    }
}

