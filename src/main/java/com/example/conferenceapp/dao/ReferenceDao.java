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
