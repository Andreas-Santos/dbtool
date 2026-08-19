package com.example.dbtool.hotkey;

import com.example.dbtool.autocomplete.JoinCompletionService;
import com.example.dbtool.autocomplete.SqlJoinContext;
import com.example.dbtool.autocomplete.SqlJoinContextParser;
import com.example.dbtool.database.MetadataService;

import java.util.function.Consumer;

/**
 * Orchestrates one hotkey trigger: capture the SQL typed so far in the focused editor,
 * resolve the JOIN condition, and leave it on the clipboard for the user to paste with
 * Ctrl+V — this never simulates a paste itself, since that risked overwriting text the
 * user didn't mean to replace.
 */
public class AutocompleteController {

    private final EditorAutomation automation = new EditorAutomation();
    private final SqlJoinContextParser parser = new SqlJoinContextParser();
    private final JoinCompletionService completionService;
    private final Consumer<String> onSuccess;
    private final Consumer<String> onError;

    public AutocompleteController(MetadataService metadataService, Consumer<String> onSuccess,
                                   Consumer<String> onError) {
        this.completionService = new JoinCompletionService(metadataService);
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    public void onHotkeyPressed() {
        automation.releaseHotkeyModifiers();

        String originalClipboard = automation.readClipboard();
        String textBeforeCursor = "";
        try {
            automation.selectToDocumentStart();
            automation.copy();
            textBeforeCursor = automation.readClipboard();
            automation.dismissPopup();
            automation.collapseSelectionForward();

            SqlJoinContext context = parser.parse(textBeforeCursor);
            String completion = completionService.complete(context);

            automation.writeClipboard(completion);
            onSuccess.accept("JOIN pronto — pressione Ctrl+V");
        } catch (Exception e) {
            automation.writeClipboard(originalClipboard);
            onError.accept(describeError(e, textBeforeCursor));
        }
    }

    /**
     * Includes a preview of what was actually captured, since most failures here come
     * from the capture step reading unexpected content rather than from parsing.
     */
    private String describeError(Exception e, String capturedText) {
        String preview = capturedText.isBlank() ? "(vazio)" : capturedText.strip();
        if (preview.length() > 200) {
            preview = "..." + preview.substring(preview.length() - 200);
        }
        return e.getMessage() + "\nTexto capturado: " + preview;
    }
}
