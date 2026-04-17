CREATE TABLE [SCHEMA_NAME].ANIMALE (
                                       id      BIGINT          NOT NULL IDENTITY(1,1),
    razza   NVARCHAR(255)   NULL,
    colore  NVARCHAR(255)   NULL,
    note    NVARCHAR(255)   NULL,

    CONSTRAINT PK_ANIMALE PRIMARY KEY (id)
    );

CREATE TABLE [SCHEMA_NAME].LOCALE (
                                      id      BIGINT          NOT NULL IDENTITY(1,1),
    nome    NVARCHAR(255)   NOT NULL,

    CONSTRAINT PK_LOCALE PRIMARY KEY (id)
    );

CREATE TABLE [SCHEMA_NAME].FORNITORE (
                                         id                  BIGINT          NOT NULL IDENTITY(1,1),
    CODICE_PROVENIENZA  NVARCHAR(255)   NULL,
    PARTITA_IVA         NVARCHAR(255)   NOT NULL,
    telefono            NVARCHAR(255)   NULL,
    indirizzo           NVARCHAR(255)   NULL,

    CONSTRAINT PK_FORNITORE         PRIMARY KEY (id),
    CONSTRAINT UQ_FORNITORE_PIVA    UNIQUE      (PARTITA_IVA)
    );

CREATE TABLE [SCHEMA_NAME].CLIENTE (
                                       id                          BIGINT          NOT NULL IDENTITY(1,1),
    NOME                        NVARCHAR(255)   NOT NULL,
    COGNOME                     NVARCHAR(255)   NOT NULL,
    CELLULARE                   NVARCHAR(255)   NOT NULL,
    TELEFONO                    NVARCHAR(255)   NULL,
    CODICE_FISCALE              NVARCHAR(255)   NULL,
    EMAIL                       NVARCHAR(255)   NULL,
    INDIRIZZO                   NVARCHAR(255)   NOT NULL,
    PROV                        NVARCHAR(255)   NOT NULL,
    COMUNE                      NVARCHAR(255)   NOT NULL,
    CODICE_IDENTIFICATIVO_ASL   NVARCHAR(255)   NULL,
    DATA_DI_NASCITA             DATE            NULL,
    ELIMINATO                   BIT             NOT NULL DEFAULT 0,

    CONSTRAINT PK_CLIENTE       PRIMARY KEY (id),
    CONSTRAINT UQ_CLIENTE_CF    UNIQUE      (CODICE_FISCALE)
    );

CREATE TABLE [SCHEMA_NAME].ORDINE (
                                      ID_ORDINE       BIGINT          NOT NULL IDENTITY(1,1),
    DATA            DATE            NOT NULL,
    ID_CLIENTE      BIGINT          NOT NULL,
    STATO           NVARCHAR(30)    NOT NULL,
    NOTE_ORDINE     NVARCHAR(MAX)   NULL,
    NOTE_SCATOLE    NVARCHAR(MAX)   NULL,
    NOTE_MANGIME    NVARCHAR(MAX)   NULL,
    SPESA_SCATOLE   DECIMAL(19, 4)  NULL,
    SPESA_MANGIME   DECIMAL(19, 4)  NULL,
    TOTALE          FLOAT           NOT NULL,

    CONSTRAINT PK_ORDINE            PRIMARY KEY (ID_ORDINE),
    CONSTRAINT FK_ORDINE_CLIENTE    FOREIGN KEY (ID_CLIENTE)
    REFERENCES [SCHEMA_NAME].CLIENTE (id)
    );

CREATE TABLE [SCHEMA_NAME].LOTTO (
                                     id                  BIGINT          NOT NULL IDENTITY(1,1),
    id_animale          BIGINT          NOT NULL,
    id_locale           BIGINT          NOT NULL,
    id_fornitore        BIGINT          NOT NULL,
    data_di_nascita     DATE            NOT NULL,
    quantita_iniziale   INT             NOT NULL,
    quantita_corrente   INT             NOT NULL,
    prezzo_unitario     DECIMAL(19, 4)  NULL,
    numero_morti        INT             NULL DEFAULT 0,

    CONSTRAINT PK_LOTTO             PRIMARY KEY (id),
    CONSTRAINT FK_LOTTO_ANIMALE     FOREIGN KEY (id_animale)
    REFERENCES [SCHEMA_NAME].ANIMALE (id),
    CONSTRAINT FK_LOTTO_LOCALE      FOREIGN KEY (id_locale)
    REFERENCES [SCHEMA_NAME].LOCALE (id),
    CONSTRAINT FK_LOTTO_FORNITORE   FOREIGN KEY (id_fornitore)
    REFERENCES [SCHEMA_NAME].FORNITORE (id)
    );

CREATE TABLE [SCHEMA_NAME].DETTAGLIO_ORDINE (
                                                ID_DETTAGLIO_ORDINE BIGINT          NOT NULL IDENTITY(1,1),
    ID_ORDINE           BIGINT          NOT NULL,
    ID_LOTTO            BIGINT          NOT NULL,
    QUANTITA            INT             NOT NULL,
    PESO                DECIMAL(19, 4)  NULL,
    PREZZO_UNITARIO     DECIMAL(19, 4)  NULL,
    NOTE                NVARCHAR(255)   NULL,

    CONSTRAINT PK_DETTAGLIO_ORDINE  PRIMARY KEY (ID_DETTAGLIO_ORDINE),
    CONSTRAINT FK_DETT_ORDINE       FOREIGN KEY (ID_ORDINE)
    REFERENCES [SCHEMA_NAME].ORDINE (ID_ORDINE)
    ON DELETE CASCADE,
    CONSTRAINT FK_DETT_LOTTO        FOREIGN KEY (ID_LOTTO)
    REFERENCES [SCHEMA_NAME].LOTTO (id)
    );

CREATE TABLE [SCHEMA_NAME].SCHEDA_VACCINAZIONE (
                                                   ID_SCHEDA_VACCINAZIONE  BIGINT          NOT NULL IDENTITY(1,1),
    NOME_FILE               NVARCHAR(255)   NOT NULL,
    BLOB_NAME               NVARCHAR(255)   NOT NULL,
    ORDINE_ID               BIGINT          NOT NULL,

    CONSTRAINT PK_SCHEDA_VACCINAZIONE   PRIMARY KEY (ID_SCHEDA_VACCINAZIONE),
    CONSTRAINT UQ_SCHEDA_NOME_FILE      UNIQUE      (NOME_FILE),
    CONSTRAINT UQ_SCHEDA_BLOB_NAME      UNIQUE      (BLOB_NAME),
    CONSTRAINT UQ_SCHEDA_ORDINE         UNIQUE      (ORDINE_ID),
    CONSTRAINT FK_SCHEDA_ORDINE         FOREIGN KEY (ORDINE_ID)
    REFERENCES [SCHEMA_NAME].ORDINE (ID_ORDINE)
    );

CREATE TABLE [SCHEMA_NAME].MODELLO04 (
                                         PROGRESSIVO  BIGINT          NOT NULL IDENTITY(1,1),
    NOME_FILE   NVARCHAR(255)   NOT NULL,
    SERIE       NVARCHAR(255)   NULL,
    BLOB_NAME   NVARCHAR(255)   NOT NULL,
    ORDINE_ID   BIGINT          NOT NULL,

    CONSTRAINT PK_PROGRESSIVO                 PRIMARY KEY (PROGRESSIVO),
    CONSTRAINT UQ_MODELLO04_NOME_FILE       UNIQUE      (NOME_FILE),
    CONSTRAINT UQ_MODELLO04_BLOB_NAME       UNIQUE      (BLOB_NAME),
    CONSTRAINT UQ_MODELLO04_ORDINE          UNIQUE      (ORDINE_ID),
    CONSTRAINT FK_MODELLO04_ORDINE          FOREIGN KEY (ORDINE_ID)
    REFERENCES [SCHEMA_NAME].ORDINE (ID_ORDINE)
    );

CREATE TABLE [SCHEMA_NAME].LOG (
                                   id                  BIGINT          NOT NULL IDENTITY(1,1),
    data_evento         DATETIME2       NOT NULL,
    utente              NVARCHAR(255)   NOT NULL,
    tipo_operazione     NVARCHAR(255)   NOT NULL,
    valore_corrente     NVARCHAR(MAX)   NOT NULL,
    valore_precedente   NVARCHAR(MAX)   NOT NULL,
    descrizione         NVARCHAR(MAX)   NULL,
    tabella_target      NVARCHAR(255)   NOT NULL,
    id_record_target    NVARCHAR(255)   NOT NULL,

    CONSTRAINT PK_LOG PRIMARY KEY (id)
    );
