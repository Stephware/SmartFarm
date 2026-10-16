/* SmartFarm — Step 1: Execute in DBeaver against the master database.
   Creates the database only if it does not exist.
   SQL Server 2022+. Do not run against a database with existing application data.
*/
IF DB_ID(N'SmartFarmDB') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE [SmartFarmDB]');
END;

SELECT name, state_desc FROM sys.databases WHERE name = N'SmartFarmDB';
