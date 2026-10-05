package com.sms.service;

import com.sms.dao.StudentDAO;
import com.sms.exception.DataAccessException;
import com.sms.exception.DuplicateStudentException;
import com.sms.exception.ValidationException;
import com.sms.model.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StudentService} with a Mockito-mocked {@link StudentDAO}.
 * No real database is involved.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService")
class StudentServiceTest {

  @Mock
  private StudentDAO dao;

  private StudentService service;

  @BeforeEach
  void setUp() {
    service = new StudentService(dao);
  }

  // ================================================================ addStudent

  @Nested
  @DisplayName("addStudent")
  class AddStudentTests {

    @Test
    @DisplayName("valid data inserts student via DAO")
    void validDataInsertsStudent() {
      when(dao.findById("CS2021001")).thenReturn(Optional.empty());
      when(dao.phoneExists("9876543210", null)).thenReturn(false);

      service.addStudent("CS2021001", "Aarav Kumar", "CSE", 2, "9876543210", "82.50");

      ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
      verify(dao).insert(captor.capture());

      Student inserted = captor.getValue();
      assertEquals("CS2021001", inserted.getStudentId());
      assertEquals("Aarav Kumar", inserted.getName());
      assertEquals("CSE", inserted.getBranch());
      assertEquals(2, inserted.getYear());
      assertEquals("9876543210", inserted.getPhone());
      assertEquals(new BigDecimal("82.50"), inserted.getMarks());
    }

    @Test
    @DisplayName("invalid fields throw ValidationException")
    void invalidFieldsThrowValidationException() {
      ValidationException ex = assertThrows(ValidationException.class,
          () -> service.addStudent("bad-id", "", "UNKNOWN", 9, "123", "abc"));

      assertFalse(ex.getFieldErrors().isEmpty());
      verifyNoInteractions(dao);
    }

    @Test
    @DisplayName("duplicate student_id throws DuplicateStudentException")
    void duplicateIdThrowsDuplicateException() {
      when(dao.findById("CS2021001")).thenReturn(Optional.of(stubStudent()));

      DuplicateStudentException ex = assertThrows(DuplicateStudentException.class,
          () -> service.addStudent("CS2021001", "Aarav Kumar", "CSE", 2, "9876543210", "82.50"));

      assertEquals("student_id", ex.getConflictField());
      verify(dao, never()).insert(any());
    }

    @Test
    @DisplayName("duplicate phone throws DuplicateStudentException")
    void duplicatePhoneThrowsDuplicateException() {
      when(dao.findById("CS2021001")).thenReturn(Optional.empty());
      when(dao.phoneExists("9876543210", null)).thenReturn(true);

      DuplicateStudentException ex = assertThrows(DuplicateStudentException.class,
          () -> service.addStudent("CS2021001", "Aarav Kumar", "CSE", 2, "9876543210", "82.50"));

      assertEquals("phone", ex.getConflictField());
      verify(dao, never()).insert(any());
    }

    @Test
    @DisplayName("DAO DataAccessException propagates unchanged")
    void daoExceptionPropagates() {
      when(dao.findById(any())).thenReturn(Optional.empty());
      when(dao.phoneExists(any(), any())).thenReturn(false);
      doThrow(new DataAccessException("DB error")).when(dao).insert(any());

      assertThrows(DataAccessException.class,
          () -> service.addStudent("CS2021001", "Aarav Kumar", "CSE", 2, "9876543210", "82.50"));
    }
  }

  // ============================================================= updateStudent

  @Nested
  @DisplayName("updateStudent")
  class UpdateStudentTests {

    @Test
    @DisplayName("valid update calls DAO.update")
    void validUpdateCallsDao() {
      when(dao.phoneExists("9876543210", "CS2021001")).thenReturn(false);

      service.updateStudent("CS2021001", "Aarav Kumar", "CSE", 2, "9876543210", "90.00");

      verify(dao).update(any(Student.class));
    }

    @Test
    @DisplayName("phone taken by another student throws DuplicateStudentException")
    void phoneTakenByOtherThrowsDuplicate() {
      when(dao.phoneExists("9876543210", "CS2021001")).thenReturn(true);

      assertThrows(DuplicateStudentException.class,
          () -> service.updateStudent("CS2021001", "Aarav Kumar", "CSE", 2, "9876543210", "90.00"));

      verify(dao, never()).update(any());
    }
  }

  // ============================================================= deleteStudent

  @Nested
  @DisplayName("deleteStudent")
  class DeleteStudentTests {

    @Test
    @DisplayName("delete delegates to DAO")
    void deleteDelegatesToDao() {
      service.deleteStudent("CS2021001");
      verify(dao).delete("CS2021001");
    }
  }

  // ================================================================= summary

  @Nested
  @DisplayName("summary helpers")
  class SummaryTests {

    private List<Student> students;

    @BeforeEach
    void buildStudents() {
      students = List.of(
          studentWithMarks("CS2021001", "80.00"),
          studentWithMarks("CS2021002", "90.00"),
          studentWithMarks("CS2021003", "70.00")
      );
    }

    @Test
    @DisplayName("averageMarks returns correct average")
    void averageMarksIsCorrect() {
      assertEquals(80.0, service.averageMarks(students), 0.001);
    }

    @Test
    @DisplayName("topScorer returns student with highest marks")
    void topScorerIsCorrect() {
      Optional<Student> top = service.topScorer(students);
      assertTrue(top.isPresent());
      assertEquals("CS2021002", top.get().getStudentId());
    }

    @Test
    @DisplayName("averageMarks on empty list returns 0.0")
    void averageMarksOnEmptyListReturnsZero() {
      assertEquals(0.0, service.averageMarks(List.of()), 0.001);
    }

    @Test
    @DisplayName("topScorer on empty list returns empty Optional")
    void topScorerOnEmptyListReturnsEmpty() {
      assertTrue(service.topScorer(List.of()).isEmpty());
    }
  }

  // ================================================================ CSV export

  @Nested
  @DisplayName("exportToCsv")
  class CsvExportTests {

    @Test
    @DisplayName("CSV file contains header and one data row")
    void csvFileContainsHeaderAndRow() throws IOException {
      List<Student> students = List.of(stubStudent());
      File tmp = File.createTempFile("sms_test_", ".csv");
      tmp.deleteOnExit();

      service.exportToCsv(students, tmp);

      String content = Files.readString(tmp.toPath());
      assertTrue(content.contains("Student ID,Name,Branch,Year,Phone,Marks"));
      assertTrue(content.contains("CS2021001"));
      assertTrue(content.contains("Aarav Kumar"));
    }
  }

  // ================================================================= helpers

  private static Student stubStudent() {
    return new Student.Builder()
        .studentId("CS2021001")
        .name("Aarav Kumar")
        .branch("CSE")
        .year(2)
        .phone("9876543210")
        .marks(new BigDecimal("82.50"))
        .build();
  }

  private static Student studentWithMarks(String id, String marks) {
    return new Student.Builder()
        .studentId(id)
        .name("Test Student")
        .branch("CSE")
        .year(1)
        .phone("98765" + id.substring(id.length() - 5))
        .marks(new BigDecimal(marks))
        .build();
  }
}
