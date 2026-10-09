package com.example.dbtool.hotkey;

import com.example.dbtool.groupby.GroupByGenerator;
import com.example.dbtool.groupby.SelectColumnsExtractor;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Orchestrates one hotkey trigger: capture the SQL typed so far in the focused editor,
 * build a GROUP BY clause from its SELECT column list, and paste it directly at the
 * cursor. The whole statement is then reformatted in place, so the query reads in house
 * style right after the clause lands, not just the inserted snippet. The clipboard is
 * restored to whatever it held before the hotkey fired right after pasting, so none of
 * this lingers there. Works the same way whether the hotkey is pressed right after the
 * last JOIN (inserts the full "GROUP BY ..." clause) or after the user already typed
 * "GROUP BY" themselves (inserts just the column list, so it doesn't get duplicated).
 *
 * <p>Reformatting failures are reported without undoing the GROUP BY: by the time it
 * runs, the clause is already in the editor, and a failed reformat (e.g. a content-assist
 * popup disrupted the capture — typing "GROUP BY" is especially prone to triggering one)
 * only means the query is left in its pre-paste shape, not that anything is broken —
 * treating that the same as a real failure would blame the GROUP BY insertion for a
 * problem that is really just cosmetic.
 */
public class GroupByController {

    private static final Pattern GROUP_BY_ALREADY_TYPED_PATTERN = Pattern.compile("(?i)\\bGROUP\\s+BY\\s*$");

    private final EditorAutomation automation = new EditorAutomation();
    private final SelectColumnsExtractor extractor = new SelectColumnsExtractor();
    private final GroupByGenerator generator = new GroupByGenerator();
    private final QueryReformatter reformatter = new QueryReformatter(automation);
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
            // When inserting the full clause (the user hasn't typed "GROUP BY" yet), the
            // cursor sits right after the last JOIN condition with no separator — pasting
            // "GROUP BY ..." straight there glues it onto that condition (e.g.
            // "...CODIGOGROUP BY ..."), and the missing word boundary then makes the
            // reformatter's clause scanner miss "GROUP BY" entirely, swallowing the whole
            // clause into the JOIN's last condition unformatted. A leading newline keeps
            // the keyword on its own token; harmless when columns are being appended after
            // the user's own already-typed "GROUP BY " instead.
            String groupBy = groupByAlreadyTyped
                    ? generator.generateColumnList(selectColumns)
                    : "\n" + generator.generate(selectColumns);

            automation.writeClipboard(groupBy);
            automation.paste();
            onSuccess.accept(reformatAndDescribeResult(originalClipboard));
        } catch (Exception e) {
            automation.writeClipboard(originalClipboard);
            onError.accept(describeError(e, textBeforeCursor));
        }
    }

    private String reformatAndDescribeResult(String originalClipboard) {
        try {
            reformatter.reformatCurrentStatement();
            automation.writeClipboard(originalClipboard);
            return "GROUP BY inserido";
        } catch (Exception reformatError) {
            automation.writeClipboard(originalClipboard);
            return "GROUP BY inserido (formatação não aplicada: " + reformatError.getMessage() + ")";
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
