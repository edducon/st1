package com.example.conferenceapp.dao;

import com.example.conferenceapp.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReferenceDao {

    public List<String> findAllDirections(){
        return simple("SELECT name FROM direction ORDER BY name");
    }
    public List<String> findAllCountries(){
        return simple("SELECT name_ru FROM country ORDER BY name_ru");
    }
    public List<String> findAllCities(){
        return simple("SELECT name FROM city ORDER BY name");
    }

    public int ensureDirection(String name){
        String sql = "SELECT id FROM direction WHERE name = ?";
        try(Connection c = DBUtil.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)){
            ps.setString(1, name);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) return rs.getInt(1);
            }
        }catch(SQLException e){ e.printStackTrace(); }

        String insert = "INSERT INTO direction(name) VALUES(?)";
        try(Connection c = DBUtil.getConnection();
            PreparedStatement ps = c.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1, name);
            ps.executeUpdate();
            try(ResultSet rs = ps.getGeneratedKeys()){
                if(rs.next()) return rs.getInt(1);
            }
        }catch(SQLException e){ e.printStackTrace(); }
        return 0;
    }

    public int ensureCity(String name){
        String sql = "SELECT id FROM city WHERE name = ?";
        try(Connection c = DBUtil.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)){
            ps.setString(1, name);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) return rs.getInt(1);
            }
        }catch(SQLException e){ e.printStackTrace(); }

        int countryId = resolveDefaultCountry();
        String insert = "INSERT INTO city(name, country_id) VALUES(?, ?)";
        try(Connection c = DBUtil.getConnection();
            PreparedStatement ps = c.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1, name);
            ps.setInt(2, countryId);
            ps.executeUpdate();
            try(ResultSet rs = ps.getGeneratedKeys()){
                if(rs.next()) return rs.getInt(1);
            }
        }catch(SQLException e){ e.printStackTrace(); }
        return 0;
    }

    private int resolveDefaultCountry(){
        String sql = "SELECT id FROM country ORDER BY id LIMIT 1";
        try(Connection c = DBUtil.getConnection();
            Statement st = c.createStatement();
            ResultSet rs = st.executeQuery(sql)){
            if(rs.next()) return rs.getInt(1);
        }catch(SQLException e){ e.printStackTrace(); }
        return 1;
    }
    /* -------- utils ------ */
    private List<String> simple(String sql){
        List<String> list = new ArrayList<>();
        try(Connection c = DBUtil.getConnection();
            Statement st = c.createStatement();
            ResultSet rs = st.executeQuery(sql)){
            while(rs.next()) list.add(rs.getString(1));
        }catch(SQLException e){ e.printStackTrace(); }
        return list;
    }
}
