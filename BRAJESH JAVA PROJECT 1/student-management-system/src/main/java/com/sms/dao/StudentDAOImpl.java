package com.sms.dao;

import com.sms.exception.DataAccessException;
import com.sms.model.Student;
import com.sms.util.Constants;
import com.sms.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link StudentDAO}.
 *
 * <p>Rules strictly followed:
 * <ul>
 *   <li>All SQL uses {@link PreparedStatement} — never string concatenation.</li>
 *   <li>Every {@link Connection}, {@link PreparedStatement}, and {@link ResultSet}
 *       is opened inside a try-with-resources block.</li>
 *   <li>No Swing or UI imports.</li>
 *   <li>{@link SQLException} is always wrapped in {@link DataAccessException}.</li>
 * </ul>
 */
public final class StudentDAOImpl implements StudentDAO {

  private static final Logger LOG = LoggerFactory.getLogger(StudentDAOImpl.class);

  // ------------------------------------------------------------------ insert

  @Override
  public void insert(Student student) {
    LOG.debug("Inserting student: {}", student.getStudentId());
    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(Constants.SQL_INSERT)) {
      ps.setString(1, student.getStudentId());
      ps.setString(2, student.getName());
      ps.setString(3, student.getBranch());
      ps.setInt(4, student.getYear());
      ps.setString(5, student.getPhone());
      ps.setBigDecimal(6, student.getMarks());
      ps.executeUpdate();
      LOG.info("Inserted student: {}", student.getStudentId());
    } catch (SQLException e) {
      LOG.error("Failed to insert student {}: {}", student.getStudentId(), e.getMessage());
      throw new DataAccessException("Could not insert student: " + student.getStudentId(), e);
    }
  }

  // ------------------------------------------------------------------ update

  @Override
  public void update(Student student) {
    LOG.debug("Updating student: {}", student.getStudentId());
    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(Constants.SQL_UPDATE)) {
      ps.setString(1, student.getName());
      ps.setString(2, student.getBranch());
      ps.setInt(3, student.getYear());
      ps.setString(4, student.getPhone());
      ps.setBigDecimal(5, student.getMarks());
      ps.setString(6, student.getStudentId());
      ps.executeUpdate();
      LOG.info("Updated student: {}", student.getStudentId());
    } catch (SQLException e) {
      LOG.error("Failed to update student {}: {}", student.getStudentId(), e.getMessage());
      throw new DataAccessException("Could not update student: " + student.getStudentId(), e);
    }
  }

  // ------------------------------------------------------------------ delete

  @Override
  public void delete(String studentId) {
    LOG.debug("Deleting student: {}", studentId);
    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(Constants.SQL_DELETE)) {
      ps.setString(1, studentId);
      ps.executeUpdate();
      LOG.info("Deleted student: {}", studentId);
    } catch (SQLException e) {
      LOG.error("Failed to delete student {}: {}", studentId, e.getMessage());
      throw new DataAccessException("Could not delete student: " + studentId, e);
    }
  }

  // ----------------------------------------------------------------- findById

  @Override
  public Optional<Student> findById(String studentId) {
    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(Constants.SQL_FIND_BY_ID)) {
      ps.setString(1, studentId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new DataAccessException("Could not find student by id: " + studentId, e);
    }
    return Optional.empty();
  }

  // ----------------------------------------------------------------- findAll

  @Override
  public List<Student> findAll() {
    List<Student> list = new ArrayList<>();
    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(Constants.SQL_FIND_ALL);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        list.add(mapRow(rs));
      }
    } catch (SQLException e) {
      throw new DataAccessException("Could not retrieve all students", e);
    }
    return list;
  }

  // ------------------------------------------------------------------ search

  @Override
  public List<Student> search(String nameFragment, String branch, int year) {
    // Build WHERE clause dynamically — values injected via PreparedStatement
    StringBuilder sql = new StringBuilder(Constants.SQL_SEARCH_PREFIX);
    List<Object> params = new ArrayList<>();

    boolean hasWhere = false;

    if (nameFragment != null && !nameFragment.isBlank()) {
      sql.append(" WHERE name LIKE ?");
      params.add("%" + nameFragment.trim() + "%");
      hasWhere = true;
    }

    if (branch != null && !branch.isBlank()) {
      sql.append(hasWhere ? " AND" : " WHERE").append(" branch = ?");
      params.add(branch.trim());
      hasWhere = true;
    }

    if (year >= Constants.YEAR_MIN && year <= Constants.YEAR_MAX) {
      sql.append(hasWhere ? " AND" : " WHERE").append(" year = ?");
      params.add(year);
    }

    sql.append(" ORDER BY name");

    List<Student> list = new ArrayList<>();
    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql.toString())) {
      for (int i = 0; i < params.size(); i++) {
        ps.setObject(i + 1, params.get(i));
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new DataAccessException("Search query failed", e);
    }
    return list;
  }

  // --------------------------------------------------------------- phoneExists

  @Override
  public boolean phoneExists(String phone, String excludeId) {
    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(Constants.SQL_PHONE_EXISTS)) {
      ps.setString(1, phone);
      ps.setString(2, excludeId != null ? excludeId : "");
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getInt(1) > 0;
      }
    } catch (SQLException e) {
      throw new DataAccessException("Phone-exists check failed", e);
    }
  }

  // ----------------------------------------------------------------- mapping

  /**
   * Maps the current row of {@code rs} to a {@link Student} instance.
   *
   * @param rs a {@link ResultSet} positioned at a valid row
   * @return a populated {@link Student}
   * @throws SQLException if any column cannot be read
   */
  private Student mapRow(ResultSet rs) throws SQLException {
    Timestamp createdTs = rs.getTimestamp("created_at");
    Timestamp updatedTs = rs.getTimestamp("updated_at");
    return new Student.Builder()
        .studentId(rs.getString("student_id"))
        .name(rs.getString("name"))
        .branch(rs.getString("branch"))
        .year(rs.getInt("year"))
        .phone(rs.getString("phone"))
        .marks(rs.getBigDecimal("marks"))
        .createdAt(createdTs != null ? createdTs.toLocalDateTime() : null)
        .updatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null)
        .build();
  }
}
