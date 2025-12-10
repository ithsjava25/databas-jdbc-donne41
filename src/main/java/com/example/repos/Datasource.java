package com.example.repos;

import java.sql.Connection;
import java.sql.SQLException;

public interface Datasource {
    Connection getConnection() throws SQLException;
}
