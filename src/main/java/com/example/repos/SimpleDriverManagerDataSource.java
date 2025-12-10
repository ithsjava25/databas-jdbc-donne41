package com.example.repos;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class SimpleDriverManagerDataSource implements Datasource {
    String url;
    String user;
    String password;


    /**
     * Create a data source configured with the provided JDBC connection parameters.
     *
     * @param url the JDBC connection URL for the target database
     * @param user the username to use when obtaining connections, or {@code null} if not required
     * @param password the password for the given user, or {@code null} if not required
     */
    public SimpleDriverManagerDataSource(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Obtain a JDBC connection using the configured URL, username, and password.
     *
     * @return a new java.sql.Connection connected to the configured database
     * @throws SQLException if a database access error occurs or the connection cannot be established
     */
    @Override
    public Connection getConnection() throws SQLException{
        return DriverManager.getConnection(url,user,password);
    }



}