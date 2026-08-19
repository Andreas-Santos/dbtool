package com.example.dbtool.hotkey;

import com.example.dbtool.groupby.GroupByGenerator;
import com.example.dbtool.groupby.SelectColumnsExtractor;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Orchestrates one hotkey trigger: capture the SQL typed so far in the focused editor,
 * build a GROUP BY clause from its SELECT column list, and leave it on the clipboard
 * for the user to paste with Ctrl+V. Works the same way whether the hotkey is pressed
 * right after the last JOIN (inserts the full "GROUP BY ..." clause) or after the user
 * already typed "GROUP BY" themselves (inserts just the column list, so it doesn't
 * get duplicated).
 */
public class GroupByController {

    private static final Pattern GROUP_BY_ALREADY_TYPED_PATTERN = Pattern.compile("(?i)\\bGROUP\\s+BY\\s*$");

    private final EditorAutomation automation = new EditorAutomation();
    private final SelectColumnsExtractor extractor = new SelectColumnsExtractor();
    private final GroupByGenerator generator = new GroupByGenerator();
    private final Consumer<String> onSuccess;
    private final Consumer<String> onError;

    public GroupByController(Consumer<String> onSuccess, Consumer<String> onError) {
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    public void onHotkeyPressed() {
        automation.releaseHotkeyModifiers();
        // Typing "GROUP BY" typically triggers DBeaver's content-assist (e.g. suggesting
        // "ORDER BY" as the next clause). Left open, it can swallow the capture keys below
        // instead of the editor receiving them — dismissing it up front, before the
        // selection/copy even starts, avoids ending up with the popup's own text on the
        // clipboard instead of the SQL typed so far.
        automation.dismissPopup();

        String originalClipboard = automation.readClipboard();
        String textBeforeCursor = "";
        try {
            automation.selectToDocumentStart();
            automation.copy();
            textBeforeCursor = automation.readClipboard();
            automation.dismissPopup();
            automation.collapseSelectionForward();

            List<String> selectColumns = extractor.extract(textBeforeCursor);
            boolean groupByAlreadyTyped = GROUP_BY_ALREADY_TYPED_PATTERN.matcher(textBeforeCursor).find();
            String groupBy = groupByAlreadyTyped
                    ? generator.generateColumnList(selectColumns)
                    : generator.generate(selectColumns);

            automation.writeClipboard(groupBy);
            onSuccess.accept("GROUP BY pronto — pressione Ctrl+V");
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
