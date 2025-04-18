    package com.incogbyte.baiwogen;

    import javax.swing.*;
    import javax.swing.border.EmptyBorder;
    import java.awt.*;
    import java.awt.datatransfer.Clipboard;
    import java.awt.datatransfer.StringSelection;
    import java.awt.event.ActionListener;
    import java.io.File;
    import java.io.FileWriter;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    public class WordlistPanel extends JPanel {
        private final JTabbedPane tabbedPane;
        private final Map<String, JTextArea> textAreas;
        private final JLabel statusLabel;
        private final JLabel contextSizeLabel;
        private final JButton resetButton;
        private final JButton refineButton;

        public WordlistPanel() {
            super(new BorderLayout(10, 10));
            this.setBorder(new EmptyBorder(10, 10, 10, 10));

        
            JPanel footer = new JPanel(new BorderLayout(5, 5));
            statusLabel = new JLabel("Ready");
            contextSizeLabel = new JLabel("Context: 0");
            footer.add(statusLabel, BorderLayout.WEST);
            footer.add(contextSizeLabel, BorderLayout.EAST);

        
            JPanel topControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            resetButton = new JButton("Reset Context");
            refineButton = new JButton("Refine");
            topControls.add(resetButton);
            topControls.add(refineButton);

        
            tabbedPane = new JTabbedPane();
            textAreas = new HashMap<>();
            String[] categories = {"Paths", "Files", "Params", "Headers"};
            for (String cat : categories) {
                JTextArea area = new JTextArea();
                area.setEditable(false);
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                JScrollPane scroll = new JScrollPane(area);
                JPanel panel = new JPanel(new BorderLayout());

        
                JToolBar toolBar = new JToolBar();
                toolBar.setFloatable(false);
                JButton copyBtn = new JButton("Copy");
                JButton saveBtn = new JButton("Save");
                toolBar.add(copyBtn);
                toolBar.add(saveBtn);

        
                copyBtn.addActionListener(e -> copyToClipboard(area.getText()));
                saveBtn.addActionListener(e -> saveToFile(area.getText(), cat));

                panel.add(toolBar, BorderLayout.NORTH);
                panel.add(scroll, BorderLayout.CENTER);

                tabbedPane.addTab(cat, panel);
                textAreas.put(cat, area);
            }

            this.add(topControls, BorderLayout.NORTH);
            this.add(tabbedPane, BorderLayout.CENTER);
            this.add(footer, BorderLayout.SOUTH);
        }

        
        public void updateStatus(String status) {
            SwingUtilities.invokeLater(() -> statusLabel.setText(status));
        }

        
        public void updateContextSize(int size) {
            SwingUtilities.invokeLater(() -> contextSizeLabel.setText("Context: " + size));
        }

        public void setCategoryItems(String category, List<String> items) {
            JTextArea area = textAreas.get(category);
            if (area != null) {
                StringBuilder sb = new StringBuilder();
                for (String item : items) {
                    sb.append(item).append("\n");
                }
                SwingUtilities.invokeLater(() -> area.setText(sb.toString()));
            }
        }


        public void resetAll() {
            textAreas.values().forEach(area ->
                SwingUtilities.invokeLater(() -> area.setText(""))
            );
        }

        
        public void addResetContextListener(ActionListener listener) {
            resetButton.addActionListener(listener);
        }

        
        public void addRefineListener(ActionListener listener) {
            refineButton.addActionListener(listener);
        }


        private void copyToClipboard(String text) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(text), null);
            updateStatus("Copied to clipboard");
        }

        private void saveToFile(String text, String category) {
            try {
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new File(category + "_wordlist.txt"));
                int ret = chooser.showSaveDialog(this);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write(text);
                    }
                    updateStatus("Saved to: " + file.getName());
                }
            } catch (Exception ex) {
                updateStatus("Error saving file: " + ex.getMessage());
            }
        }
    }
