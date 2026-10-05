package com.sms.util;

/**
 * Application-wide constants.
 *
 * <p>No magic numbers or string literals elsewhere in the codebase; all
 * shared values are declared here as {@code public static final} members.
 */
public final class Constants {

  private Constants() {}

  // ---------------------------------------------------------------- branches

  /** Ordered list of allowed academic branches displayed in combos and filter. */
  public static final String[] BRANCHES = {
      "CSE", "ECE", "ME", "CE", "EE", "IT"
  };

  // --------------------------------------------------------------- table columns

  /** Table column index — Student ID. */
  public static final int COL_ID     = 0;
  /** Table column index — Name. */
  public static final int COL_NAME   = 1;
  /** Table column index — Branch. */
  public static final int COL_BRANCH = 2;
  /** Table column index — Year. */
  public static final int COL_YEAR   = 3;
  /** Table column index — Phone. */
  public static final int COL_PHONE  = 4;
  /** Table column index — Marks. */
  public static final int COL_MARKS  = 5;

  /** Display headers matching column indices above. */
  public static final String[] TABLE_HEADERS = {
      "Student ID", "Name", "Branch", "Year", "Phone", "Marks (%)"
  };

  // ----------------------------------------------------------------- UI sizes

  /** Default window width in pixels. */
  public static final int WINDOW_WIDTH  = 1100;
  /** Default window height in pixels. */
  public static final int WINDOW_HEIGHT = 700;

  /** Form dialog width in pixels. */
  public static final int DIALOG_WIDTH  = 480;
  /** Form dialog height in pixels. */
  public static final int DIALOG_HEIGHT = 460;

  /** Standard 8-px spacing grid unit. */
  public static final int SPACING = 8;

  /** Body font size (points). */
  public static final int FONT_SIZE_BODY   = 13;
  /** Header font size (points). */
  public static final int FONT_SIZE_HEADER = 15;

  // ----------------------------------------------------------------- colours (hex strings for UIManager)

  /** Deep-blue accent used for toolbar, table header, selection. */
  public static final String COLOR_ACCENT  = "#1565C0";
  /** Alternate row stripe colour. */
  public static final String COLOR_STRIPE  = "#EEF2FF";
  /** Inline validation error text colour. */
  public static final String COLOR_ERROR   = "#C62828";
  /** Success status bar text colour. */
  public static final String COLOR_SUCCESS = "#1B5E20";

  // ----------------------------------------------------------------- validation

  /**
   * Regular expression for valid student IDs: 2–4 uppercase letters followed
   * by exactly 7 digits (e.g., {@code CS2021001}, {@code ECE2022042}).
   */
  public static final String REGEX_STUDENT_ID = "^[A-Z]{2,4}\\d{7}$";

  /** Regular expression for names: letters, spaces, hyphens, apostrophes; 2–100 chars. */
  public static final String REGEX_NAME = "^[A-Za-z][A-Za-z\\s\\-']{1,99}$";

  /** Regular expression for 10-digit phone number. */
  public static final String REGEX_PHONE = "^\\d{10}$";

  /** Minimum allowed marks value. */
  public static final double MARKS_MIN = 0.0;
  /** Maximum allowed marks value. */
  public static final double MARKS_MAX = 100.0;

  /** Minimum allowed year. */
  public static final int YEAR_MIN = 1;
  /** Maximum allowed year. */
  public static final int YEAR_MAX = 4;

  // ----------------------------------------------------------------- SQL literals

  /** SQL for inserting a student row. */
  public static final String SQL_INSERT =
      "INSERT INTO students (student_id, name, branch, year, phone, marks) "
      + "VALUES (?, ?, ?, ?, ?, ?)";

  /** SQL for updating a student row by primary key. */
  public static final String SQL_UPDATE =
      "UPDATE students SET name=?, branch=?, year=?, phone=?, marks=? "
      + "WHERE student_id=?";

  /** SQL for deleting a student row by primary key. */
  public static final String SQL_DELETE =
      "DELETE FROM students WHERE student_id=?";

  /** SQL for finding one student by primary key. */
  public static final String SQL_FIND_BY_ID =
      "SELECT student_id, name, branch, year, phone, marks, created_at, updated_at "
      + "FROM students WHERE student_id=?";

  /** SQL for listing all students ordered by ID. */
  public static final String SQL_FIND_ALL =
      "SELECT student_id, name, branch, year, phone, marks, created_at, updated_at "
      + "FROM students ORDER BY student_id";

  /** SQL prefix for the dynamic search query (WHERE clause built at runtime). */
  public static final String SQL_SEARCH_PREFIX =
      "SELECT student_id, name, branch, year, phone, marks, created_at, updated_at "
      + "FROM students";

  /** SQL for checking phone uniqueness (excluding a given ID). */
  public static final String SQL_PHONE_EXISTS =
      "SELECT COUNT(*) FROM students WHERE phone=? AND student_id != ?";

  // ----------------------------------------------------------------- CSV

  /** CSV file header line. */
  public static final String CSV_HEADER =
      "Student ID,Name,Branch,Year,Phone,Marks\n";

  /** Search debounce delay in milliseconds. */
  public static final int SEARCH_DEBOUNCE_MS = 300;
}
