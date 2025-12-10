package com.example;


import org.testcontainers.containers.MySQLContainer;
import java.sql.*;

public class DevDatabaseInitializer {
    private static MySQLContainer<?> mysql;

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
