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

    public Account(Datasource dataSource) {
        this.dataSource = dataSource;
    }


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

