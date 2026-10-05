package com.sms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable domain object representing a student record.
 *
 * <p>Use {@link Builder} to construct instances. The {@code updatedAt} and
 * {@code createdAt} fields are populated only when reading from the database;
 * they are ignored on insert/update calls.
 */
public final class Student {

  private final String        studentId;
  private final String        name;
  private final String        branch;
  private final int           year;
  private final String        phone;
  private final BigDecimal    marks;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  private Student(Builder b) {
    this.studentId = b.studentId;
    this.name      = b.name;
    this.branch    = b.branch;
    this.year      = b.year;
    this.phone     = b.phone;
    this.marks     = b.marks;
    this.createdAt = b.createdAt;
    this.updatedAt = b.updatedAt;
  }

  // ------------------------------------------------------------------ getters

  /** @return primary-key ID such as {@code CS2021001}. */
  public String getStudentId() { return studentId; }

  /** @return full display name. */
  public String getName() { return name; }

  /** @return branch abbreviation such as {@code CSE}. */
  public String getBranch() { return branch; }

  /** @return academic year in range 1–4. */
  public int getYear() { return year; }

  /** @return 10-digit phone number string. */
  public String getPhone() { return phone; }

  /** @return marks in range 0.00–100.00. */
  public BigDecimal getMarks() { return marks; }

  /** @return timestamp of first insertion, or {@code null} when not loaded from DB. */
  public LocalDateTime getCreatedAt() { return createdAt; }

  /** @return timestamp of last update, or {@code null} when not loaded from DB. */
  public LocalDateTime getUpdatedAt() { return updatedAt; }

  // ------------------------------------------------------------------ Object

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Student s)) return false;
    return Objects.equals(studentId, s.studentId);
  }

  @Override
  public int hashCode() { return Objects.hash(studentId); }

  @Override
  public String toString() {
    return "Student{id='" + studentId + "', name='" + name
        + "', branch='" + branch + "', year=" + year + '}';
  }

  // --------------------------------------------------------------- Builder

  /** Creates a new {@link Builder} pre-filled with this student's values (for edit use-cases). */
  public Builder toBuilder() {
    return new Builder()
        .studentId(studentId)
        .name(name)
        .branch(branch)
        .year(year)
        .phone(phone)
        .marks(marks)
        .createdAt(createdAt)
        .updatedAt(updatedAt);
  }

  /** Fluent builder for {@link Student}. */
  public static final class Builder {

    private String        studentId;
    private String        name;
    private String        branch;
    private int           year;
    private String        phone;
    private BigDecimal    marks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Builder studentId(String val)      { studentId = val; return this; }
    public Builder name(String val)           { name      = val; return this; }
    public Builder branch(String val)         { branch    = val; return this; }
    public Builder year(int val)              { year      = val; return this; }
    public Builder phone(String val)          { phone     = val; return this; }
    public Builder marks(BigDecimal val)      { marks     = val; return this; }
    public Builder createdAt(LocalDateTime v) { createdAt = v;   return this; }
    public Builder updatedAt(LocalDateTime v) { updatedAt = v;   return this; }

    /** @return new immutable {@link Student}. */
    public Student build() { return new Student(this); }
  }
}
