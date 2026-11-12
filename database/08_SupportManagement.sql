/* Phase 3: additive support tables. Back up SmartFarmDB before applying.
   Run after 07_AdminFoundation.sql; safe to rerun. No existing data is deleted. */
SET XACT_ABORT ON;
SET NOCOUNT ON;
IF DB_NAME() <> N'SmartFarmDB'
    THROW 50080, 'Connect to SmartFarmDB before running 08_SupportManagement.sql.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.SupportTicket', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.SupportTicket (
            Id UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_SupportTicket PRIMARY KEY,
            UserId UNIQUEIDENTIFIER NOT NULL,
            FarmId UNIQUEIDENTIFIER NULL,
            Title NVARCHAR(140) NOT NULL,
            Description NVARCHAR(2000) NOT NULL,
            Category NVARCHAR(20) NOT NULL,
            Status NVARCHAR(16) NOT NULL,
            Priority NVARCHAR(8) NOT NULL,
            CreatedAtUtc DATETIME2(3) NOT NULL,
            UpdatedAtUtc DATETIME2(3) NOT NULL,
            ResolvedAtUtc DATETIME2(3) NULL,
            RowVersion ROWVERSION NOT NULL,
            CONSTRAINT FK_SupportTicket_User FOREIGN KEY (UserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
            CONSTRAINT FK_SupportTicket_Farm FOREIGN KEY (FarmId) REFERENCES dbo.Farm(Id) ON DELETE NO ACTION,
            CONSTRAINT CK_SupportTicket_Status CHECK (Status IN (N'Open', N'InProgress', N'Resolved', N'Closed')),
            CONSTRAINT CK_SupportTicket_Priority CHECK (Priority IN (N'Normal', N'High')),
            CONSTRAINT CK_SupportTicket_Category CHECK (Category IN (N'Account', N'Farm', N'Mobile', N'Other'))
        );
    END;
    IF OBJECT_ID(N'dbo.SupportTicketMessage', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.SupportTicketMessage (
            Id UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_SupportTicketMessage PRIMARY KEY,
            TicketId UNIQUEIDENTIFIER NOT NULL,
            AuthorUserId UNIQUEIDENTIFIER NOT NULL,
            Body NVARCHAR(1200) NOT NULL,
            IsInternal BIT NOT NULL,
            CreatedAtUtc DATETIME2(3) NOT NULL,
            CONSTRAINT FK_SupportTicketMessage_Ticket FOREIGN KEY (TicketId)
                REFERENCES dbo.SupportTicket(Id) ON DELETE NO ACTION,
            CONSTRAINT FK_SupportTicketMessage_Author FOREIGN KEY (AuthorUserId)
                REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION
        );
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.SupportTicket') AND name = N'IX_SupportTicket_Status_Updated')
        CREATE INDEX IX_SupportTicket_Status_Updated ON dbo.SupportTicket(Status, UpdatedAtUtc);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.SupportTicket') AND name = N'IX_SupportTicket_User_Created')
        CREATE INDEX IX_SupportTicket_User_Created ON dbo.SupportTicket(UserId, CreatedAtUtc);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.SupportTicket') AND name = N'IX_SupportTicket_Farm')
        CREATE INDEX IX_SupportTicket_Farm ON dbo.SupportTicket(FarmId);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.SupportTicketMessage') AND name = N'IX_SupportTicketMessage_Ticket_Created')
        CREATE INDEX IX_SupportTicketMessage_Ticket_Created ON dbo.SupportTicketMessage(TicketId, CreatedAtUtc);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
