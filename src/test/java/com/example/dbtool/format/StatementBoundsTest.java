package com.example.dbtool.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementBoundsTest {

    @Test
    void shouldTreatWholeBufferAsOneStatementWhenNoSemicolonsArePresent() {
        StatementBounds bounds = StatementBounds.locate("SELECT A.ID ", "FROM T A");

        assertEquals("", bounds.prefix());
        assertEquals("SELECT A.ID FROM T A", bounds.statementText());
        assertEquals("", bounds.suffix());
    }

    @Test
    void shouldBoundStatementByNearestSemicolonsOnEachSide() {
        StatementBounds bounds = StatementBounds.locate(
                "SELECT 1 FROM DUAL;\nSELECT A.ID ", "FROM T A;\nSELECT 2 FROM DUAL;");

        assertEquals("SELECT 1 FROM DUAL;", bounds.prefix());
        assertEquals("\nSELECT A.ID FROM T A", bounds.statementText());
        assertEquals(";\nSELECT 2 FROM DUAL;", bounds.suffix());
    }

    @Test
    void rebuildDocumentShouldInsertNewlineAfterANonEmptyPrefix() {
        StatementBounds bounds = new StatementBounds("SELECT 1 FROM DUAL;", "SELECT A.ID FROM T A", "");

        assertEquals("SELECT 1 FROM DUAL;\nFORMATTED", bounds.rebuildDocument("FORMATTED"));
    }

    @Test
    void rebuildDocumentShouldNotInsertLeadingNewlineWhenThereIsNoPrefix() {
        StatementBounds bounds = new StatementBounds("", "SELECT A.ID FROM T A", ";\nSELECT 2 FROM DUAL;");

        assertEquals("FORMATTED;\nSELECT 2 FROM DUAL;", bounds.rebuildDocument("FORMATTED"));
    }
}
