package com.sms.exception;

/**
 * Thrown when an attempt is made to insert a student whose ID or phone
 * already exists in the database.
 */
public class DuplicateStudentException extends RuntimeException {

  /** The field that caused the conflict: {@code "student_id"} or {@code "phone"}. */
  private final String conflictField;

  /** The conflicting value. */
  private final String conflictValue;

  /**
   * @param conflictField field name that triggered the duplicate ({@code "student_id"} or {@code "phone"})
   * @param conflictValue the duplicate value
   */
  public DuplicateStudentException(String conflictField, String conflictValue) {
    super("Duplicate value for field '" + conflictField + "': " + conflictValue);
    this.conflictField = conflictField;
    this.conflictValue = conflictValue;
  }

  /** @return the field name that has a duplicate */
  public String getConflictField() { return conflictField; }

  /** @return the duplicate value */
  public String getConflictValue() { return conflictValue; }
}
