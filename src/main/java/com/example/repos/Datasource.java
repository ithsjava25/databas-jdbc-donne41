package com.example.repos;

import java.sql.Connection;
import java.sql.SQLException;

public interface Datasource {
    /**
 * Obtain a JDBC connection to the configured datasource.
 *
 * @return a JDBC {@link java.sql.Connection} connected to the datasource
 * @throws java.sql.SQLException if a database access error occurs or a connection cannot be established
 */
Connection getConnection() throws SQLException;
}