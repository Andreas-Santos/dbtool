package com.example.dbtool.hotkey;

import com.example.dbtool.format.SqlFormatter;
import com.example.dbtool.format.StatementBounds;

/**
 * Reformats the SQL statement the cursor is currently inside, in place. There's no
 * editor API available to replace an arbitrary text range — only whole-document
 * selection via Ctrl+A — so this rebuilds the entire document around just that one
 * statement (leaving everything else byte-for-byte untouched) and replaces it all in a
 * single select-all + paste.
 *
 * <p>The text after the cursor is derived by substring from a single "select all" copy
 * rather than captured with its own "select to document end" — a second, separate
 * selection was tried first, but a content-assist popup left open by the completion
 * paste that runs right before this (common right after pasting an identifier like
 * "PED.PED_IN_CODIGO") could swallow one of the two selection keystrokes, which made the
 * captured text silently inconsistent and corrupted the paste (the formatted statement
 * followed by a stale, unformatted copy of it). Deriving the "after" half from a single
 * capture — plus the consistency check below — turns that failure mode from silent
 * corruption into a clean, visible error instead.
 *
 * <p>Used both by the standalone "format current query" hotkey and, right after pasting
 * a completion, by the JOIN and GROUP BY hotkeys — so every insertion leaves the whole
 * statement in house style instead of just the snippet that was typed.
 */
public class QueryReformatter {

    private final EditorAutomation automation;
    private final SqlFormatter formatter = new SqlFormatter();

    public QueryReformatter(EditorAutomation automation) {
        this.automation = automation;
    }

    public void reformatCurrentStatement() {
        // A paste just landed in the caller (the JOIN/GROUP BY completion, or nothing
        // yet for the standalone format hotkey) — dismiss any content-assist popup it
        // may have triggered before it gets a chance to swallow the selection below.
        automation.dismissPopup();

        automation.selectToDocumentStart();
        automation.copy();
        String textBeforeCursor = automation.readClipboard();
        automation.dismissPopup();

        automation.selectAll();
        automation.copy();
        String fullDocument = automation.readClipboard();
        automation.dismissPopup();

        if (!fullDocument.startsWith(textBeforeCursor)) {
            throw new IllegalStateException(
                    "Não foi possível localizar a posição do cursor para formatar a query "
                            + "(provavelmente um popup de autocomplete atrapalhou a seleção).");
        }
        String textAfterCursor = fullDocument.substring(textBeforeCursor.length());

        StatementBounds bounds = StatementBounds.locate(textBeforeCursor, textAfterCursor);
        String formattedStatement = formatter.format(bounds.statementText());
        String newDocument = bounds.rebuildDocument(formattedStatement);

        automation.selectAll();
        automation.writeClipboard(newDocument);
        automation.paste();
    }
}
