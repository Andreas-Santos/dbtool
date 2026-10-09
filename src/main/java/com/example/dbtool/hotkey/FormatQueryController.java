package com.example.dbtool.hotkey;

import java.util.function.Consumer;

/**
 * Orchestrates the standalone "format current query" hotkey: locate the statement the
 * cursor is in and rewrite it in house style, without needing a JOIN or GROUP BY
 * completion to trigger it first. Shares {@link QueryReformatter} with
 * {@link AutocompleteController} and {@link GroupByController}, which call the same
 * reformat right after pasting their own completion.
 */
public class FormatQueryController {

    private final EditorAutomation automation = new EditorAutomation();
    private final QueryReformatter reformatter;
    private final Consumer<String> onSuccess;
    private final Consumer<String> onError;

    public FormatQueryController(String owner, Consumer<String> onSuccess, Consumer<String> onError) {
        this.reformatter = new QueryReformatter(automation, owner);
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    public void onHotkeyPressed() {
        automation.releaseHotkeyModifiers();
        automation.dismissPopup();

        String originalClipboard = automation.readClipboard();
        try {
            reformatter.reformatCurrentStatement();
            automation.writeClipboard(originalClipboard);
            onSuccess.accept("Query formatada");
        } catch (Exception e) {
            automation.writeClipboard(originalClipboard);
            onError.accept(e.getMessage());
        }
    }
}
