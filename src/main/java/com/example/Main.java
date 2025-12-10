package com.example;


import com.example.repos.Account;
import com.example.repos.Datasource;
import com.example.repos.MoonMission;
import com.example.repos.SimpleDriverManagerDataSource;

import java.io.IOException;
import java.sql.*;
import java.util.Arrays;
import java.util.Scanner;

import static java.lang.System.*;

public class Main {
    private Scanner sc;
    private Account accountRepo;
    private MoonMission moonMissionRepo;
    private boolean run;


    static void main(String[] args) {


        if (isDevMode(args)) {
            out.println("Starting dev Mode!");
            DevDatabaseInitializer.start();
        }
        new Main().run();
    }

    public void run() {
        // Resolve DB settings with precedence: System properties -> Environment variables
        String jdbcUrl = resolveConfig("APP_JDBC_URL", "APP_JDBC_URL");
        String dbUser = resolveConfig("APP_DB_USER", "APP_DB_USER");
        String dbPass = resolveConfig("APP_DB_PASS", "APP_DB_PASS");

        if (jdbcUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException(
                    "Missing DB configuration. Provide APP_JDBC_URL, APP_DB_USER, APP_DB_PASS " +
                            "as system properties (-Dkey=value) or environment variables.");
        }
        Datasource ds = new SimpleDriverManagerDataSource(jdbcUrl, dbUser, dbPass);
        accountRepo = new Account(ds);
        moonMissionRepo = new MoonMission(ds);
        sc = new Scanner(in);
        run = true;


        dbInitilization();
        try {
            while (run) {
                logInPromt();
                if (run) {
                    options();
                }
            }
        } catch (java.util.NoSuchElementException e) {
            out.println("Input stream ended unexpectedly: " + e.getMessage());
        }

    }

    private void dbInitilization() {

        try {
            if (accountRepo.checkDatabaseTables()) {
                out.println("Database did not exist or tables had been removed.\n" +
                        "Database has been created and/or tables with content from init file.");
            }
        } catch (SQLException e) {
            out.println("Sql error connection to database or doing queries" + e.getMessage());
        } catch (IOException e) {
            out.println("IO error reading data from file to database" + e.getMessage());
        }
    }


    private void logInPromt() {
        boolean invalidInput = true;
        String userName = "";
        String password = "";
        while (invalidInput) {
            out.println("Username or 0 for exit.");
            userName = sc.nextLine();
            if (userName.equals("0")) {
                out.println("Exiting..");
                run = false;
                return;
            }
            //consider using hashed passwords.
            out.println("Password or 0 for exit");
            password = sc.nextLine();
            if (password.equals("0")) {
                out.println("Exiting..");
                run = false;
                return;
            }
            try {
                if (password.equals(accountRepo.login(userName))) {
                    out.println("Logged in successfully!");
                    invalidInput = false;
                } else {
                    out.println("Invalid username or password");
                }
            } catch (SQLException e) {
                out.println("Error getting loging" + e.getMessage());
            }
        }
    }

    private void options() {
        String inputChoice = "";
        while (!inputChoice.equals("0")) {
            out.println("----------------");
            out.print("""
                    1) List moon missions (prints spacecraft names from `moon_mission`).
                    2) Get a moon mission by mission_id (prints details for that mission).
                    3) Count missions for a given year (prompts: year; prints the number of missions launched that year).
                    4) Create an account (prompts: first name, last name, ssn, password; prints confirmation).
                    5) Update an account password (prompts: user_id, new password; prints confirmation).
                    6) Delete an account (prompts: user_id; prints confirmation).
                    0) Exit.
                    """);
            inputChoice = sc.nextLine();
            switch (inputChoice) {
                case "1" -> getMoonMission();
                case "2" -> getMoonMissionId();
                case "3" -> missionCountYear();
                case "4" -> createAccount();
                case "5" -> updateAccount();
                case "6" -> deleteAccount();
                case "0" -> out.println("Logging out..");
                default -> out.println("Invalid selection, 1-6 or 0 for exit.");

            }
        }
    }

    private void getMoonMission() {
        try {
            var spacecraft = moonMissionRepo.getSpaceCraft();

            if (spacecraft.isEmpty()) {
                out.println("No moon missions found!");
                return;
            }
            for (String sp : spacecraft) {
                out.println(sp);
            }
        } catch (SQLException e) {
            out.println("Error getting moon mission" + e.getMessage());
        }
    }

    private void getMoonMissionId() {
        String missionId = "";
        boolean idInvalid;
        do {
            out.println("Input mission_ID:");
            missionId = sc.nextLine();
            if (!missionId.matches("\\d+")) {
                out.println("Invalid mission id, only numbers (e.g) 8.");
                idInvalid = true;
            } else {
                idInvalid = false;
            }

        } while (idInvalid);
        int intMisson = Integer.parseInt(missionId);

        try {
            var missionDetails = moonMissionRepo.getMission(intMisson);

            if (missionDetails.isEmpty()) {
                out.println("No moon missions found!");
                return;
            }
            if (missionDetails.size() == 7) {
                out.printf("""
                                Mission ID:     %s
                                Spacecraft:     %s
                                Launch date:    %s
                                Carrier Rocket: %s
                                Operator:       %s
                                Mission type:   %s
                                Outcome:        %s
                                """, missionDetails.get(0), missionDetails.get(1), missionDetails.get(2),
                        missionDetails.get(3), missionDetails.get(4), missionDetails.get(5), missionDetails.get(6)
                );
            }else{
                out.println("Unexpected data format for mission.");
            }

        } catch (SQLException e) {
            out.println("Error getting mission details" + e.getMessage());
        }

    }

    private void missionCountYear() {
        String stringYear = "";
        int missionYear = 0;
        boolean invalidYear;
        do {
            out.println("Input mission year:");
            stringYear = sc.nextLine();
            if (stringYear.matches("\\d{4}")) {
                missionYear = Integer.parseInt(stringYear);
                invalidYear = false;
            } else {
                out.println("Input year as YYYY");
                invalidYear = true;
            }

        } while (invalidYear);
        try {
            int missionCount = moonMissionRepo.missionCount(missionYear);

            out.println("Missions count year " + missionYear + ": " + missionCount);
        } catch (SQLException e) {
            out.println("Error getting mission count year" + e.getMessage());
        }
    }

    private void updateAccount() {
        int userId = 0;
        String password = "";
        boolean invalid = true;
        do {
            out.println("User_id to update password: ");
            String stringId = sc.nextLine();
            if (stringId.matches("\\d+")) {
                userId = Integer.parseInt(stringId);
                invalid = false;
            } else {
                out.println("Input user Id number.");
                continue;
            }
            out.println("New password:");
            password = sc.nextLine();
            if (password.isEmpty()) {
                out.println("Password cannot be empty or just whitespace.");
                invalid = true;
            } else {
                break;
            }
        } while (invalid);
        try {
            if (accountRepo.updateAccount(userId, password)) {
                out.println("Account updated!");
            } else {
                out.println("Failed to update account!");
            }
        } catch (SQLException e) {
            out.println("Error updating account" + e.getMessage());
        }
    }

    private void createAccount() {
        boolean inValidInput = true;
        String firstName = "";
        String lastName = "";
        String ssn = "";
        String password = "";
        String name = "";

        do {
            out.println("First name of account.");
            firstName = sc.nextLine();
            if (firstName.length() < 3) {
                out.println("First name cannot be empty or shorter than 3 characters.");
                continue;
            }
            out.println("Last name of account.");
            lastName = sc.nextLine();
            if (lastName.length() < 3) {
                out.println("Last name cannot be empty or shorter than 3 characters.");
                continue;
            }
            out.println("SSN of account.");
            ssn = sc.nextLine();
            if (ssn.length() < 10 && !ssn.matches("^\\d{6}-?\\d{4}$")) {
                out.println("SSN cannot be empty or shorter than 10 characters.");
                continue;
            }
            out.println("Password of account.");
            password = sc.nextLine();

            name = firstName.substring(0, 3) +
                    lastName.substring(0, 3);

            inValidInput = false;

        } while (inValidInput);

        try {
            if (accountRepo.createAccount(firstName, lastName, ssn, password, name)) {
                out.println("Account created!");
            } else {
                out.println("Failed to create account!");
            }
        } catch (SQLException e) {
            out.println("Error creating account" + e.getMessage());
        }
    }

    private void deleteAccount() {
        Integer userId = null;
        out.println("User_id to delete. Warning: There is no undo after this step, type any letter to exit.");
        String stringId = sc.nextLine();
        out.println("StringID: " + stringId);
        if (stringId.matches("\\d+")) {
            userId = Integer.parseInt(stringId);
        } else {
            return;
        }

        try {
            if (accountRepo.deleteAccount(userId)) {
                out.println("Account deleted!");
            } else {
                out.println("Failed to delete account!");
            }
        } catch (SQLException e) {
            out.println("Error deleting account" + e.getMessage());
        }
    }

    /**
     * Determines if the application is running in development mode based on system properties,
     * environment variables, or command-line arguments.
     *
     * @param args an array of command-line arguments
     * @return {@code true} if the application is in development mode; {@code false} otherwise
     */
    private static boolean isDevMode(String[] args) {
        if (Boolean.getBoolean("devMode"))  //Add VM option -DdevMode=true
            return true;
        if ("true".equalsIgnoreCase(getenv("DEV_MODE")))  //Environment variable DEV_MODE=true
            return true;
        for (String arg : args) {
            out.println(arg);
        }
        return Arrays.asList(args).contains("--dev"); //Argument --dev
    }

    /**
     * Reads configuration with precedence: Java system property first, then environment variable.
     * Returns trimmed value or null if neither source provides a non-empty value.
     */
    private static String resolveConfig(String propertyKey, String envKey) {
        String v = getProperty(propertyKey);
        if (v == null || v.trim().isEmpty()) {
            v = getenv(envKey);
        }
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }
}
