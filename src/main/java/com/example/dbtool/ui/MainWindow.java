package com.example.dbtool.ui;

import com.example.dbtool.database.MetadataService;
import com.example.dbtool.database.MetadataServiceFactory;
import com.example.dbtool.join.JoinGenerator;
import com.example.dbtool.model.ForeignKey;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class MainWindow extends VBox {

    private final TextField mainTableField = new TextField("VEN_PEDIDOVENDA");
    private final TextField relatedTableField = new TextField("VEN_EXPEDICAO");
    private final TextField mainAliasField = new TextField("PED");
    private final TextField relatedAliasField = new TextField("OE");
    private final TextArea resultArea = new TextArea();

    private final JoinGenerator joinGenerator = new JoinGenerator();
    private MetadataService metadataService;

    public MainWindow() {
        setPadding(new Insets(12));
        setSpacing(12);

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.addRow(0, new Label("Tabela principal"), mainTableField);
        form.addRow(1, new Label("Tabela relacionada"), relatedTableField);
        form.addRow(2, new Label("Alias principal"), mainAliasField);
        form.addRow(3, new Label("Alias relacionada"), relatedAliasField);

        Button generateButton = new Button("Gerar JOIN");
        generateButton.setOnAction(e -> generateJoin());

        resultArea.setEditable(false);
        resultArea.setPrefRowCount(8);

        Button copyButton = new Button("Copiar");
        copyButton.setOnAction(e -> copyResult());

        getChildren().addAll(form, generateButton, new Label("Resultado"), resultArea, copyButton);
    }

    private void generateJoin() {
        String mainTable = mainTableField.getText().trim();
        String relatedTable = relatedTableField.getText().trim();
        String mainAlias = mainAliasField.getText().trim();
        String relatedAlias = relatedAliasField.getText().trim();

        try {
            MetadataService service = metadataService();
            List<ForeignKey> relationships = service.findRelationship(mainTable, relatedTable);
            String sql = joinGenerator.generate(relatedTable, relatedAlias, mainAlias, relationships);
            resultArea.setText(sql);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private MetadataService metadataService() {
        if (metadataService == null) {
            metadataService = new MetadataServiceFactory().create();
        }
        return metadataService;
    }

    private void copyResult() {
        ClipboardContent content = new ClipboardContent();
        content.putString(resultArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText("Não foi possível gerar o JOIN");
        alert.showAndWait();
    }
}
