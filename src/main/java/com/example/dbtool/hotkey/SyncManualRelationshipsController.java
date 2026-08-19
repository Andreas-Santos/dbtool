package com.example.dbtool.hotkey;

import com.example.dbtool.autocomplete.ExtractedJoinRelationship;
import com.example.dbtool.autocomplete.SqlJoinExtractor;
import com.example.dbtool.database.ManualRelationshipRepository;

import java.util.List;
import java.util.function.Consumer;

/**
 * Scans the whole SQL in the focused editor for JOIN ... ON clauses that are already
 * fully written, and adds any relationship not yet present in the manual relationships
 * file — an easy way to feed that file from queries the user already got working.
 */
public class SyncManualRelationshipsController {

    private final EditorAutomation automation = new EditorAutomation();
    private final SqlJoinExtractor extractor = new SqlJoinExtractor();
    private final ManualRelationshipRepository manualRelationships;
    private final Consumer<String> onSuccess;
    private final Consumer<String> onError;

    public SyncManualRelationshipsController(ManualRelationshipRepository manualRelationships,
                                              Consumer<String> onSuccess, Consumer<String> onError) {
        this.manualRelationships = manualRelationships;
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    public void onHotkeyPressed() {
        automation.releaseHotkeyModifiers();

        String originalClipboard = automation.readClipboard();
        try {
            automation.selectAll();
            automation.copy();
            String fullText = automation.readClipboard();
            automation.moveToDocumentEnd();

            List<ExtractedJoinRelationship> relationships = extractor.extractAll(fullText);
            int added = 0;
            for (ExtractedJoinRelationship relationship : relationships) {
                boolean wasAdded = manualRelationships.addIfMissing(
                        relationship.sourceTable(), relationship.joinedTable(), relationship.columns());
                if (wasAdded) {
                    added++;
                }
            }

            onSuccess.accept(added == 0
                    ? "Nenhum relacionamento novo encontrado"
                    : added + " relacionamento(s) adicionados ao arquivo manual");
        } catch (Exception e) {
            onError.accept(e.getMessage());
        } finally {
            automation.writeClipboard(originalClipboard);
        }
    }
}
