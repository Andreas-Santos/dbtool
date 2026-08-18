package com.example.dbtool.config;

public record DbConfig(String host, String port, String service, String username, String password) {

    public String jdbcUrl() {
        return "jdbc:oracle:thin:@//" + host + ":" + port + "/" + service;
    }
}
