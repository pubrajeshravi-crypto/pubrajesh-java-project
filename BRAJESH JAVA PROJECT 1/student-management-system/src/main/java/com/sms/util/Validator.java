package com.sms.util;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stateless utility class providing field-level validation methods.
 *
 * <p>Each {@code validate*} method returns an error message string when
 * validation fails, or {@code null} when the value is acceptable. The
 * aggregate {@link #validateAll} method collects all errors into a map
 * suitable for passing to {@link com.sms.exception.ValidationException}.
 */
public final class Validator {

  private Validator() {}

  // ---------------------------------------------------------- individual rules

  /**
   * Validates a student ID against the pattern {@code ^[A-Z]{2,4}\d{7}$}.
   *
   * @param id raw input string
   * @return error message or {@code null}
   */
  public static String validateStudentId(String id) {
    if (id == null || id.isBlank()) return "Student ID is required.";
    if (!id.matches(Constants.REGEX_STUDENT_ID))
      return "ID must be 2–4 uppercase letters followed by 7 digits (e.g. CS2021001).";
    return null;
  }

  /**
   * Validates a student name: letters, spaces, hyphens, apostrophes; 2–100 chars.
   *
   * @param name raw input string
   * @return error message or {@code null}
   */
  public static String validateName(String name) {
    if (name == null || name.isBlank()) return "Name is required.";
    String trimmed = name.trim();
    if (trimmed.length() < 2 || trimmed.length() > 100)
      return "Name must be between 2 and 100 characters.";
    if (!trimmed.matches(Constants.REGEX_NAME))
      return "Name may only contain letters, spaces, hyphens, or apostrophes.";
    return null;
  }

  /**
   * Validates that branch is non-blank and in the allowed list.
   *
   * @param branch raw input string
   * @return error message or {@code null}
   */
  public static String validateBranch(String branch) {
    if (branch == null || branch.isBlank()) return "Branch is required.";
    for (String b : Constants.BRANCHES) {
      if (b.equals(branch.trim())) return null;
    }
    return "Branch must be one of: CSE, ECE, ME, CE, EE, IT.";
  }

  /**
   * Validates that year is an integer in range 1–4.
   *
   * @param year candidate year value
   * @return error message or {@code null}
   */
  public static String validateYear(int year) {
    if (year < Constants.YEAR_MIN || year > Constants.YEAR_MAX)
      return "Year must be between 1 and 4.";
    return null;
  }

  /**
   * Validates a phone number as exactly 10 digits.
   *
   * @param phone raw input string
   * @return error message or {@code null}
   */
  public static String validatePhone(String phone) {
    if (phone == null || phone.isBlank()) return "Phone number is required.";
    if (!phone.trim().matches(Constants.REGEX_PHONE))
      return "Phone must be exactly 10 digits.";
    return null;
  }

  /**
   * Validates marks as a decimal value in range 0.00–100.00.
   *
   * @param marks candidate marks value
   * @return error message or {@code null}
   */
  public static String validateMarks(BigDecimal marks) {
    if (marks == null) return "Marks are required.";
    double d = marks.doubleValue();
    if (d < Constants.MARKS_MIN || d > Constants.MARKS_MAX)
      return "Marks must be between 0.00 and 100.00.";
    return null;
  }

  /**
   * Parses and validates marks from a raw string input.
   *
   * @param marksText raw text from a UI field
   * @return error message or {@code null} when the text is a valid decimal in range
   */
  public static String validateMarksText(String marksText) {
    if (marksText == null || marksText.isBlank()) return "Marks are required.";
    try {
      BigDecimal bd = new BigDecimal(marksText.trim());
      return validateMarks(bd);
    } catch (NumberFormatException e) {
      return "Marks must be a valid number (e.g. 82.50).";
    }
  }

  // ----------------------------------------------------------- aggregate

  /**
   * Validates all student fields at once and returns a map of
   * {@code field → errorMessage} for every failing field.
   *
   * @param studentId  raw student ID text
   * @param name       raw name text
   * @param branch     selected branch string
   * @param year       selected year integer
   * @param phone      raw phone text
   * @param marksText  raw marks text
   * @return a map of errors; empty if all fields pass
   */
  public static Map<String, String> validateAll(
      String studentId,
      String name,
      String branch,
      int year,
      String phone,
      String marksText) {

    Map<String, String> errors = new LinkedHashMap<>();

    addIfError(errors, "studentId", validateStudentId(studentId));
    addIfError(errors, "name",      validateName(name));
    addIfError(errors, "branch",    validateBranch(branch));
    addIfError(errors, "year",      validateYear(year));
    addIfError(errors, "phone",     validatePhone(phone));
    addIfError(errors, "marks",     validateMarksText(marksText));

    return errors;
  }

  // ----------------------------------------------------------------- private

  private static void addIfError(Map<String, String> map, String field, String error) {
    if (error != null) map.put(field, error);
  }
}
