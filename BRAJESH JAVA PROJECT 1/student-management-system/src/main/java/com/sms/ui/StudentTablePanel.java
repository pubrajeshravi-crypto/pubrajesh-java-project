package com.sms.ui;

import com.sms.model.Student;
import com.sms.util.Constants;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel containing a sortable, striped student table.
 *
 * <p>Marks column is right-aligned; Year column is centred; all other columns
 * are left-aligned.  Row striping alternates between white and
 * {@code COLOR_STRIPE}.
 */
public final class StudentTablePanel extends JPanel {

  private final StudentTableModel tableModel;
  private final JTable            table;
  private final TableRowSorter<StudentTableModel> sorter;

  /** Creates the panel with an empty table. */
  public StudentTablePanel() {
    super(new BorderLayout());
    tableModel = new StudentTableModel();
    table      = buildTable();
    sorter     = new TableRowSorter<>(tableModel);
    table.setRowSorter(sorter);

    JScrollPane scroll = new JScrollPane(table);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getViewport().setBackground(Color.WHITE);
    add(scroll, BorderLayout.CENTER);
  }

  // --------------------------------------------------------------- public API

  /**
   * Replaces the current table contents with {@code students}.
   * Must be called on the Event Dispatch Thread.
   *
   * @param students new list to display; must not be {@code null}
   */
  public void setStudents(List<Student> students) {
    tableModel.setStudents(students);
  }

  /**
   * Returns the {@link Student} currently selected in the table, or
   * {@code null} if no row is selected.
   */
  public Student getSelectedStudent() {
    int viewRow = table.getSelectedRow();
    if (viewRow < 0) return null;
    int modelRow = table.convertRowIndexToModel(viewRow);
    return tableModel.getStudentAt(modelRow);
  }

  /**
   * Returns the underlying {@link JTable} for listener registration
   * (e.g., {@code ListSelectionListener}).
   */
  public JTable getTable() { return table; }

  /**
   * Returns the current list of displayed students (model order).
   */
  public List<Student> getDisplayedStudents() {
    return tableModel.getStudents();
  }

  // ---------------------------------------------------------------- private

  private JTable buildTable() {
    JTable t = new JTable(tableModel) {
      @Override
      public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int col) {
        Component c = super.prepareRenderer(renderer, row, col);
        if (!isRowSelected(row)) {
          c.setBackground(row % 2 == 0
              ? Color.WHITE
              : Color.decode(Constants.COLOR_STRIPE));
        }
        return c;
      }
    };

    t.setRowHeight(26);
    t.setShowHorizontalLines(true);
    t.setShowVerticalLines(false);
    t.setGridColor(new Color(0xE0E0E0));
    t.setFont(t.getFont().deriveFont((float) Constants.FONT_SIZE_BODY));
    t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    t.setFillsViewportHeight(true);
    t.getTableHeader().setReorderingAllowed(false);
    t.getTableHeader().setFont(
        t.getFont().deriveFont(Font.BOLD, Constants.FONT_SIZE_BODY));

    // Column widths
    setColumnWidth(t, Constants.COL_ID,     130);
    setColumnWidth(t, Constants.COL_NAME,   180);
    setColumnWidth(t, Constants.COL_BRANCH,  80);
    setColumnWidth(t, Constants.COL_YEAR,    55);
    setColumnWidth(t, Constants.COL_PHONE,  120);
    setColumnWidth(t, Constants.COL_MARKS,   90);

    // Right-align marks
    DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
    rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
    t.getColumnModel().getColumn(Constants.COL_MARKS).setCellRenderer(rightRenderer);

    // Centre-align year
    DefaultTableCellRenderer centreRenderer = new DefaultTableCellRenderer();
    centreRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    t.getColumnModel().getColumn(Constants.COL_YEAR).setCellRenderer(centreRenderer);

    return t;
  }

  private static void setColumnWidth(JTable t, int col, int width) {
    TableColumn tc = t.getColumnModel().getColumn(col);
    tc.setPreferredWidth(width);
    tc.setMinWidth(40);
  }

  // ============================================================= TableModel

  /** Internal {@link AbstractTableModel} for the student table. */
  static final class StudentTableModel extends AbstractTableModel {

    private List<Student> students = new ArrayList<>();

    void setStudents(List<Student> list) {
      this.students = new ArrayList<>(list);
      fireTableDataChanged();
    }

    Student getStudentAt(int modelRow) { return students.get(modelRow); }

    List<Student> getStudents() { return List.copyOf(students); }

    @Override public int getRowCount()    { return students.size(); }
    @Override public int getColumnCount() { return Constants.TABLE_HEADERS.length; }
    @Override public String getColumnName(int col) { return Constants.TABLE_HEADERS[col]; }

    @Override
    public Class<?> getColumnClass(int col) {
      return switch (col) {
        case Constants.COL_YEAR  -> Integer.class;
        case Constants.COL_MARKS -> Double.class;
        default                  -> String.class;
      };
    }

    @Override
    public Object getValueAt(int row, int col) {
      Student s = students.get(row);
      return switch (col) {
        case Constants.COL_ID     -> s.getStudentId();
        case Constants.COL_NAME   -> s.getName();
        case Constants.COL_BRANCH -> s.getBranch();
        case Constants.COL_YEAR   -> s.getYear();
        case Constants.COL_PHONE  -> s.getPhone();
        case Constants.COL_MARKS  -> s.getMarks().doubleValue();
        default                   -> "";
      };
    }

    @Override public boolean isCellEditable(int row, int col) { return false; }
  }
}
