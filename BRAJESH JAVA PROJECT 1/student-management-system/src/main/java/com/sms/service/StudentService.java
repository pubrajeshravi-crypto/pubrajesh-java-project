package com.sms.service;

import com.sms.dao.StudentDAO;
import com.sms.exception.DataAccessException;
import com.sms.exception.DuplicateStudentException;
import com.sms.exception.ValidationException;
import com.sms.model.Student;
import com.sms.util.Constants;
import com.sms.util.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Business-logic layer for student operations.
 *
 * <p>This class enforces all validation rules and business constraints before
 * delegating to {@link StudentDAO}.  It never touches JDBC directly; all
 * persistence errors arrive as {@link DataAccessException}.
 *
 * <p>The UI layer calls this class exclusively; it never calls {@link StudentDAO}
 * directly.
 */
public final class StudentService {

  private static final Logger LOG = LoggerFactory.getLogger(StudentService.class);

  private final StudentDAO dao;

  /**
   * Constructs a {@code StudentService} backed by the given DAO.
   *
   * @param dao the data-access object to delegate persistence to
   */
  public StudentService(StudentDAO dao) {
    this.dao = dao;
  }

  // ------------------------------------------------------------------- add

  /**
   * Validates and inserts a new student.
   *
   * @param studentId  raw ID text from the form
   * @param name       raw name text from the form
   * @param branch     selected branch
   * @param year       selected year (1–4)
   * @param phone      raw phone text from the form
   * @param marksText  raw marks text from the form
   * @throws ValidationException        if any field fails validation
   * @throws DuplicateStudentException  if the ID or phone is already taken
   * @throws DataAccessException        on unexpected DB failure
   */
  public void addStudent(String studentId, String name, String branch,
                         int year, String phone, String marksText) {
    Map<String, String> errors = Validator.validateAll(
        studentId, name, branch, year, phone, marksText);
    if (!errors.isEmpty()) throw new ValidationException(errors);

    String id    = studentId.trim();
    String ph    = phone.trim();
    BigDecimal m = new BigDecimal(marksText.trim());

    // Duplicate ID check
    if (dao.findById(id).isPresent()) {
      throw new DuplicateStudentException("student_id", id);
    }

    // Duplicate phone check
    if (dao.phoneExists(ph, null)) {
      throw new DuplicateStudentException("phone", ph);
    }

    Student s = new Student.Builder()
        .studentId(id)
        .name(name.trim())
        .branch(branch.trim())
        .year(year)
        .phone(ph)
        .marks(m)
        .build();

    dao.insert(s);
    LOG.info("Student added: {}", id);
  }

  // ----------------------------------------------------------------- update

  /**
   * Validates and updates an existing student.  The {@code studentId} field
   * is immutable — validation still runs on it but no duplicate-ID check is
   * needed.
   *
   * @param studentId  existing student ID (read-only in the form)
   * @param name       updated name
   * @param branch     updated branch
   * @param year       updated year
   * @param phone      updated phone
   * @param marksText  updated marks text
   * @throws ValidationException        on field validation failure
   * @throws DuplicateStudentException  if the new phone is already used by another student
   * @throws DataAccessException        on unexpected DB failure
   */
  public void updateStudent(String studentId, String name, String branch,
                            int year, String phone, String marksText) {
    Map<String, String> errors = Validator.validateAll(
        studentId, name, branch, year, phone, marksText);
    if (!errors.isEmpty()) throw new ValidationException(errors);

    String id = studentId.trim();
    String ph = phone.trim();
    BigDecimal m = new BigDecimal(marksText.trim());

    // Phone uniqueness: exclude self
    if (dao.phoneExists(ph, id)) {
      throw new DuplicateStudentException("phone", ph);
    }

    Student s = new Student.Builder()
        .studentId(id)
        .name(name.trim())
        .branch(branch.trim())
        .year(year)
        .phone(ph)
        .marks(m)
        .build();

    dao.update(s);
    LOG.info("Student updated: {}", id);
  }

  // ----------------------------------------------------------------- delete

  /**
   * Deletes a student by ID.
   *
   * @param studentId the ID of the student to remove
   * @throws DataAccessException on unexpected DB failure
   */
  public void deleteStudent(String studentId) {
    dao.delete(studentId.trim());
    LOG.info("Student deleted: {}", studentId);
  }

  // ---------------------------------------------------------------- findById

  /**
   * Retrieves a single student by primary key.
   *
   * @param studentId the ID to look up
   * @return an {@link Optional} containing the student, or empty if not found
   * @throws DataAccessException on unexpected DB failure
   */
  public Optional<Student> findById(String studentId) {
    return dao.findById(studentId.trim());
  }

  // ----------------------------------------------------------------- findAll

  /**
   * Returns all students ordered by ID.
   *
   * @return list of all students; never {@code null}
   * @throws DataAccessException on unexpected DB failure
   */
  public List<Student> findAll() {
    return dao.findAll();
  }

  // ------------------------------------------------------------------ search

  /**
   * Searches students by optional name fragment, branch, and year.
   *
   * @param nameFragment partial name; pass {@code null} or blank for no filter
   * @param branch       exact branch; pass {@code null} or blank for any
   * @param year         1–4 to filter; pass {@code 0} for any
   * @return matching students; never {@code null}
   * @throws DataAccessException on unexpected DB failure
   */
  public List<Student> search(String nameFragment, String branch, int year) {
    return dao.search(nameFragment, branch, year);
  }

  // ---------------------------------------------------------------- summary

  /**
   * Computes the average marks over a list of students.
   *
   * @param students list to aggregate; must not be {@code null}
   * @return average marks, or {@code 0.0} if the list is empty
   */
  public double averageMarks(List<Student> students) {
    return students.stream()
        .mapToDouble(s -> s.getMarks().doubleValue())
        .average()
        .orElse(0.0);
  }

  /**
   * Returns the student with the highest marks from the given list.
   *
   * @param students list to inspect; must not be {@code null}
   * @return an {@link Optional} containing the top scorer, or empty if list is empty
   */
  public Optional<Student> topScorer(List<Student> students) {
    return students.stream()
        .max(Comparator.comparing(s -> s.getMarks().doubleValue()));
  }

  // ---------------------------------------------------------------- CSV export

  /**
   * Exports the given list of students to a UTF-8 BOM CSV file.
   *
   * @param students list to export (typically the currently filtered view)
   * @param file     destination file chosen by the user via JFileChooser
   * @throws IOException if the file cannot be written
   */
  public void exportToCsv(List<Student> students, File file) throws IOException {
    // UTF-8 BOM so Excel opens the file correctly without re-encoding
    byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    try (FileOutputStream fos = new FileOutputStream(file);
         BufferedWriter bw = new BufferedWriter(
             new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
      fos.write(bom);
      bw.write(Constants.CSV_HEADER);
      for (Student s : students) {
        bw.write(String.join(",",
            csvEscape(s.getStudentId()),
            csvEscape(s.getName()),
            csvEscape(s.getBranch()),
            String.valueOf(s.getYear()),
            csvEscape(s.getPhone()),
            s.getMarks().toPlainString()));
        bw.newLine();
      }
    }
    LOG.info("Exported {} students to {}", students.size(), file.getAbsolutePath());
  }

  // ----------------------------------------------------------------- private

  /**
   * Wraps a CSV field in double-quotes if it contains commas, quotes, or newlines.
   *
   * @param value raw field value
   * @return CSV-safe string
   */
  private static String csvEscape(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
