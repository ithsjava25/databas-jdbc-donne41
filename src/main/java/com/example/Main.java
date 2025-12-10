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


    /**
     * Application entry point that optionally starts development initialization and then launches the interactive application.
     *
     * <p>If the command-line arguments include the flag {@code --dev} the development database initializer is invoked before the application runs.</p>
     *
     * @param args command-line arguments; recognizes {@code --dev} to enable development mode (in addition to equivalent system properties or environment variables)
     */
    static void main(String[] args) {


        if (isDevMode(args)) {
            out.println("Starting dev Mode!");
            DevDatabaseInitializer.start();
        }
        new Main().run();
    }

    /**
     * Initialize database access, repositories, and input scanner, perform database initialization,
     * then enter the interactive login and options loop until the application is exited.
     *
     * <p>Resolves JDBC configuration (system properties then environment variables),
     * constructs the datasource and repository objects, runs DB initialization, and repeatedly
     * prompts for login and presents the options menu while the main loop flag is true.</p>
     *
     * @throws IllegalStateException if any of APP_JDBC_URL, APP_DB_USER, or APP_DB_PASS is not provided
     */
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

    /**
     * Ensures required database tables exist and initializes them from the built-in init data when missing.
     * <p>
     * Attempts to verify and create the application's database tables; when tables were absent or removed,
     * prints a message indicating creation and loading from the init file. On SQL or I/O failures this
     * method prints an error message describing the problem.
     */
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

    /**
     * Prompts the user for username and password, authenticates against the account repository,
     * and updates program state based on user input.
     *
     * <p>The method repeatedly prompts for credentials until a successful login or the user
     * chooses to exit by entering "0" for username or password. On exit it sets {@code run}
     * to {@code false}. On successful authentication it prints a success message and returns
     * to the caller; on failure it prints an invalid-credentials message. SQL errors during
     * authentication are caught and reported via printed messages.
     */
    private void logInPromt() {
        boolean invalidInput = true;
        String userName = "";
        String password = "";
        do {
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
        } while (invalidInput);
    }

    /**
     * Presents an interactive menu, reads user selections, and performs the chosen account or mission actions.
     * <p>
     * Repeatedly displays menu options, prompts for a selection, and dispatches to the corresponding operation:
     * list missions, get mission by ID, count missions by year, create/update/delete accounts, or exit.
     */
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

    /**
     * Prints the names of all spacecraft retrieved from the moon mission repository to standard output.
     * <p>
     * If no missions are found, prints a "No moon missions found!" message. On database errors, prints an error
     * message containing the exception message.
     */
    private void getMoonMission() {
        try {
            var spacecraft = moonMissionRepo.getSpaceCraft();

            if (spacecraft.isEmpty()) {
                out.println("No Spacecrafts found!");
                return;
            }
            for (String sp : spacecraft) {
                out.println(sp);
            }
        } catch (SQLException e) {
            out.println("Error getting moon mission" + e.getMessage());
        }
    }

    /**
     * Prompt for a mission ID, retrieve that mission's details, and print them if found.
     * <p>
     * Prompts the user until a numeric mission ID is entered. Fetches the mission by ID
     * from the moonMission repository and prints a formatted block of mission fields
     * when seven fields are returned; prints a not-found message when no details are available.
     */
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
            } else {
                out.println("Unexpected data format for mission.");
            }
        } catch (SQLException e) {
            out.println("Error getting mission details" + e.getMessage());
        }
    }

    /**
     * Prompts the user for a four-digit year, retrieves the number of moon missions in that year, and prints the result.
     * <p>
     * Repeats input until a valid year in YYYY format is entered, queries the repository for the mission count, and prints
     * either the count or an error message if a database error occurs.
     */
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

    /**
     * Prompts the user for a numeric user ID and a non-empty new password, validates the inputs,
     * and attempts to update the account password in the repository, reporting success or failure.
     *
     * <p>Input is read from the configured scanner; feedback and error messages are printed to the configured output.
     */
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
            if (password.isBlank()) {
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

    /**
     * Prompts the user for account details, validates the input, and attempts to create a new account
     * in the account repository.
     *
     * <p>Validation performed:
     * <ul>
     *   <li>First and last name must be at least 3 characters.</li>
     *   <li>SSN must be at least 10 characters and match the pattern `\d{6}-?\d{4}`.</li>
     * </ul>
     *
     * <p>The account username is constructed by concatenating the first three characters of the
     * first name and the first three characters of the last name. On success or failure the method
     * prints a corresponding message to standard output; SQL errors are caught and their messages
     * are printed.
     */
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
            if (!ssn.matches("^\\d{6}-?\\d{4}$")) {
                out.println("SSN cannot be empty, shorter than 10 characters and only contain numbers with or without '-' after 6 digits.");
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

    /**
     * Prompts for a user ID and attempts to delete the corresponding account.
     * <p>
     * If the entered input is not a numeric user ID the method returns without taking action.
     * On successful deletion prints "Account deleted!", on failure prints "Failed to delete account!".
     * If a SQL error occurs during deletion the error message is printed.
     */
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
