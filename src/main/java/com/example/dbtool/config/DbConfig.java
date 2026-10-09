package com.example.dbtool.config;

/**
 * @param owner the schema owner used to qualify FROM/JOIN tables when formatting a
 *              query (see {@code SqlFormatter}); blank leaves table references
 *              unqualified.
 */
public record DbConfig(String host, String port, String service, String username, String password, String owner) {

    public String jdbcUrl() {
        return "jdbc:oracle:thin:@//" + host + ":" + port + "/" + service;
    }
}
