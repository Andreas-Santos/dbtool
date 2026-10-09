package com.example.dbtool.hotkey;

import com.example.dbtool.autocomplete.JoinCompletionService;
import com.example.dbtool.autocomplete.SqlJoinContext;
import com.example.dbtool.autocomplete.SqlJoinContextParser;
import com.example.dbtool.database.MetadataService;

import java.util.function.Consumer;

/**
 * Orchestrates one hotkey trigger: capture the SQL typed so far in the focused editor,
 * resolve the JOIN condition, and paste it directly at the cursor — the selection was
 * already collapsed back to that exact spot by {@link EditorAutomation#collapseSelectionForward()},
 * so the paste can't land anywhere else. The clipboard is restored to whatever it held
 * before the hotkey fired right after pasting, so the completion doesn't linger there.
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
            automation.paste();
            automation.writeClipboard(originalClipboard);
            onSuccess.accept("JOIN inserido");
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
