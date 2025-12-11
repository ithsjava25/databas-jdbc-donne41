package com.example.repos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Objects;

import static java.lang.System.out;

public class Account {
    private final Datasource dataSource;

    public Account(Datasource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Inserts a new account record into the database.
     *
     * @param firstname the account holder's first name
     * @param lastname  the account holder's last name
     * @param ssn       the account holder's social security number
     * @param password  the account password (stored as provided)
     * @param name      the account username
     * @return true if the account was created (at least one row affected), false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean createAccount(String firstname, String lastname, String ssn, String password, String name) throws SQLException {
        String sql = "INSERT INTO account (name, password, first_name, last_name, ssn) VALUES (?,?,?,?,?)";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, password);
            ps.setString(3, firstname);
            ps.setString(4, lastname);
            ps.setString(5, ssn);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update the password for the account with the given user ID.
     *
     * @param id       the user_id of the account to update
     * @param password the new password to set for the account
     * @return true if at least one row was updated, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updateAccount(int id, String password) throws SQLException {
        String sql = "UPDATE account SET password = ? WHERE user_id = ?;";

        try (Connection con = dataSource.getConnection();
             PreparedStatement updatePass = con.prepareStatement(sql)) {

            updatePass.setInt(2, id);
            updatePass.setString(1, password);

            int rows = updatePass.executeUpdate();
            if (rows > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prompts for a user ID and attempts to delete the corresponding account.
     * <p>
     * If the entered input is not a numeric user ID the method returns without taking action.
     * On successful deletion prints "Account deleted!", on failure prints "Failed to delete account!".
     * If a SQL error occurs during deletion the error message is printed.
     */
    public boolean deleteAccount(int id) throws SQLException {
        String sql = "DELETE FROM account WHERE user_id = ?;";

        try (Connection con = dataSource.getConnection();
             PreparedStatement deleteAcc = con.prepareStatement(sql)) {

            deleteAcc.setInt(1, id);

            int rows = deleteAcc.executeUpdate();
            if (rows > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the stored password for the account with the given username.
     *
     * @param username the account name to look up
     * @return the password for the account, or an empty string if no matching account exists
     * @throws SQLException if a database access error occurs
     */
    public String login(String username) throws SQLException {
        String sql = "select password from account where name = ?;";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return "";
    }


    /**
     * Checks whether the "testdb" database contains the required tables and, if not, executes the SQL statements
     * in src/main/resources/init.sql to initialize the schema.
     *
     * @return `true` if initialization was performed (or attempted), `false` if the required tables already exist
     * @throws SQLException if a database access error occurs while checking tables or executing statements
     * @throws IOException  if the initialization SQL file cannot be read
     */
    public boolean checkDatabaseTables() throws SQLException, IOException {
        String showDb = "show databases";
        String createDb = "create database testdb";
        String showTables = "show tables from testdb";
        boolean hasDb = false;
        boolean hasAccount = false;
        boolean hasMission = false;
        String tempUrl = dataSource.getUrl().replace("/testdb", "");

        try (Connection con = dataSource.getConnection(tempUrl);
             PreparedStatement showdb = con.prepareStatement(showDb);
             PreparedStatement createdb = con.prepareStatement(createDb)) {

            try (ResultSet dbresult = showdb.executeQuery()) {
                while (dbresult.next()) {
                    if (dbresult.getString("Database").equals("testdb")) {
                        hasDb = true;
                    }
                }
            }
            if (!hasDb) {
                int rows = createdb.executeUpdate();
                if (rows > 0) {
                    hasDb = true;
                }
            }
        }
        if (hasDb) {
            try (Connection con = dataSource.getConnection();
                 PreparedStatement showtable = con.prepareStatement(showTables);
                 ResultSet tableResult = showtable.executeQuery()) {
                while (tableResult.next()) {
                    if (tableResult.getString(1).equals("account")) {
                        hasAccount = true;
                    }
                    if (tableResult.getString(1).equals("moon_mission")) {
                        hasMission = true;
                    }
                }
            }
        }
        if (hasMission && hasAccount) {
            return false;
        }
        try (Connection con = dataSource.getConnection();
             Statement statement = con.createStatement()) {

            try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass()
                    .getClassLoader()
                    .getResourceAsStream("init.sql"))))) {

                StringBuilder query = new StringBuilder();
                String line;
                out.println("Loading file from classPath");
                while ((line = br.readLine()) != null) {

                    if (line.trim().startsWith("--")) {
                        continue;
                    }
                    query.append(line).append(" ");

                    if (line.endsWith(";")) {
                        statement.execute(query.toString().trim());
                        query = new StringBuilder();
                    }
                }
            }
        }
        return true;
    }
}

