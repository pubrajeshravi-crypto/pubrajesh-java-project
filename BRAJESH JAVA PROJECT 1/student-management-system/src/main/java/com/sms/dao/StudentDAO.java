package com.sms.dao;

import com.sms.model.Student;

import java.util.List;
import java.util.Optional;

/**
 * Data-access contract for {@link Student} persistence.
 *
 * <p>All implementations must use {@code PreparedStatement} exclusively and
 * wrap every {@link java.sql.Connection} in a try-with-resources block.
 * No Swing types may appear in any implementation class.
 */
public interface StudentDAO {

  /**
   * Inserts a new student record.
   *
   * @param student the student to persist; {@code student_id} must be unique
   * @throws com.sms.exception.DataAccessException on any SQL error or constraint violation
   */
  void insert(Student student);

  /**
   * Updates all mutable fields of an existing student.
   *
   * @param student must have a valid, pre-existing {@code student_id}
   * @throws com.sms.exception.DataAccessException on any SQL error
   */
  void update(Student student);

  /**
   * Deletes a student by primary key.
   *
   * @param studentId the primary key to remove
   * @throws com.sms.exception.DataAccessException on any SQL error
   */
  void delete(String studentId);

  /**
   * Finds a student by primary key.
   *
   * @param studentId the ID to look up
   * @return an {@link Optional} containing the student, or empty if not found
   * @throws com.sms.exception.DataAccessException on any SQL error
   */
  Optional<Student> findById(String studentId);

  /**
   * Returns every student ordered by {@code student_id}.
   *
   * @return a list of all students; never {@code null}
   * @throws com.sms.exception.DataAccessException on any SQL error
   */
  List<Student> findAll();

  /**
   * Returns students whose {@code name} contains {@code nameFragment} (case-insensitive),
   * optionally filtered by branch and/or year.
   *
   * @param nameFragment partial name to match; pass {@code null} or blank for no name filter
   * @param branch       exact branch to match; pass {@code null} or blank for any branch
   * @param year         year to match; pass {@code 0} for any year
   * @return matching students ordered by {@code name}; never {@code null}
   * @throws com.sms.exception.DataAccessException on any SQL error
   */
  List<Student> search(String nameFragment, String branch, int year);

  /**
   * Checks whether a phone number is already in use by another student.
   *
   * @param phone         the phone number to check
   * @param excludeId     student ID to exclude from the check (pass {@code null} on insert)
   * @return {@code true} if a different student already owns this phone
   * @throws com.sms.exception.DataAccessException on any SQL error
   */
  boolean phoneExists(String phone, String excludeId);
}
