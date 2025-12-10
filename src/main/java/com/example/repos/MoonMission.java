package com.example.repos;

import org.testcontainers.shaded.org.checkerframework.common.returnsreceiver.qual.This;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MoonMission {
    private final Datasource dataSource;

    public MoonMission(Datasource dataSource) {
        this.dataSource = dataSource;
    }
    /**
     * Retrieve the spacecraft names from the `moon_mission` table.
     *
     * @return a list of spacecraft names; empty if no rows are found
     * @throws SQLException if a database access error occurs
     */
    public List<String> getSpaceCraft() throws SQLException {
        String sql = "select spacecraft from moon_mission";
        List<String> spaceCrafts = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                spaceCrafts.add(rs.getString("spacecraft"));
            }
        }
        return spaceCrafts;
    }
    /**
     * Retrieve all column values for the mission with the given mission_id in column order.
     *
     * @param id the mission_id to query
     * @return a list of column values (as strings) for each matching row in column order; empty if no match
     */
    public List<String> getMission(int id) throws SQLException {
        String sql = "select * from moon_mission where mission_id = ?";
        List<String> missionDetails = new ArrayList<>();

        try (Connection con = dataSource.getConnection();
             PreparedStatement md = con.prepareStatement(sql)) {

            md.setInt(1, id);

            try (ResultSet rs = md.executeQuery()) {
                ResultSetMetaData rsmd = rs.getMetaData();

                while (rs.next()) {
                    for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                        missionDetails.add(rs.getString(i));
                    }
                }
            }
        }
        return missionDetails;
    }
    /**
     * Count missions launched in the specified year.
     *
     * @param year the launch year to count missions for
     * @return the number of missions launched in the specified year, or 0 if none
     */
    public int missionCount(int year) throws SQLException {
        String sql = "select count(*) as missionCount, year(launch_date) as launchyear" +
                " from moon_mission where year(launch_date) = ? group by launchyear;";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, year);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }


}
