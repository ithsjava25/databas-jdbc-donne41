package com.example;


import org.testcontainers.containers.MySQLContainer;
import java.sql.*;

public class DevDatabaseInitializer {
    private static MySQLContainer<?> mysql;

    /**
     * Starts a shared MySQL Testcontainers instance (if not already started) and publishes its JDBC URL,
     * username, and password as system properties.
     *
     * <p>If the container does not exist this method creates and starts it; on success it sets the
     * system properties "APP_JDBC_URL", "APP_DB_USER", and "APP_DB_PASS" with the container's connection
     * details. Exceptions during creation or startup are caught and printed to standard output/err.
     */
    public static void start() {
        if (mysql == null) {
            System.out.println("Starting MySQL Container!");
            try {
                mysql = new MySQLContainer<>("mysql:9.5.0")
                        .withDatabaseName("testdb")
                        .withUsername("user")
                        .withPassword("password")
                        .withConfigurationOverride("myconfig")
                        .withInitScript("init.sql");
                System.out.println(mysql.getDatabaseName());
            }catch (Exception e){
                e.printStackTrace();
                return;
            }
            System.out.println("mysql object: " + mysql);
            try {
                mysql.start();
            } catch (Exception e) {
                System.out.println("Error starting mySql: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            System.out.println("setting properties!");
            System.setProperty("APP_JDBC_URL", mysql.getJdbcUrl());
            System.setProperty("APP_DB_USER", mysql.getUsername());
            System.setProperty("APP_DB_PASS", mysql.getPassword());
        }
    }
}
