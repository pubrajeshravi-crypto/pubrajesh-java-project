package com.sms.ui;

import com.sms.util.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Search bar panel containing a text field, branch combo, year combo,
 * and a clear button.  Fires {@code ActionListener} callbacks when the
 * user types (with a 300 ms debounce) or changes the combo selections.
 */
public final class SearchBar extends JPanel {

  private final JTextField searchField;
  private final JComboBox<String> branchCombo;
  private final JComboBox<String> yearCombo;
  private final JButton clearButton;

  /** Timer used to debounce keystrokes before triggering a search. */
  private Timer debounceTimer;

  /**
   * Constructs the search bar.
   *
   * @param onSearch callback invoked (on the EDT) after debounce expires
   *                 or when combos change
   */
  public SearchBar(ActionListener onSearch) {
    super(new FlowLayout(FlowLayout.LEFT, Constants.SPACING, 0));
    setBorder(new EmptyBorder(
        Constants.SPACING, Constants.SPACING,
        Constants.SPACING, Constants.SPACING));

    // Search text field
    searchField = new JTextField(22);
    searchField.putClientProperty("JTextField.placeholderText",
        "Search by name or ID…");
    searchField.setFont(searchField.getFont().deriveFont((float) Constants.FONT_SIZE_BODY));

    // Branch combo
    String[] branches = buildBranchItems();
    branchCombo = new JComboBox<>(branches);
    branchCombo.setFont(branchCombo.getFont().deriveFont((float) Constants.FONT_SIZE_BODY));

    // Year combo
    String[] years = {"All Years", "1", "2", "3", "4"};
    yearCombo = new JComboBox<>(years);
    yearCombo.setFont(yearCombo.getFont().deriveFont((float) Constants.FONT_SIZE_BODY));

    // Clear button
    clearButton = new JButton("Clear");
    clearButton.setFont(clearButton.getFont().deriveFont((float) Constants.FONT_SIZE_BODY));

    add(new JLabel("Search:"));
    add(searchField);
    add(new JLabel("Branch:"));
    add(branchCombo);
    add(new JLabel("Year:"));
    add(yearCombo);
    add(clearButton);

    // Debounced keystroke listener
    searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
      @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { schedule(); }
      @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { schedule(); }
      @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { schedule(); }

      private void schedule() {
        if (debounceTimer != null && debounceTimer.isRunning()) debounceTimer.stop();
        debounceTimer = new Timer(Constants.SEARCH_DEBOUNCE_MS, onSearch);
        debounceTimer.setRepeats(false);
        debounceTimer.start();
      }
    });

    branchCombo.addActionListener(onSearch);
    yearCombo.addActionListener(onSearch);

    clearButton.addActionListener(e -> {
      searchField.setText("");
      branchCombo.setSelectedIndex(0);
      yearCombo.setSelectedIndex(0);
      onSearch.actionPerformed(e);
    });
  }

  // --------------------------------------------------------------- public API

  /**
   * @return trimmed text from the search field; empty string if blank
   */
  public String getSearchText() { return searchField.getText().trim(); }

  /**
   * @return selected branch string, or blank if "All Branches" is selected
   */
  public String getSelectedBranch() {
    String sel = (String) branchCombo.getSelectedItem();
    return (sel == null || sel.startsWith("All")) ? "" : sel;
  }

  /**
   * @return selected year (1–4), or {@code 0} if "All Years" is selected
   */
  public int getSelectedYear() {
    String sel = (String) yearCombo.getSelectedItem();
    try {
      return Integer.parseInt(sel);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * Focuses the search text field (called on Ctrl+F).
   */
  public void focusSearchField() {
    searchField.requestFocusInWindow();
    searchField.selectAll();
  }

  // ---------------------------------------------------------------- private

  private static String[] buildBranchItems() {
    String[] items = new String[Constants.BRANCHES.length + 1];
    items[0] = "All Branches";
    System.arraycopy(Constants.BRANCHES, 0, items, 1, Constants.BRANCHES.length);
    return items;
  }
}
