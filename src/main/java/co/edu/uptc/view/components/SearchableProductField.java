package co.edu.uptc.view.components;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import co.edu.uptc.models.products.Product;
import co.edu.uptc.view.GlobalView;

public class SearchableProductField extends JTextField {

    private final JPopupMenu suggestionsPopup;
    private final JList<Product> suggestionsList;
    private final DefaultListModel<Product> listModel;
    private final List<Product> productCatalog;
    private Product selectedProduct;

    private boolean isAdjusting = false;
    private final Timer searchTimer;

    public SearchableProductField(List<Product> catalog) {
        super();
        this.productCatalog = new ArrayList<>(catalog);
        setFont(new Font("Segoe UI", Font.PLAIN, 16));
        setBackground(new Color(245, 245, 245));
        setBorder(new CompoundBorder(
                new LineBorder(new Color(180, 180, 180), 1, true),
                new EmptyBorder(8, 12, 8, 12)));

        suggestionsPopup = new JPopupMenu();
        suggestionsPopup.setFocusable(false);
        suggestionsPopup.setBorder(new LineBorder(GlobalView.BORDER_COLOR, 1));

        listModel = new DefaultListModel<>();
        suggestionsList = new JList<>(listModel);
        suggestionsList.setCellRenderer(new ProductListCellRenderer());
        suggestionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionsList.setBackground(Color.WHITE);
        suggestionsList.setFocusable(false);
        suggestionsList.setAutoscrolls(true);

        JScrollPane scrollPane = new JScrollPane(suggestionsList);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(300, 150));
        suggestionsPopup.add(scrollPane);

        searchTimer = new Timer(300, e -> SwingUtilities.invokeLater(this::performSearch));
        searchTimer.setRepeats(false);

        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!isAdjusting)
                    restartTimer();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!isAdjusting)
                    restartTimer();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!isAdjusting)
                    restartTimer();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!suggestionsPopup.isVisible() || listModel.isEmpty()) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        performSearch();
                    }
                    return;
                }

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        int next = Math.min(suggestionsList.getSelectedIndex() + 1, listModel.getSize() - 1);
                        suggestionsList.setSelectedIndex(next);
                        suggestionsList.ensureIndexIsVisible(next);
                        e.consume();
                        break;
                    case KeyEvent.VK_UP:
                        int prev = Math.max(suggestionsList.getSelectedIndex() - 1, 0);
                        suggestionsList.setSelectedIndex(prev);
                        suggestionsList.ensureIndexIsVisible(prev);
                        e.consume();
                        break;
                    case KeyEvent.VK_ENTER:
                        selectItemFromList();
                        e.consume();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        suggestionsPopup.setVisible(false);
                        break;
                }
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                setBorder(new CompoundBorder(
                        new LineBorder(GlobalView.ASIDE_BACKGROUND, 2, true),
                        new EmptyBorder(7, 11, 7, 11)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                setBorder(new CompoundBorder(
                        new LineBorder(new Color(180, 180, 180), 1, true),
                        new EmptyBorder(8, 12, 8, 12)));

                Timer hideTimer = new Timer(150, ae -> {
                    if (!suggestionsPopup.isFocusOwner()) {
                        suggestionsPopup.setVisible(false);
                        validateSelection();
                    }
                });
                hideTimer.setRepeats(false);
                hideTimer.start();
            }
        });

        suggestionsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectItemFromList();
            }
        });
    }

    private void restartTimer() {
        searchTimer.restart();
    }

    private void performSearch() {
        String query = getText().toLowerCase().trim();
        if (query.isEmpty()) {
            listModel.clear();
            suggestionsPopup.setVisible(false);
            selectedProduct = null;
            return;
        }

        List<Product> matches = productCatalog.stream()
                .filter(p -> p.getName().toLowerCase().contains(query) ||
                        p.getBrand().toLowerCase().contains(query) ||
                        p.getId().toLowerCase().startsWith(query))
                .limit(10)
                .collect(Collectors.toList());

        listModel.clear();
        matches.forEach(listModel::addElement);

        if (matches.isEmpty()) {
            suggestionsPopup.setVisible(false);
            selectedProduct = null;
        } else {
            suggestionsPopup.setPopupSize(getWidth(), suggestionsPopup.getPreferredSize().height);
            if (!suggestionsPopup.isVisible() && isFocusOwner()) {
                suggestionsPopup.show(this, 0, getHeight());
            }
        }
    }

    private void selectItemFromList() {
        Product selected = suggestionsList.getSelectedValue();
        if (selected != null) {
            isAdjusting = true;
            this.selectedProduct = selected;
            setText(selected.getName() + " (" + selected.getBrand() + ")");
            suggestionsPopup.setVisible(false);
            isAdjusting = false;
        }
    }

    private void validateSelection() {
        if (selectedProduct != null
                && !getText().equals(selectedProduct.getName() + " (" + selectedProduct.getBrand() + ")")) {
            selectedProduct = null;
        }
        if (getText().trim().isEmpty()) {
            selectedProduct = null;
        }
    }

    public Product getSelectedProduct() {
        validateSelection();
        return this.selectedProduct;
    }

    public void clearSelection() {
        isAdjusting = true;
        setText("");
        this.selectedProduct = null;
        listModel.clear();
        suggestionsPopup.setVisible(false);
        isAdjusting = false;
    }

    private class ProductListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Product) {
                Product product = (Product) value;
                setText(product.getName() + " (" + product.getBrand() + ")");
                setBorder(new EmptyBorder(5, 5, 5, 5));
            }
            return c;
        }
    }
}