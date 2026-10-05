package com.sms.ui;

import com.sms.exception.DuplicateStudentException;
import com.sms.exception.ValidationException;
import com.sms.model.Student;
import com.sms.service.StudentService;
import com.sms.util.Constants;
import com.sms.util.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Modal dialog for adding a new student or editing an existing one.
 *
 * <p>In <em>add</em> mode, all fields are blank and {@code student_id} is editable.
 * In <em>edit</em> mode, the dialog is pre-filled with the student's data and
 * {@code student_id} is read-only.
 *
 * <p>Field-level inline validation error labels appear below each field
 * when {@code Save} is clicked.
 */
public final class StudentFormDialog extends JDialog {

  /** {@code true} if the user confirmed Save without validation errors. */
  private boolean saved = false;

  private final StudentService service;

  // Form fields
  private JTextField idField;
  private JTextField nameField;
  private JComboBox<String> branchCombo;
  private final ButtonGroup yearGroup = new ButtonGroup();
  private final JRadioButton[] yearButtons = new JRadioButton[4];
  private JTextField phoneField;
  private JTextField marksField;

  // Inline error labels (field name → label)
  private final Map<String, JLabel> errorLabels = new HashMap<>();

  // The student being edited (null in add mode)
  private final Student existingStudent;

  // ------------------------------------------------------------- constructors

  /**
   * Opens the dialog in <em>add</em> mode.
   *
   * @param owner   parent frame
   * @param service service layer for persistence
   */
  public StudentFormDialog(Frame owner, StudentService service) {
    this(owner, service, null);
  }

  /**
   * Opens the dialog in <em>edit</em> mode, pre-filling all fields.
   *
   * @param owner    parent frame
   * @param service  service layer for persistence
   * @param student  student to edit; must not be {@code null}
   */
  public StudentFormDialog(Frame owner, StudentService service, Student student) {
    super(owner, student == null ? "Add Student" : "Edit Student — " + student.getStudentId(),
        true);
    this.service        = service;
    this.existingStudent = student;
    buildUI();
    if (student != null) prefill(student);
    pack();
    setSize(Constants.DIALOG_WIDTH, Constants.DIALOG_HEIGHT);
    setResizable(false);
    setLocationRelativeTo(owner);
    installKeyBindings();
  }

  // --------------------------------------------------------------- public API

  /**
   * @return {@code true} if the user saved the form without errors
   */
  public boolean isSaved() { return saved; }

  // ----------------------------------------------------------------- private

  private void buildUI() {
    JPanel root = new JPanel(new BorderLayout(0, 0));
    root.setBorder(new EmptyBorder(
        Constants.SPACING * 2, Constants.SPACING * 3,
        Constants.SPACING * 2, Constants.SPACING * 3));

    root.add(buildFormPanel(), BorderLayout.CENTER);
    root.add(buildButtonPanel(), BorderLayout.SOUTH);

    setContentPane(root);
  }

  private JPanel buildFormPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = defaultGbc();

    // Student ID
    idField = new JTextField(18);
    idField.putClientProperty("JTextField.placeholderText", "e.g. CS2021001");
    if (existingStudent != null) idField.setEditable(false);
    addFormRow(panel, gbc, "Student ID *", idField, "studentId");

    // Name
    nameField = new JTextField(18);
    nameField.putClientProperty("JTextField.placeholderText", "Full name");
    addFormRow(panel, gbc, "Full Name *", nameField, "name");

    // Branch
    branchCombo = new JComboBox<>(Constants.BRANCHES);
    addFormRow(panel, gbc, "Branch *", branchCombo, "branch");

    // Year (radio buttons)
    JPanel yearPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    yearPanel.setOpaque(false);
    for (int i = 0; i < 4; i++) {
      yearButtons[i] = new JRadioButton(String.valueOf(i + 1));
      yearButtons[i].setFont(yearButtons[i].getFont().deriveFont((float) Constants.FONT_SIZE_BODY));
      yearGroup.add(yearButtons[i]);
      yearPanel.add(yearButtons[i]);
    }
    yearButtons[0].setSelected(true);
    addFormRow(panel, gbc, "Year *", yearPanel, "year");

    // Phone
    phoneField = new JTextField(18);
    phoneField.putClientProperty("JTextField.placeholderText", "10-digit number");
    addFormRow(panel, gbc, "Phone *", phoneField, "phone");

    // Marks
    marksField = new JTextField(18);
    marksField.putClientProperty("JTextField.placeholderText", "0.00 – 100.00");
    addFormRow(panel, gbc, "Marks (%) *", marksField, "marks");

    return panel;
  }

  private void addFormRow(JPanel panel, GridBagConstraints gbc,
                          String labelText, JComponent field, String fieldKey) {
    // Label
    gbc.gridx = 0; gbc.gridy++;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill   = GridBagConstraints.NONE;
    gbc.insets = new Insets(Constants.SPACING, 0, 2, Constants.SPACING);
    JLabel lbl = new JLabel(labelText);
    lbl.setFont(lbl.getFont().deriveFont((float) Constants.FONT_SIZE_BODY));
    panel.add(lbl, gbc);

    // Field
    gbc.gridx = 1;
    gbc.fill  = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets  = new Insets(Constants.SPACING, 0, 2, 0);
    field.setFont(field.getFont() != null
        ? field.getFont().deriveFont((float) Constants.FONT_SIZE_BODY)
        : new Font(Font.SANS_SERIF, Font.PLAIN, Constants.FONT_SIZE_BODY));
    panel.add(field, gbc);

    // Error label
    gbc.gridx = 1; gbc.gridy++;
    gbc.fill   = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(0, 0, 4, 0);
    JLabel errLabel = new JLabel(" ");
    errLabel.setForeground(Color.decode(Constants.COLOR_ERROR));
    errLabel.setFont(errLabel.getFont().deriveFont(Font.PLAIN, 11f));
    errorLabels.put(fieldKey, errLabel);
    panel.add(errLabel, gbc);

    gbc.weightx = 0;
  }

  private JPanel buildButtonPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, Constants.SPACING, Constants.SPACING));
    panel.setBorder(new EmptyBorder(Constants.SPACING, 0, 0, 0));

    JButton cancelBtn = new JButton("Cancel");
    cancelBtn.setFont(cancelBtn.getFont().deriveFont((float) Constants.FONT_SIZE_BODY));
    cancelBtn.addActionListener(e -> dispose());

    JButton saveBtn = new JButton("Save");
    saveBtn.setFont(saveBtn.getFont().deriveFont(Font.BOLD, Constants.FONT_SIZE_BODY));
    saveBtn.addActionListener(e -> handleSave());

    panel.add(cancelBtn);
    panel.add(saveBtn);

    getRootPane().setDefaultButton(saveBtn);
    return panel;
  }

  private void installKeyBindings() {
    // Escape closes the dialog
    getRootPane().registerKeyboardAction(
        e -> dispose(),
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW);
  }

  // --------------------------------------------------------------- save logic

  private void handleSave() {
    clearErrors();
    String id        = idField.getText().trim();
    String name      = nameField.getText().trim();
    String branch    = (String) branchCombo.getSelectedItem();
    int    year      = getSelectedYear();
    String phone     = phoneField.getText().trim();
    String marksText = marksField.getText().trim();

    try {
      if (existingStudent == null) {
        service.addStudent(id, name, branch, year, phone, marksText);
      } else {
        service.updateStudent(id, name, branch, year, phone, marksText);
      }
      saved = true;
      dispose();

    } catch (ValidationException ex) {
      showFieldErrors(ex.getFieldErrors());

    } catch (DuplicateStudentException ex) {
      String field = ex.getConflictField().equals("student_id") ? "studentId" : "phone";
      showFieldError(field, "Value '" + ex.getConflictValue() + "' is already in use.");

    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this,
          "Unexpected error: " + ex.getMessage(),
          "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void showFieldErrors(Map<String, String> errors) {
    errors.forEach(this::showFieldError);
  }

  private void showFieldError(String field, String message) {
    JLabel lbl = errorLabels.get(field);
    if (lbl != null) lbl.setText(message);
  }

  private void clearErrors() {
    errorLabels.values().forEach(l -> l.setText(" "));
  }

  // --------------------------------------------------------------- helpers

  private void prefill(Student s) {
    idField.setText(s.getStudentId());
    nameField.setText(s.getName());
    branchCombo.setSelectedItem(s.getBranch());
    int yr = s.getYear();
    if (yr >= 1 && yr <= 4) yearButtons[yr - 1].setSelected(true);
    phoneField.setText(s.getPhone());
    marksField.setText(s.getMarks().toPlainString());
  }

  private int getSelectedYear() {
    for (int i = 0; i < yearButtons.length; i++) {
      if (yearButtons[i].isSelected()) return i + 1;
    }
    return 0;
  }

  private static GridBagConstraints defaultGbc() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridy   = -1;
    gbc.anchor  = GridBagConstraints.WEST;
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0;
    return gbc;
  }
}
