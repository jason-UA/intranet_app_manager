package org.yzr.dropbox;

import java.sql.*;

public enum AppDatabase {

    INSTANCE;

    public static final String URL = "jdbc:mysql://127.0.0.1:3306/app_manager?useUnicode=true&characterEncoding=utf-8";
    public static final String USER = "root";
    public static final String PASSWORD = "123456";
    public static final String CLASSNAME = "com.mysql.cj.jdbc.Driver";

    public Boolean isPackageExisted(String commit) {

        try {
            //1.加载驱动程序
            Class.forName(CLASSNAME);
            //2. 获得数据库连接
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            //3.操作数据库，实现增删改查
            Statement stmt = conn.createStatement();
            String sql = String.format("select * from tb_package where version like '%%%s'", commit) ;
            ResultSet rs = stmt.executeQuery(sql);
            //如果有数据，rs.next()返回true
            return rs.next();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
