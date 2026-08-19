package com.example.dbtool.ui;

import com.example.dbtool.config.ConfigLoader;
import com.example.dbtool.config.DbConfig;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.function.Consumer;

/**
 * Lets the user type in the Oracle connection details instead of hand-editing
 * config/db.properties. Opened on first run (no settings found yet) and from the tray
 * menu afterwards to change them.
 */
public class DbConfigWindow extends JFrame {

    private final ConfigLoader configLoader = new ConfigLoader();
    private final Consumer<DbConfig> onSaved;

    private final JTextField hostField = new JTextField();
    private final JTextField portField = new JTextField();
    private final JTextField serviceField = new JTextField();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel statusLabel = new JLabel(" ");

    public DbConfigWindow(Consumer<DbConfig> onSaved) {
        super("DB Tool — Conexão com o banco");
        this.onSaved = onSaved;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setIconImage(AppIcon.load());

        DbConfig existing = configLoader.tryLoad();
        if (existing != null) {
            hostField.setText(existing.host());
            portField.setText(existing.port());
            serviceField.setText(existing.service());
            usernameField.setText(existing.username());
            passwordField.setText(existing.password());
        } else {
            portField.setText("1521");
        }

        setContentPane(buildContent());
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildContent() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, c, 0, "Host", hostField);
        addRow(form, c, 1, "Porta", portField);
        addRow(form, c, 2, "Serviço/SID", serviceField);
        addRow(form, c, 3, "Usuário", usernameField);
        addRow(form, c, 4, "Senha", passwordField);

        JTextField[] fields = {hostField, portField, serviceField, usernameField};
        for (JTextField field : fields) {
            field.setColumns(20);
        }
        passwordField.setColumns(20);

        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        form.add(statusLabel, c);

        JButton testButton = new JButton("Testar conexão");
        testButton.addActionListener(e -> testConnection());

        JButton saveButton = new JButton("Salvar");
        saveButton.addActionListener(e -> save());

        JPanel buttons = new JPanel();
        buttons.add(testButton);
        buttons.add(saveButton);

        JPanel content = new JPanel(new BorderLayout());
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        return content;
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String label, JTextField field) {
        c.gridy = row;
        c.gridx = 0;
        c.weightx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(field, c);
    }

    private DbConfig readForm() {
        return new DbConfig(
                hostField.getText().strip(),
                portField.getText().strip(),
                serviceField.getText().strip(),
                usernameField.getText().strip(),
                new String(passwordField.getPassword()));
    }

    private boolean isFilledIn(DbConfig config) {
        return !config.host().isBlank() && !config.port().isBlank() && !config.service().isBlank()
                && !config.username().isBlank() && !config.password().isBlank();
    }

    private void save() {
        DbConfig config = readForm();
        if (!isFilledIn(config)) {
            setStatus("Preencha todos os campos.", true);
            return;
        }
        configLoader.save(config);
        onSaved.accept(config);
        dispose();
    }

    private void testConnection() {
        DbConfig config = readForm();
        if (!isFilledIn(config)) {
            setStatus("Preencha todos os campos.", true);
            return;
        }
        setStatus("Testando...", false);

        new SwingWorker<Void, Void>() {
            private Exception failure;

            @Override
            protected Void doInBackground() {
                try (Connection connection = DriverManager.getConnection(
                        config.jdbcUrl(), config.username(), config.password())) {
                    connection.isValid(5);
                } catch (Exception e) {
                    failure = e;
                }
                return null;
            }

            @Override
            protected void done() {
                if (failure == null) {
                    setStatus("Conexão bem-sucedida!", false);
                } else {
                    setStatus("Falha: " + failure.getMessage(), true);
                }
            }
        }.execute();
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? Color.RED : new Color(0x1B, 0x7A, 0x1B));
    }

    public static void showOnEventThread(Consumer<DbConfig> onSaved) {
        SwingUtilities.invokeLater(() -> new DbConfigWindow(onSaved).setVisible(true));
    }
}
