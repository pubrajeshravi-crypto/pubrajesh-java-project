package com.sms.ui;

import com.sms.exception.DataAccessException;
import com.sms.model.Student;
import com.sms.service.StudentService;
import com.sms.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Root application window for the Student Management System.
 *
 * <p>Wires together the toolbar, search bar, summary strip, table panel, and
 * status bar.  All database calls are dispatched via {@link SwingWorker} so
 * the Event Dispatch Thread is never blocked.
 */
public final class MainFrame extends JFrame {

  private static final Logger LOG = LoggerFactory.getLogger(MainFrame.class);

  private final StudentService service;

  // UI components
  private final StudentTablePanel tablePanel   = new StudentTablePanel();
  private final SearchBar         searchBar;
  private final StatusBar         statusBar    = new StatusBar();
  private final JLabel            totalLabel   = makeSummaryLabel();
  private final JLabel            avgLabel     = makeSummaryLabel();
  private final JLabel            topLabel     = makeSummaryLabel();

  // Toolbar actions (kept as fields so we can enable/disable them)
  private final JButton editButton;
  private final JButton deleteButton;

  // ----------------------------------------------------------------- ctor

  /**
   * Constructs and fully initialises the main frame.
   *
   * @param service the service layer to delegate all operations to
   */
  public MainFrame(StudentService service) {
    super("Student Management System");
    this.service = service;

    // Build the search bar with the search callback
    searchBar = new SearchBar(e -> triggerSearch());

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
    setMinimumSize(new Dimension(800, 500));
    setLocationRelativeTo(null);

    // Build toolbar (needed before wiring shortcuts)
    JToolBar toolbar = buildToolbar();
    editButton   = findButton(toolbar, "Edit");
    deleteButton = findButton(toolbar, "Delete");
    setEditDeleteEnabled(false);

    // Selection listener → enable/disable Edit & Delete
    tablePanel.getTable().getSelectionModel().addListSelectionListener(this::onSelectionChanged);

    // Assemble layout
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.add(toolbar,              BorderLayout.NORTH);
    mainPanel.add(buildCenterPanel(),   BorderLayout.CENTER);
    mainPanel.add(statusBar,            BorderLayout.SOUTH);

    setContentPane(mainPanel);

    installKeyboardShortcuts();

    // Load all students on startup
    loadAll();
  }

  // --------------------------------------------------------------- toolbar

  private JToolBar buildToolbar() {
    JToolBar bar = new JToolBar();
    bar.setFloatable(false);
    bar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(0xD0D0D0)));
    bar.setBackground(new Color(0xF5F5F5));

    bar.add(makeToolButton("+ Add",    "Add new student (Ctrl+N)",   e -> openAddDialog()));
    bar.add(makeToolButton("✎ Edit",   "Edit selected student",      e -> openEditDialog()));
    bar.add(makeToolButton("✕ Delete", "Delete selected (Delete)",   e -> confirmDelete()));
    bar.addSeparator(new Dimension(16, 0));
    bar.add(makeToolButton("↓ Export CSV", "Export current view",   e -> exportCsv()));
    bar.addSeparator(new Dimension(16, 0));
    bar.add(makeToolButton("🌐 Open Web UI (8080)", "Launch modern web dashboard in browser", e -> openWebDashboard()));

    return bar;
  }

  private void openWebDashboard() {
    try {
      if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
        java.awt.Desktop.getDesktop().browse(new java.net.URI("http://localhost:8080/"));
      } else {
        Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "http://localhost:8080/"});
      }
    } catch (Exception ex) {
      LOG.error("Failed to open web browser", ex);
    }
  }

  private static JButton makeToolButton(String text, String tooltip, java.awt.event.ActionListener action) {
    JButton btn = new JButton(text);
    btn.setToolTipText(tooltip);
    btn.setFocusPainted(false);
    btn.setFont(btn.getFont().deriveFont((float) Constants.FONT_SIZE_BODY));
    btn.setBorder(new EmptyBorder(6, 14, 6, 14));
    btn.addActionListener(action);
    return btn;
  }

  /** Finds a toolbar button by its text label. */
  private static JButton findButton(JToolBar bar, String text) {
    for (Component c : bar.getComponents()) {
      if (c instanceof JButton b && b.getText().contains(text)) return b;
    }
    return new JButton(); // fallback, should never happen
  }

  // --------------------------------------------------------------- center

  private JPanel buildCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());

    // Search bar + summary strip at the top
    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(searchBar,         BorderLayout.NORTH);
    topPanel.add(buildSummaryStrip(), BorderLayout.SOUTH);

    panel.add(topPanel,   BorderLayout.NORTH);
    panel.add(tablePanel, BorderLayout.CENTER);
    return panel;
  }

  private JPanel buildSummaryStrip() {
    JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, Constants.SPACING * 3, 4));
    strip.setBackground(new Color(0xEAEFF7));
    strip.setBorder(new MatteBorder(1, 0, 1, 0, new Color(0xD0D0D0)));

    strip.add(new JLabel("Total:"));
    strip.add(totalLabel);
    strip.add(separator());
    strip.add(new JLabel("Avg Marks:"));
    strip.add(avgLabel);
    strip.add(separator());
    strip.add(new JLabel("Top Scorer:"));
    strip.add(topLabel);
    return strip;
  }

  private static JSeparator separator() {
    JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
    sep.setPreferredSize(new Dimension(1, 16));
    return sep;
  }

  private static JLabel makeSummaryLabel() {
    JLabel lbl = new JLabel("—");
    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, Constants.FONT_SIZE_BODY));
    lbl.setForeground(Color.decode(Constants.COLOR_ACCENT));
    return lbl;
  }

  // --------------------------------------------------------------- shortcuts

  private void installKeyboardShortcuts() {
    JRootPane rp = getRootPane();

    // Ctrl+N → Add
    rp.registerKeyboardAction(e -> openAddDialog(),
        KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK),
        JComponent.WHEN_IN_FOCUSED_WINDOW);

    // Ctrl+F → Focus search
    rp.registerKeyboardAction(e -> searchBar.focusSearchField(),
        KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
        JComponent.WHEN_IN_FOCUSED_WINDOW);

    // Delete key → delete selected row
    rp.registerKeyboardAction(e -> confirmDelete(),
        KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
        JComponent.WHEN_IN_FOCUSED_WINDOW);
  }

  // ---------------------------------------------------------- SwingWorker ops

  /** Loads all students and refreshes the table (no filter). */
  private void loadAll() {
    new SwingWorker<List<Student>, Void>() {
      @Override protected List<Student> doInBackground() { return service.findAll(); }
      @Override protected void done() {
        try {
          List<Student> students = get();
          tablePanel.setStudents(students);
          updateSummary(students);
          statusBar.setInfo("Ready — " + students.size() + " students loaded.");
          statusBar.setCount(students.size());
        } catch (ExecutionException | InterruptedException e) {
          handleWorkerException(e);
        }
      }
    }.execute();
  }

  /** Runs the search based on current SearchBar values. */
  private void triggerSearch() {
    String nameFragment = searchBar.getSearchText();
    String branch       = searchBar.getSelectedBranch();
    int    year         = searchBar.getSelectedYear();

    new SwingWorker<List<Student>, Void>() {
      @Override protected List<Student> doInBackground() {
        return service.search(nameFragment, branch, year);
      }
      @Override protected void done() {
        try {
          List<Student> students = get();
          tablePanel.setStudents(students);
          updateSummary(students);
          statusBar.setInfo("Found " + students.size() + " student(s).");
          statusBar.setCount(students.size());
        } catch (ExecutionException | InterruptedException e) {
          handleWorkerException(e);
        }
      }
    }.execute();
  }

  /** Opens the Add dialog, then reloads on save. */
  private void openAddDialog() {
    StudentFormDialog dlg = new StudentFormDialog(this, service);
    dlg.setVisible(true);
    if (dlg.isSaved()) {
      statusBar.setSuccess("Student added successfully.");
      triggerSearch();
    }
  }

  /** Opens the Edit dialog for the selected student, then reloads on save. */
  private void openEditDialog() {
    Student selected = tablePanel.getSelectedStudent();
    if (selected == null) return;
    StudentFormDialog dlg = new StudentFormDialog(this, service, selected);
    dlg.setVisible(true);
    if (dlg.isSaved()) {
      statusBar.setSuccess("Student '" + selected.getStudentId() + "' updated.");
      triggerSearch();
    }
  }

  /** Shows a confirm dialog and deletes the selected student via SwingWorker. */
  private void confirmDelete() {
    Student selected = tablePanel.getSelectedStudent();
    if (selected == null) return;

    int choice = JOptionPane.showConfirmDialog(this,
        "<html>Delete student <b>" + selected.getStudentId() + "</b> — "
            + selected.getName() + "?<br>This action cannot be undone.</html>",
        "Confirm Delete",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE);

    if (choice != JOptionPane.YES_OPTION) return;

    new SwingWorker<Void, Void>() {
      @Override protected Void doInBackground() {
        service.deleteStudent(selected.getStudentId());
        return null;
      }
      @Override protected void done() {
        try {
          get();
          statusBar.setSuccess("Student '" + selected.getStudentId() + "' deleted.");
          triggerSearch();
        } catch (ExecutionException | InterruptedException e) {
          handleWorkerException(e);
        }
      }
    }.execute();
  }

  /** Exports the currently visible rows to a user-chosen CSV file. */
  private void exportCsv() {
    List<Student> visible = tablePanel.getDisplayedStudents();
    JFileChooser fc = new JFileChooser();
    fc.setSelectedFile(new File("students_export.csv"));
    fc.setDialogTitle("Export Students to CSV");
    int result = fc.showSaveDialog(this);
    if (result != JFileChooser.APPROVE_OPTION) return;

    File dest = fc.getSelectedFile();
    new SwingWorker<Void, Void>() {
      @Override protected Void doInBackground() throws Exception {
        service.exportToCsv(visible, dest);
        return null;
      }
      @Override protected void done() {
        try {
          get();
          statusBar.setSuccess("Exported " + visible.size() + " records to " + dest.getName());
        } catch (ExecutionException | InterruptedException e) {
          handleWorkerException(e);
        }
      }
    }.execute();
  }

  // -------------------------------------------------------------- helpers

  private void onSelectionChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting()) return;
    boolean hasSelection = tablePanel.getSelectedStudent() != null;
    setEditDeleteEnabled(hasSelection);
  }

  private void setEditDeleteEnabled(boolean enabled) {
    editButton.setEnabled(enabled);
    deleteButton.setEnabled(enabled);
  }

  private void updateSummary(List<Student> students) {
    totalLabel.setText(String.valueOf(students.size()));
    avgLabel.setText(String.format("%.2f", service.averageMarks(students)));
    Optional<Student> top = service.topScorer(students);
    topLabel.setText(top.map(s -> s.getName() + " (" + s.getMarks().toPlainString() + ")")
        .orElse("—"));
  }

  private void handleWorkerException(Exception ex) {
    Throwable cause = (ex instanceof ExecutionException ee && ee.getCause() != null)
        ? ee.getCause() : ex;
    LOG.error("Background task failed", cause);
    statusBar.setError("Error: " + cause.getMessage());
    JOptionPane.showMessageDialog(this,
        "<html><b>An error occurred:</b><br>" + cause.getMessage() + "</html>",
        "Error", JOptionPane.ERROR_MESSAGE);
  }
}
