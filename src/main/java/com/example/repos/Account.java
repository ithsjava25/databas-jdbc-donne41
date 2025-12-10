package com.example.repos;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;

import static java.lang.System.out;

public class Account {
    private final Datasource dataSource;

    /**
     * Creates a new Account repository backed by the provided Datasource.
     *
     * @param dataSource the Datasource used to obtain database connections for account operations
     */
    public Account(Datasource dataSource) {
        this.dataSource = dataSource;
    }


/**
     * Inserts a new account record into the database.
     *
     * @param firstname the account holder's first name
     * @param lastname the account holder's last name
     * @param ssn the account holder's social security number
     * @param password the account password (stored as provided)
     * @param name the account username
     * @return true if the account was created (at least one row affected), false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean createAccount(String firstname, String lastname, String ssn, String password, String name) throws SQLException{
        String sql = "INSERT INTO account (name, password, first_name, last_name, ssn) VALUES (?,?,?,?,?)";

        try (Connection con = dataSource.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)){

            ps.setString(1, name);
            ps.setString(2, password);
            ps.setString(3, firstname);
            ps.setString(4, lastname);
            ps.setString(5, ssn);

            int rows = ps.executeUpdate();
            if(rows > 0){
                return true;
            }else{
                return false;
            }

        }
    }

    /**
     * Update the password for the account with the given user ID.
     *
     * @param id       the user_id of the account to update
     * @param password the new password to set for the account
     * @return         true if at least one row was updated, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updateAccount(int id, String password) throws SQLException {
        String sql = "UPDATE account SET password = ? WHERE user_id = ?;";

        try(Connection con = dataSource.getConnection();
            PreparedStatement updatePass = con.prepareStatement(sql)) {

            updatePass.setInt(2, id);
            updatePass.setString(1, password);

            int rows = updatePass.executeUpdate();
            if(rows > 0){
                return true;
            }else{
                return false;
            }
        }
    }

    /**
     * Deletes the account with the given user id.
     *
     * @param id the user_id of the account to delete
     * @return `true` if a row was deleted, `false` otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean deleteAccount(int id) throws SQLException {
        String sql = "DELETE FROM account WHERE user_id = ?;";

        try(Connection con = dataSource.getConnection();
        PreparedStatement deleteAcc = con.prepareStatement(sql)){

            deleteAcc.setInt(1, id);

            int rows = deleteAcc.executeUpdate();
            if(rows > 0){
                return true;
            }else{
                return false;
            }
        }
    }

    /**
         * Retrieves the stored password for the account with the given username.
         *
         * @param username the account name to look up
         * @return the password for the account, or an empty string if no matching account exists
         * @throws SQLException if a database access error occurs
         */
        public String login(String username) throws SQLException{
        String sql = "select password from account where name = ?;";

        try(Connection con = dataSource.getConnection();
        PreparedStatement ps = con.prepareStatement(sql)){

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                return rs.getString(1);
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
        String showTables = "show tables from testdb";
        int tables = 0;
        boolean hasDb = false;
        boolean hasAccount = false;
        boolean hasMission = false;

        try (Connection con = dataSource.getConnection();
        PreparedStatement showdb = con.prepareStatement(showDb);
        PreparedStatement showtable = con.prepareStatement(showTables)){

            ResultSet dbresult = showdb.executeQuery();
            while(dbresult.next()){
                if(dbresult.getString("Database").equals("testdb")){
                    hasDb = true;
                }
            }
            if(hasDb){
                ResultSet tableResult = showtable.executeQuery();
                while(tableResult.next()){
                    if(tableResult.getString(1).matches("account")){
                        hasAccount = true;
                    }
                    if(tableResult.getString(1).matches("moon_mission")){
                        hasMission = true;
                    }
                }
            }
            if(hasMission && hasAccount){
                return false;
            }
        }

        try (Connection con = dataSource.getConnection();
             Statement statement = con.createStatement()){

            String init = Paths.get("src/main/resources/init.sql").toString();
            BufferedReader br = new BufferedReader(new FileReader(init));

            StringBuilder query = new StringBuilder();
            String line;
            out.println("File found at :" + init);
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
        return true;
    }
}
