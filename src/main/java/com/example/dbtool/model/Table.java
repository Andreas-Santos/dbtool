package com.example.dbtool.model;

import java.util.List;

public record Table(String name, List<Column> columns) {
}
