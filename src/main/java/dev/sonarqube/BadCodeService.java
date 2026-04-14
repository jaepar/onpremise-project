package dev.sonarqube;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class BadCodeService {

    public String getUserName(String input) {
        String password = "root1234"; // 하드코딩된 민감정보
        String result = null;

        try {
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/test",
                    "root",
                    password
            );

            Statement stmt = conn.createStatement();

            String sql = "SELECT name FROM users WHERE id = '" + input + "'"; // SQL 문자열 직접 연결
            System.out.println("실행 SQL: " + sql); // 민감 로그 출력

            stmt.executeQuery(sql);

        } catch (Exception e) {
            // 예외 무시
        }

        if (result == "") { // 문자열 비교 잘못
            return "empty";
        }

        return result;
    }
}