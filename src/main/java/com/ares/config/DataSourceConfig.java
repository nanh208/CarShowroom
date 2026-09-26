package com.ares.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.InetSocketAddress;
import java.net.Socket;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:jdbc:sqlserver://localhost:1433;databaseName=carshop;encrypt=true;trustServerCertificate=true}")
    private String sqlServerUrl;

    @Value("${spring.datasource.username:sa}")
    private String sqlServerUsername;

    @Value("${spring.datasource.password:123}")
    private String sqlServerPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        boolean sqlServerUp = isPortOpen("localhost", 1433, 600);

        if (sqlServerUp) {
            log.info("====> [DATABASE] Đã kết nối thành công tới SQL Server (localhost:1433, database: carshop)");
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(sqlServerUrl);
            ds.setUsername(sqlServerUsername);
            ds.setPassword(sqlServerPassword);
            ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return ds;
        } else {
            log.warn("====> [DATABASE FALLBACK] SQL Server (localhost:1433) chưa bật TCP. Hệ thống tự động kích hoạt H2 In-Memory DB (carshop) để chạy ứng dụng mượt mà không bị crash!");
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl("jdbc:h2:mem:carshop;MODE=MSSQLServer;DB_CLOSE_DELAY=-1");
            ds.setDriverClassName("org.h2.Driver");
            ds.setUsername("sa");
            ds.setPassword("");
            return ds;
        }
    }

    private boolean isPortOpen(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
