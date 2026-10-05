package com.sms.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Validator}.
 */
@DisplayName("Validator")
class ValidatorTest {

  // =========================================================== Student ID

  @Nested
  @DisplayName("validateStudentId")
  class StudentIdTests {

    @Test
    @DisplayName("null input returns error")
    void nullReturnsError() {
      assertNotNull(Validator.validateStudentId(null));
    }

    @Test
    @DisplayName("blank input returns error")
    void blankReturnsError() {
      assertNotNull(Validator.validateStudentId("   "));
    }

    @ParameterizedTest(name = "[{index}] valid id: {0}")
    @ValueSource(strings = {"CS2021001", "ECE2022042", "IT2023100", "MECH2024999"})
    @DisplayName("valid IDs pass")
    void validIdsPass(String id) {
      assertNull(Validator.validateStudentId(id));
    }

    @ParameterizedTest(name = "[{index}] invalid id: {0}")
    @ValueSource(strings = {"cs2021001", "2021001CS", "CS202100", "CS20210011", "A2021001"})
    @DisplayName("invalid IDs return error")
    void invalidIdsReturnError(String id) {
      assertNotNull(Validator.validateStudentId(id));
    }
  }

  // ============================================================== Name

  @Nested
  @DisplayName("validateName")
  class NameTests {

    @Test
    @DisplayName("null returns error")
    void nullReturnsError() {
      assertNotNull(Validator.validateName(null));
    }

    @ParameterizedTest(name = "[{index}] valid name: {0}")
    @ValueSource(strings = {"Aarav Kumar", "Priya-Sharma", "O'Brien", "Li"})
    @DisplayName("valid names pass")
    void validNamesPass(String name) {
      assertNull(Validator.validateName(name));
    }

    @ParameterizedTest(name = "[{index}] invalid name: {0}")
    @ValueSource(strings = {"A", "12345", "Name123", ""})
    @DisplayName("invalid names return error")
    void invalidNamesReturnError(String name) {
      assertNotNull(Validator.validateName(name));
    }
  }

  // ============================================================= Branch

  @Nested
  @DisplayName("validateBranch")
  class BranchTests {

    @ParameterizedTest(name = "[{index}] valid branch: {0}")
    @ValueSource(strings = {"CSE", "ECE", "ME", "CE", "EE", "IT"})
    @DisplayName("allowed branches pass")
    void allowedBranchesPass(String branch) {
      assertNull(Validator.validateBranch(branch));
    }

    @ParameterizedTest(name = "[{index}] invalid branch: {0}")
    @ValueSource(strings = {"cse", "CS", "MECH", "", "   "})
    @DisplayName("unknown branches return error")
    void unknownBranchesReturnError(String branch) {
      assertNotNull(Validator.validateBranch(branch));
    }
  }

  // =============================================================== Year

  @Nested
  @DisplayName("validateYear")
  class YearTests {

    @ParameterizedTest(name = "[{index}] valid year: {0}")
    @ValueSource(ints = {1, 2, 3, 4})
    @DisplayName("years 1-4 pass")
    void validYearsPass(int year) {
      assertNull(Validator.validateYear(year));
    }

    @ParameterizedTest(name = "[{index}] invalid year: {0}")
    @ValueSource(ints = {0, 5, -1, 99})
    @DisplayName("out-of-range years return error")
    void outOfRangeYearsReturnError(int year) {
      assertNotNull(Validator.validateYear(year));
    }
  }

  // ============================================================== Phone

  @Nested
  @DisplayName("validatePhone")
  class PhoneTests {

    @Test
    @DisplayName("10-digit phone passes")
    void tenDigitPhonePasses() {
      assertNull(Validator.validatePhone("9876543210"));
    }

    @ParameterizedTest(name = "[{index}] invalid phone: {0}")
    @ValueSource(strings = {"987654321", "98765432100", "987654321A", "", "   "})
    @DisplayName("invalid phones return error")
    void invalidPhonesReturnError(String phone) {
      assertNotNull(Validator.validatePhone(phone));
    }
  }

  // ============================================================== Marks

  @Nested
  @DisplayName("validateMarksText")
  class MarksTests {

    @ParameterizedTest(name = "[{index}] valid marks: {0}")
    @ValueSource(strings = {"0", "0.00", "50", "82.5", "100", "100.00"})
    @DisplayName("valid marks text passes")
    void validMarksTextPasses(String marks) {
      assertNull(Validator.validateMarksText(marks));
    }

    @ParameterizedTest(name = "[{index}] invalid marks: {0}")
    @ValueSource(strings = {"-0.01", "100.01", "abc", "", "   "})
    @DisplayName("invalid marks text returns error")
    void invalidMarksTextReturnsError(String marks) {
      assertNotNull(Validator.validateMarksText(marks));
    }

    @Test
    @DisplayName("null BigDecimal returns error")
    void nullBigDecimalReturnsError() {
      assertNotNull(Validator.validateMarks(null));
    }

    @Test
    @DisplayName("boundary 100.00 passes")
    void exactlyOneHundredPasses() {
      assertNull(Validator.validateMarks(new BigDecimal("100.00")));
    }
  }

  // ============================================================ validateAll

  @Nested
  @DisplayName("validateAll")
  class ValidateAllTests {

    @Test
    @DisplayName("all valid fields return empty error map")
    void allValidReturnsEmptyMap() {
      Map<String, String> errors = Validator.validateAll(
          "CS2021001", "Aarav Kumar", "CSE", 2, "9876543210", "82.50");
      assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("multiple invalid fields return all errors in map")
    void multipleInvalidFieldsReturnAllErrors() {
      Map<String, String> errors = Validator.validateAll(
          "bad", "", "UNKNOWN", 5, "123", "abc");
      assertEquals(6, errors.size());
      assertTrue(errors.containsKey("studentId"));
      assertTrue(errors.containsKey("name"));
      assertTrue(errors.containsKey("branch"));
      assertTrue(errors.containsKey("year"));
      assertTrue(errors.containsKey("phone"));
      assertTrue(errors.containsKey("marks"));
    }
  }
}
