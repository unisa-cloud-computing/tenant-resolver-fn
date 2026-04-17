DECLARE @principal_name SYSNAME = '__ACA1_SQL_USER__';
DECLARE @clientId UNIQUEIDENTIFIER = '__ACA1_CLIENT_ID__';
DECLARE @castClientId NVARCHAR(MAX) =
    CONVERT(VARCHAR(MAX), CONVERT(VARBINARY(16), @clientId), 1);

DECLARE @cmd NVARCHAR(MAX) =
    N'CREATE USER [' + @principal_name + N'] WITH SID = ' + @castClientId + N', TYPE = E;';
EXEC (@cmd);

DECLARE @cmdRole1 NVARCHAR(MAX) =
    N'ALTER ROLE db_datareader ADD MEMBER [' + @principal_name + N'];';
EXEC (@cmdRole1);

DECLARE @cmdRole2 NVARCHAR(MAX) =
    N'ALTER ROLE db_datawriter ADD MEMBER [' + @principal_name + N'];';
EXEC (@cmdRole2);

DECLARE @principal_name_2 SYSNAME = '__ACA2_SQL_USER__';
DECLARE @clientId_2 UNIQUEIDENTIFIER = '__ACA2_CLIENT_ID__';
DECLARE @castClientId_2 NVARCHAR(MAX) =
    CONVERT(VARCHAR(MAX), CONVERT(VARBINARY(16), @clientId_2), 1);

DECLARE @cmd NVARCHAR(MAX) =
    N'CREATE USER [' + @principal_name_2 + N'] WITH SID = ' + @castClientId_2 + N', TYPE = E;';
EXEC (@cmd);

DECLARE @cmdRole3 NVARCHAR(MAX) =
    N'ALTER ROLE db_datareader ADD MEMBER [' + @principal_name_2 + N'];';
EXEC (@cmdRole3);

DECLARE @cmdRole4 NVARCHAR(MAX) =
    N'ALTER ROLE db_datawriter ADD MEMBER [' + @principal_name_2 + N'];';
EXEC (@cmdRole4);


DECLARE @principal_name_3 SYSNAME = '__ACA3_SQL_USER__';
DECLARE @clientId_3 UNIQUEIDENTIFIER = '__ACA3_CLIENT_ID__';
DECLARE @castClientId_3 NVARCHAR(MAX) =
    CONVERT(VARCHAR(MAX), CONVERT(VARBINARY(16), @clientId_3), 1);

DECLARE @cmd NVARCHAR(MAX) =
    N'CREATE USER [' + @principal_name_3 + N'] WITH SID = ' + @castClientId_3 + N', TYPE = E;';
EXEC (@cmd);

DECLARE @cmdRole5 NVARCHAR(MAX) =
    N'ALTER ROLE db_datareader ADD MEMBER [' + @principal_name_3 + N'];';
EXEC (@cmdRole5);

DECLARE @cmdRole6 NVARCHAR(MAX) =
    N'ALTER ROLE db_datawriter ADD MEMBER [' + @principal_name_3 + N'];';
EXEC (@cmdRole6);


DECLARE @principal_name_4 SYSNAME = '__ACA4_SQL_USER__';
DECLARE @clientId_4 UNIQUEIDENTIFIER = '__ACA4_CLIENT_ID__';
DECLARE @castClientId_4 NVARCHAR(MAX) =
    CONVERT(VARCHAR(MAX), CONVERT(VARBINARY(16), @clientId_4), 1);

DECLARE @cmd NVARCHAR(MAX) =
    N'CREATE USER [' + @principal_name_4 + N'] WITH SID = ' + @castClientId_4 + N', TYPE = E;';
EXEC (@cmd);

DECLARE @cmdRole7 NVARCHAR(MAX) =
    N'ALTER ROLE db_datareader ADD MEMBER [' + @principal_name_4 + N'];';
EXEC (@cmdRole7);

DECLARE @cmdRole8 NVARCHAR(MAX) =
    N'ALTER ROLE db_datawriter ADD MEMBER [' + @principal_name_4 + N'];';
EXEC (@cmdRole8);

DECLARE @principal_name_5 SYSNAME = '__ACA5_SQL_USER__';
DECLARE @clientId_5 UNIQUEIDENTIFIER = '__ACA5_CLIENT_ID__';
DECLARE @castClientId_5 NVARCHAR(MAX) =
    CONVERT(VARCHAR(MAX), CONVERT(VARBINARY(16), @clientId_5), 1);

DECLARE @cmd NVARCHAR(MAX) =
    N'CREATE USER [' + @principal_name_5 + N'] WITH SID = ' + @castClientId_5 + N', TYPE = E;';
EXEC (@cmd);

DECLARE @cmdRole9 NVARCHAR(MAX) =
    N'ALTER ROLE db_datareader ADD MEMBER [' + @principal_name_5 + N'];';
EXEC (@cmdRole9);

DECLARE @cmdRole10 NVARCHAR(MAX) =
    N'ALTER ROLE db_datawriter ADD MEMBER [' + @principal_name_5 + N'];';
EXEC (@cmdRole10);