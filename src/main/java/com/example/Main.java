package com.example;


import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Arrays;
import java.util.Scanner;

import static java.lang.System.*;

public class Main {
    String jdbcUrl;
    String dbUser;
    String dbPass;
    Scanner sc;


    static void main(String[] args) {
        System.out.println(System.getenv("DEV_MODE"));


        if (isDevMode(args)) {
            System.out.println("Starting dev Mode!");
            DevDatabaseInitializer.start();
        }
        new Main().run();
    }

    public void run() {
        // Resolve DB settings with precedence: System properties -> Environment variables
        jdbcUrl = resolveConfig("APP_JDBC_URL", "APP_JDBC_URL");
        dbUser = resolveConfig("APP_DB_USER", "APP_DB_USER");
        dbPass = resolveConfig("APP_DB_PASS", "APP_DB_PASS");

        if (jdbcUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException(
                    "Missing DB configuration. Provide APP_JDBC_URL, APP_DB_USER, APP_DB_PASS " +
                            "as system properties (-Dkey=value) or environment variables.");
        }


        //Todo: Starting point for your code
        //dbInitilization();
        logInPromt();
        //todo fixa testdb ansluting.


        //System.out.println("Database connection established");
    }

    private void dbInitilization() {
        try (Connection con = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
            if(con == null) {
                databaseDisconnected();
            }
            PreparedStatement checkforDb = con.prepareStatement("show databases");
            ResultSet rs = checkforDb.executeQuery();
            boolean hasTestDb = false;
            boolean hasAccountTable = false;
            boolean hasMoonMissionTable = false;
            while (rs.next()) {
                if (rs.getString("Database").matches("testdb")) {
                    hasTestDb = true;
                    //jdbcUrl = jdbcUrl.concat("/testdb");
                }
            }
            if (hasTestDb) {
                PreparedStatement checkforTables;
                checkforTables = con.prepareStatement("show tables from testdb");
                ResultSet rs2 = checkforTables.executeQuery();
                while (rs2.next()) {
                    if (rs2.getString(1).equals("account")) hasAccountTable = true;
                    if (rs2.getString(1).equals("moon_mission")) hasMoonMissionTable = true;
                }
            }
            if (hasAccountTable && hasMoonMissionTable) {
                return;
            } else
                return;
            //inputAccountAndMissionsfromfile(con);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void databaseDisconnected() {
        out.println("Database disconnected!");
        System.exit(1);
    }

    private void inputAccountAndMissionsfromfile(Connection con) {
        try {
            Statement statement = con.createStatement();
            String init = Paths.get("src/main/resources/init.sql").toString();
            BufferedReader br = new BufferedReader(new FileReader(init));

            StringBuilder query = new StringBuilder();
            String line;
            System.out.println("File found at :" + init);
            while ((line = br.readLine()) != null) {

                if (line.trim().startsWith("--")) {
                    continue;
                }
                if (line.trim().startsWith("USE")) {
                    int space = line.indexOf(' ');
                    String newLine = line.replace(";", "");
                    jdbcUrl = jdbcUrl.concat("/" + newLine.substring(space + 1));
                }
                query.append(line).append(" ");

                if (line.endsWith(";")) {
                    statement.execute(query.toString().trim());
                    query = new StringBuilder();
                }

            }

            System.out.println("Database created and data filled from file!");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void logInPromt() {
        try (Connection con = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
            if(con == null) {
                databaseDisconnected();
            }
            sc = new Scanner(System.in);

            //TODO Improve security, get all name from database, check if exists, then check password?
            out.println("Inout username readline");
            out.println("Username");
            String userName = sc.nextLine();
            //consider using char[] for storing passwords.
            out.println("Password");
            String password = sc.nextLine();

            out.println("username set to: " + userName);
            String nameRes = "";
            String passwordRes = "";
            if (userName.equals("0")) {
                exit();
            }
            if(password.equals("0")) {
                exit();
            }

            PreparedStatement loginStmnt;
            loginStmnt = con.prepareStatement("Select name, password from account where name = ?");
            loginStmnt.setString(1, userName);


            ResultSet userRes = loginStmnt.executeQuery();


            while (userRes.next()) {
                nameRes = userRes.getString("name");
                passwordRes = userRes.getString("password");
                out.println("Response name" + nameRes);
                out.println("Response pass" + passwordRes);
            }
            if (nameRes != null && passwordRes.equals(password)) {
                System.out.println("Logged in successfully!");
                options();
            } else {
                throw new Exception("invalid");
                 }
            } catch(Exception e){
                out.println("Invalid username or password!" + e.getMessage());
            }


        }
        private void options () {
            System.out.printf("""
                    1) List moon missions (prints spacecraft names from `moon_mission`).
                    2) Get a moon mission by mission_id (prints details for that mission).
                    3) Count missions for a given year (prompts: year; prints the number of missions launched that year).
                    4) Create an account (prompts: first name, last name, ssn, password; prints confirmation).
                    5) Update an account password (prompts: user_id, new password; prints confirmation).
                    6) Delete an account (prompts: user_id; prints confirmation).
                    0) Exit.
                    """);
            String inputChoice = sc.nextLine().trim();
            out.println(inputChoice);
            switch (inputChoice) {
                case "1" -> getMoonMission();
                case "2" -> getMoonMissionId();
                case "3" -> missionCountYear();
                case "4" -> createAccount();
                case "5" -> updateAccount();
                case "6" -> deleteAccount();
                case "0" -> exit();
                default -> {
                    System.out.println("Invalid selection, 1-6 or 0 for exit.");
                    options();
                }
            }

        }

        private void getMoonMission () {
            try (Connection con = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
                if(con == null) {
                    databaseDisconnected();
                }
                PreparedStatement getMission = con.prepareStatement("SELECT spacecraft FROM moon_mission");
                ResultSet rs = getMission.executeQuery();
                System.out.println("Spacecraft names: ");
                while (rs.next()) {
                    System.out.println(rs.getString("spacecraft"));
                }
                out.println("-------------");
                options();

            } catch (Exception e) {
                System.out.println("Error getting moon mission!" + e.getMessage());
            }

        }

        private void getMoonMissionId () {
            try (Connection con = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
                if(con == null) {
                    databaseDisconnected();
                }
                out.println("Input mission_ID:");
                String missionId = sc.nextLine().trim();
                //TODO input validation
                PreparedStatement getMission = con.prepareStatement("SELECT * FROM moon_mission WHERE mission_id = ?");
                getMission.setString(1, missionId);
                ResultSet rs = getMission.executeQuery();
                ResultSetMetaData rsmd = rs.getMetaData();
                out.println("Mission details \n ---------------");
                while (rs.next()) {
                    out.print(rsmd.getColumnName(1) + ": ");
                    out.println(rs.getString("mission_id"));
                    out.print(rsmd.getColumnName(2) + ": ");
                    out.println(rs.getString("spacecraft"));
                    out.print(rsmd.getColumnName(3) + ": ");
                    out.println(rs.getString("launch_date"));
                    out.print(rsmd.getColumnName(4) + ": ");
                    out.println(rs.getString("carrier_rocket"));
                    out.print(rsmd.getColumnName(5) + ": ");
                    out.println(rs.getString("operator"));
                    out.print(rsmd.getColumnName(6) + ": ");
                    out.println(rs.getString("mission_type"));
                    out.print(rsmd.getColumnName(7) + ": ");
                    out.println(rs.getString("outcome"));
                }
                options();
            } catch (Exception e) {
                System.out.println("something went wrong!" + e.getMessage());
            }
        }

        private void missionCountYear () {
            try (Connection con = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
                if(con == null) {
                    databaseDisconnected();
                }
                out.println("Input mission year:");
                String stringYear = sc.nextLine();
                Integer missionYear = null;
                if (stringYear.matches("\\d{4}")) {
                    missionYear = Integer.parseInt(stringYear);
                } else {
                    System.out.println("Input mission year as YYYY");
                    getMoonMission();
                }
                PreparedStatement getMission = con.prepareStatement("select count(*) as missionCount, year(launch_date) as launchyear from moon_mission where year(launch_date) = ? group by launchyear;");
                getMission.setInt(1, missionYear);
                ResultSet rs = getMission.executeQuery();

                while (rs.next()) {
                    out.println("Missions count year " + missionYear + ": " + rs.getInt("missionCount"));
                }
                options();

            } catch (SQLException e) {
                System.out.println("Error getting moon mission!" + e.getMessage());
            }
        }

        private void updateAccount () {
            Integer userId = null;
            out.println("User_id to update password: ");
            String stringId = sc.nextLine();
            if (stringId.matches("\\d+")) {
                userId = Integer.parseInt(stringId);
            } else {
                System.out.println("Input user Id number.");
                updateAccount();
            }
            try (Connection con = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
                if(con == null) {
                    databaseDisconnected();
                }
                String getUserId = "select user_id from account where user_id = ?";
                PreparedStatement userIdCheck = con.prepareStatement(getUserId);
                userIdCheck.setInt(1, userId);

                ResultSet getUserCheck = userIdCheck.executeQuery();
                String newPassword = "";
                while (getUserCheck.next()) {
                    int responseId = getUserCheck.getInt("user_id");
                    if (responseId == userId) {
                        out.println("Set new password for user: " + userId);
                        newPassword = sc.nextLine();

                        PreparedStatement updatePass = con.prepareStatement("update account set password = ? where user_id = ?;");
                        updatePass.setString(1, newPassword);
                        updatePass.setInt(2, userId);

                        updatePass.executeUpdate();
                        out.println("Password updated!");
                        options();
                    }
                }
                out.println("No user found.");
                options();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void createAccount () {
            try (Connection con = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
                if(con == null) {
                    databaseDisconnected();
                }
                //TODO Flytta alla input utanför try, så det är färdigt när con ska starta.
                out.println("First name of account.");
                String firstName = sc.nextLine();
                out.println("Last name of account.");
                String lastName = sc.nextLine();
                out.println("SSN of account.");
                String ssn = sc.nextLine();
                out.println("Password of account.");
                String password = sc.nextLine();

                StringBuilder sb = new StringBuilder();
                sb.append(firstName.substring(0, 3));
                sb.append(lastName.substring(0, 3));
                String name = sb.toString();
                PreparedStatement createAccount = con.prepareStatement("insert into account (name, password, first_name, last_name, ssn) values (?,?,?,?,?)");
                createAccount.setString(1, name);
                createAccount.setString(2, password);
                createAccount.setString(3, firstName);
                createAccount.setString(4, lastName);
                createAccount.setString(5, ssn);

                createAccount.executeUpdate();
                out.println("Account created!\n Username is: " + name);
            } catch (Exception e) {
                throw new  RuntimeException(e);
            }

        }

        private void exit () {
            System.out.println("Exiting...");
            logInPromt();
        }
        private void deleteAccount () {
            Integer userId = null;
            out.println("User_id to delete:");
            String stringId = sc.nextLine();
            out.println("StringID: " + stringId);
            if (stringId.matches("\\d+")) {
                userId = Integer.parseInt(stringId);
            } else {
                System.out.println("Input user Id number.");
                deleteAccount();
            }
            try (Connection con = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
                if(con == null) {
                    databaseDisconnected();
                }
                String getUserId = "select user_id, name from account where user_id = ?";
                PreparedStatement userIdCheck = con.prepareStatement(getUserId);
                userIdCheck.setInt(1, userId);

                ResultSet getUserCheck = userIdCheck.executeQuery();
                String userName = "";
                int id = 0;

                while (getUserCheck.next()) {
                    userName = getUserCheck.getString("name");
                    id = Integer.parseInt(getUserCheck.getString("user_id"));
                }
                if (id == userId) {
                    //String response = IO.readln("Delete user: " + userName + " " + id + "? \n 1 ) yes. \n 0 ) no. \n Anykey ) exit.");
                    String response = "1";
                    switch (response) {
                        case "1" -> {
                            PreparedStatement deleteUser = con.prepareStatement("delete from account where user_id = ?");
                            deleteUser.setInt(1, userId);
                            deleteUser.executeUpdate();
                            System.out.println("User deleted!");

                        }
                        case "0" -> {
                            deleteAccount();
                        }
                        default -> options();
                    }
                } else {
                    System.out.println("User_id: " + userId + " not found");
                    String tryAgain = IO.readln("1 ) try again. \n Anykey ) exit.").trim();
                    if (tryAgain.equals("1")) {
                        deleteAccount();
                    } else {
                        options();
                    }
                }


            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Determines if the application is running in development mode based on system properties,
         * environment variables, or command-line arguments.
         *
         * @param args an array of command-line arguments
         * @return {@code true} if the application is in development mode; {@code false} otherwise
         */
        private static boolean isDevMode (String[]args){
            if (Boolean.getBoolean("devMode"))  //Add VM option -DdevMode=true
                return true;
            if ("true".equalsIgnoreCase(System.getenv("DEV_MODE")))  //Environment variable DEV_MODE=true
                return true;
            for (String arg : args) {
                System.out.println(arg);
            }
            return Arrays.asList(args).contains("--dev"); //Argument --dev
        }

        /**
         * Reads configuration with precedence: Java system property first, then environment variable.
         * Returns trimmed value or null if neither source provides a non-empty value.
         */
        private static String resolveConfig (String propertyKey, String envKey){
            String v = System.getProperty(propertyKey);
            System.out.println("V get property: " + v);
            if (v == null || v.trim().isEmpty()) {
                v = System.getenv(envKey);
                System.out.println("v after == null: " + v);
            }
            return (v == null || v.trim().isEmpty()) ? null : v.trim();
        }
    }
