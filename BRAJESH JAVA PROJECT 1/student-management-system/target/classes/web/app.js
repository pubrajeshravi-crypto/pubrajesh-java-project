/**
 * Student Management System — Modern Web Application Logic
 * Full REST API Integration, Real-Time Filtering, Sorting & Modal Management
 */

(function () {
  'use strict';

  // --- State ---
  const state = {
    students: [],
    searchQuery: '',
    activeBranch: 'ALL',
    selectedYear: 0,
    sortCol: 'id',
    sortAsc: true,
    editingStudentId: null,
    deletingStudent: null,
  };

  // --- DOM Selectors ---
  const searchInput = document.getElementById('searchInput');
  const clearSearchBtn = document.getElementById('clearSearchBtn');
  const branchPills = document.querySelectorAll('.branch-pill');
  const yearSelect = document.getElementById('yearSelect');
  const refreshBtn = document.getElementById('refreshBtn');
  const studentsTableBody = document.getElementById('studentsTableBody');
  const emptyState = document.getElementById('emptyState');
  const studentCountBadge = document.getElementById('studentCountBadge');
  const themeToggleBtn = document.getElementById('themeToggleBtn');
  const addStudentBtn = document.getElementById('addStudentBtn');

  // KPI elements
  const kpiTotalStudents = document.getElementById('kpiTotalStudents');
  const kpiAvgMarks = document.getElementById('kpiAvgMarks');
  const kpiPassRate = document.getElementById('kpiPassRate');
  const kpiAvgBar = document.getElementById('kpiAvgBar');
  const kpiTopScorerName = document.getElementById('kpiTopScorerName');
  const kpiTopScorerDetail = document.getElementById('kpiTopScorerDetail');
  const dbStatusText = document.getElementById('dbStatusText');

  // Modal elements
  const studentModal = document.getElementById('studentModal');
  const modalTitle = document.getElementById('modalTitle');
  const modalSubtitle = document.getElementById('modalSubtitle');
  const closeModalBtn = document.getElementById('closeModalBtn');
  const cancelModalBtn = document.getElementById('cancelModalBtn');
  const studentForm = document.getElementById('studentForm');

  const inputStudentId = document.getElementById('inputStudentId');
  const inputName = document.getElementById('inputName');
  const selectBranch = document.getElementById('selectBranch');
  const selectYear = document.getElementById('selectYear');
  const inputPhone = document.getElementById('inputPhone');
  const inputMarks = document.getElementById('inputMarks');

  // Delete modal elements
  const deleteModal = document.getElementById('deleteModal');
  const closeDeleteModalBtn = document.getElementById('closeDeleteModalBtn');
  const cancelDeleteBtn = document.getElementById('cancelDeleteBtn');
  const confirmDeleteBtn = document.getElementById('confirmDeleteBtn');
  const deleteStudentPreview = document.getElementById('deleteStudentPreview');

  // --- Initialize ---
  document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    bindEvents();
    loadDashboard();
    checkHealth();
    // Refresh stats periodically
    setInterval(checkHealth, 30000);
  });

  // --- Theme Management ---
  function initTheme() {
    const savedTheme = localStorage.getItem('sms_theme') || 'theme-dark';
    document.body.className = savedTheme;
  }

  function toggleTheme() {
    const newTheme = document.body.classList.contains('theme-dark') ? 'theme-light' : 'theme-dark';
    document.body.className = newTheme;
    localStorage.setItem('sms_theme', newTheme);
  }

  // --- Event Bindings ---
  function bindEvents() {
    themeToggleBtn.addEventListener('click', toggleTheme);

    // Search with debounce
    let debounceTimer;
    searchInput.addEventListener('input', (e) => {
      state.searchQuery = e.target.value.trim();
      clearSearchBtn.style.display = state.searchQuery ? 'block' : 'none';
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
        fetchStudents();
      }, 250);
    });

    clearSearchBtn.addEventListener('click', () => {
      searchInput.value = '';
      state.searchQuery = '';
      clearSearchBtn.style.display = 'none';
      fetchStudents();
    });

    // Branch filter pills
    branchPills.forEach((pill) => {
      pill.addEventListener('click', () => {
        branchPills.forEach((p) => p.classList.remove('active'));
        pill.classList.add('active');
        state.activeBranch = pill.getAttribute('data-branch');
        fetchStudents();
      });
    });

    // Year dropdown
    yearSelect.addEventListener('change', (e) => {
      state.selectedYear = parseInt(e.target.value, 10) || 0;
      fetchStudents();
    });

    // Refresh button
    refreshBtn.addEventListener('click', () => {
      loadDashboard();
      showToast('Data refreshed from MySQL', 'info');
    });

    // Table sorting headers
    document.querySelectorAll('.data-table th[data-sort]').forEach((th) => {
      th.addEventListener('click', () => {
        const col = th.getAttribute('data-sort');
        if (state.sortCol === col) {
          state.sortAsc = !state.sortAsc;
        } else {
          state.sortCol = col;
          state.sortAsc = true;
        }
        renderTable();
      });
    });

    // Modal controls
    addStudentBtn.addEventListener('click', openAddModal);
    closeModalBtn.addEventListener('click', closeStudentModal);
    cancelModalBtn.addEventListener('click', closeStudentModal);
    studentForm.addEventListener('submit', handleFormSubmit);

    // Delete Modal controls
    closeDeleteModalBtn.addEventListener('click', closeDeleteModal);
    cancelDeleteBtn.addEventListener('click', closeDeleteModal);
    confirmDeleteBtn.addEventListener('click', executeDelete);

    // Global Esc key closes modals
    window.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        closeStudentModal();
        closeDeleteModal();
      }
    });

    // Click backdrop to close
    studentModal.addEventListener('click', (e) => {
      if (e.target === studentModal) closeStudentModal();
    });
    deleteModal.addEventListener('click', (e) => {
      if (e.target === deleteModal) closeDeleteModal();
    });
  }

  // --- API Calls ---
  async function loadDashboard() {
    await Promise.all([fetchStudents(), fetchStats()]);
  }

  async function checkHealth() {
    try {
      const res = await fetch('/api/health');
      if (res.ok) {
        dbStatusText.textContent = 'Connected (sms_db)';
        document.getElementById('systemStatusBadge').style.borderColor = 'rgba(16, 185, 129, 0.25)';
      }
    } catch {
      dbStatusText.textContent = 'Disconnected';
      document.getElementById('systemStatusBadge').style.borderColor = 'rgba(239, 68, 68, 0.4)';
    }
  }

  async function fetchStudents() {
    try {
      const params = new URLSearchParams();
      if (state.searchQuery) params.append('search', state.searchQuery);
      if (state.activeBranch && state.activeBranch !== 'ALL') params.append('branch', state.activeBranch);
      if (state.selectedYear > 0) params.append('year', state.selectedYear);

      const url = `/api/students${params.toString() ? '?' + params.toString() : ''}`;
      const res = await fetch(url);
      if (!res.ok) throw new Error('Failed to fetch students');

      const data = await res.json();
      state.students = Array.isArray(data) ? data : [];
      renderTable();
    } catch (err) {
      console.error(err);
      studentsTableBody.innerHTML = `<tr><td colspan="7" class="loading-state" style="color: #ef4444;">Failed to load records from database.</td></tr>`;
    }
  }

  async function fetchStats() {
    try {
      const res = await fetch('/api/stats');
      if (!res.ok) return;
      const stats = await res.json();

      kpiTotalStudents.textContent = stats.totalStudents ?? 0;
      kpiAvgMarks.textContent = (stats.averageMarks ? stats.averageMarks.toFixed(1) : '0.0') + '%';
      kpiPassRate.textContent = (stats.passRate ? stats.passRate.toFixed(1) : '0.0') + '% Pass';
      kpiAvgBar.style.width = Math.min(Math.max(stats.averageMarks || 0, 0), 100) + '%';

      if (stats.topScorer) {
        kpiTopScorerName.textContent = stats.topScorer.name;
        kpiTopScorerDetail.textContent = `${stats.topScorer.branch} • Year ${stats.topScorer.year} • ${Number(stats.topScorer.marks).toFixed(1)}%`;
      } else {
        kpiTopScorerName.textContent = 'None';
        kpiTopScorerDetail.textContent = 'No records available';
      }
    } catch (err) {
      console.error('Error fetching stats:', err);
    }
  }

  // --- Rendering ---
  function renderTable() {
    studentCountBadge.textContent = `${state.students.length} record${state.students.length === 1 ? '' : 's'}`;

    if (state.students.length === 0) {
      studentsTableBody.innerHTML = '';
      emptyState.style.display = 'flex';
      return;
    }

    emptyState.style.display = 'none';

    // Sort students
    const sorted = [...state.students].sort((a, b) => {
      let va = a[state.sortCol === 'id' ? 'studentId' : state.sortCol];
      let vb = b[state.sortCol === 'id' ? 'studentId' : state.sortCol];

      if (state.sortCol === 'marks' || state.sortCol === 'year') {
        va = Number(va) || 0;
        vb = Number(vb) || 0;
        return state.sortAsc ? va - vb : vb - va;
      }

      va = (va || '').toString().toLowerCase();
      vb = (vb || '').toString().toLowerCase();
      return state.sortAsc ? va.localeCompare(vb) : vb.localeCompare(va);
    });

    const rowsHtml = sorted
      .map((s) => {
        const marksVal = Number(s.marks) || 0;
        const gradeInfo = getGradeInfo(marksVal);
        const avatarColor = getAvatarColor(s.name);
        const initials = getInitials(s.name);

        return `
        <tr data-id="${escapeHtml(s.studentId)}">
          <td>
            <div class="student-id-cell">
              <span>${escapeHtml(s.studentId)}</span>
              <button class="copy-id-btn" title="Copy ID" onclick="copyToClipboard('${escapeHtml(s.studentId)}')">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                </svg>
              </button>
            </div>
          </td>
          <td>
            <div class="student-name-cell">
              <div class="avatar-badge" style="background: ${avatarColor}">${initials}</div>
              <strong>${escapeHtml(s.name)}</strong>
            </div>
          </td>
          <td><span class="branch-tag branch-${escapeHtml(s.branch)}">${escapeHtml(s.branch)}</span></td>
          <td>Year ${s.year}</td>
          <td>${formatPhone(s.phone)}</td>
          <td>
            <div class="marks-cell">
              <div class="marks-bar-wrap" title="${marksVal}%">
                <div class="marks-bar-fill" style="width: ${marksVal}%; background: ${gradeInfo.color};"></div>
              </div>
              <span><strong>${marksVal.toFixed(1)}%</strong></span>
              <span class="grade-badge ${gradeInfo.badgeClass}">${gradeInfo.letter}</span>
            </div>
          </td>
          <td class="text-right">
            <div class="table-actions">
              <button class="btn-table-action action-edit" title="Edit Student" onclick="openEditModal('${escapeHtml(s.studentId)}')">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                </svg>
              </button>
              <button class="btn-table-action action-delete" title="Delete Student" onclick="openDeleteModal('${escapeHtml(s.studentId)}')">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"></polyline>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                </svg>
              </button>
            </div>
          </td>
        </tr>`;
      })
      .join('');

    studentsTableBody.innerHTML = rowsHtml;
  }

  // --- Modal Management ---
  function openAddModal() {
    state.editingStudentId = null;
    modalTitle.textContent = 'Add New Student';
    modalSubtitle.textContent = 'Create an official university enrollment entry in MySQL';
    inputStudentId.value = '';
    inputStudentId.readOnly = false;
    inputName.value = '';
    selectBranch.value = '';
    selectYear.value = '1';
    inputPhone.value = '';
    inputMarks.value = '';

    clearAllErrors();
    studentModal.style.display = 'flex';
    setTimeout(() => inputStudentId.focus(), 50);
  }

  window.openAddStudentModal = openAddModal;

  window.openEditModal = function (studentId) {
    const s = state.students.find((item) => item.studentId === studentId);
    if (!s) return;

    state.editingStudentId = studentId;
    modalTitle.textContent = 'Edit Student Details';
    modalSubtitle.textContent = `Updating record for ${s.name} (${s.studentId})`;
    inputStudentId.value = s.studentId;
    inputStudentId.readOnly = true; // Primary key immutable
    inputName.value = s.name;
    selectBranch.value = s.branch;
    selectYear.value = s.year.toString();
    inputPhone.value = s.phone;
    inputMarks.value = Number(s.marks).toFixed(2);

    clearAllErrors();
    studentModal.style.display = 'flex';
    setTimeout(() => inputName.focus(), 50);
  };

  function closeStudentModal() {
    studentModal.style.display = 'none';
    state.editingStudentId = null;
  }

  // --- Delete Modal ---
  window.openDeleteModal = function (studentId) {
    const s = state.students.find((item) => item.studentId === studentId);
    if (!s) return;

    state.deletingStudent = s;
    deleteStudentPreview.innerHTML = `
      <strong>${escapeHtml(s.name)}</strong> (${escapeHtml(s.studentId)})<br>
      <span style="color: var(--text-subtle);">Branch: ${escapeHtml(s.branch)} &bull; Year ${s.year} &bull; Phone: ${formatPhone(s.phone)}</span>
    `;
    deleteModal.style.display = 'flex';
  };

  function closeDeleteModal() {
    deleteModal.style.display = 'none';
    state.deletingStudent = null;
  }

  async function executeDelete() {
    if (!state.deletingStudent) return;
    const id = state.deletingStudent.studentId;

    confirmDeleteBtn.disabled = true;
    confirmDeleteBtn.querySelector('.btn-spinner').style.display = 'inline-block';

    try {
      const res = await fetch(`/api/students/${encodeURIComponent(id)}`, { method: 'DELETE' });
      if (!res.ok) {
        const data = await res.json();
        throw new Error(data.error || 'Failed to delete student');
      }

      showToast(`Student ${id} removed permanently`, 'success');
      closeDeleteModal();
      await loadDashboard();
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      confirmDeleteBtn.disabled = false;
      confirmDeleteBtn.querySelector('.btn-spinner').style.display = 'none';
    }
  }

  // --- Form Validation & Submission ---
  async function handleFormSubmit(e) {
    e.preventDefault();
    clearAllErrors();

    const studentId = inputStudentId.value.trim().toUpperCase();
    const name = inputName.value.trim();
    const branch = selectBranch.value;
    const year = parseInt(selectYear.value, 10);
    const phone = inputPhone.value.trim();
    const marksText = inputMarks.value.trim();

    // Client-side regex checks matching Constants.java
    let isValid = true;

    if (!state.editingStudentId) {
      if (!studentId) {
        showError('StudentId', 'Student ID is required');
        isValid = false;
      } else if (!/^[A-Z]{2,4}\d{7}$/.test(studentId)) {
        showError('StudentId', 'Format: 2-4 uppercase letters followed by 7 digits (e.g. CS2021001)');
        isValid = false;
      }
    }

    if (!name) {
      showError('Name', 'Name is required');
      isValid = false;
    } else if (!/^[A-Za-z][A-Za-z\s\-']{1,99}$/.test(name)) {
      showError('Name', 'Name must start with a letter and be 2-100 characters');
      isValid = false;
    }

    if (!branch) {
      showError('Branch', 'Please select a branch');
      isValid = false;
    }

    if (!year || year < 1 || year > 4) {
      showError('Year', 'Year must be between 1 and 4');
      isValid = false;
    }

    if (!phone) {
      showError('Phone', 'Phone number is required');
      isValid = false;
    } else if (!/^\d{10}$/.test(phone)) {
      showError('Phone', 'Phone number must be exactly 10 digits');
      isValid = false;
    }

    const marksNum = parseFloat(marksText);
    if (!marksText || isNaN(marksNum) || marksNum < 0 || marksNum > 100) {
      showError('Marks', 'Marks must be a number between 0.00 and 100.00');
      isValid = false;
    }

    if (!isValid) return;

    const payload = {
      studentId: state.editingStudentId ? state.editingStudentId : studentId,
      name,
      branch,
      year,
      phone,
      marks: marksText,
    };

    const isEdit = Boolean(state.editingStudentId);
    const saveBtn = document.getElementById('saveStudentBtn');
    saveBtn.disabled = true;
    saveBtn.querySelector('.btn-spinner').style.display = 'inline-block';

    try {
      const url = isEdit ? `/api/students/${encodeURIComponent(state.editingStudentId)}` : '/api/students';
      const method = isEdit ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      const data = await res.json();

      if (!res.ok) {
        if (res.status === 409) {
          // Duplicate key
          if (data.field === 'phone') {
            showError('Phone', 'This phone number is already registered to another student');
          } else {
            showError('StudentId', 'A student with this ID already exists');
          }
          return;
        } else if (res.status === 400 && data.fields) {
          // Field validation errors
          for (const [field, msg] of Object.entries(data.fields)) {
            const cap = field.charAt(0).toUpperCase() + field.slice(1);
            showError(cap, msg);
          }
          return;
        }
        throw new Error(data.error || 'Server error occurred');
      }

      showToast(isEdit ? 'Student updated successfully!' : 'Student registered successfully!', 'success');
      closeStudentModal();
      await loadDashboard();
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      saveBtn.disabled = false;
      saveBtn.querySelector('.btn-spinner').style.display = 'none';
    }
  }

  function showError(fieldId, message) {
    const errorEl = document.getElementById(`error${fieldId}`);
    const groupEl = document.getElementById(`group${fieldId}`);
    if (errorEl) errorEl.textContent = message;
    if (groupEl) groupEl.classList.add('has-error');
  }

  function clearAllErrors() {
    document.querySelectorAll('.error-msg').forEach((el) => (el.textContent = ''));
    document.querySelectorAll('.form-group').forEach((el) => el.classList.remove('has-error'));
  }

  // --- Toast System ---
  function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let iconSvg = '';
    if (type === 'success') {
      iconSvg = `<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#10b981" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>`;
    } else if (type === 'error') {
      iconSvg = `<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#ef4444" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>`;
    } else {
      iconSvg = `<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#3b82f6" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`;
    }

    toast.innerHTML = `
      <div class="toast-icon">${iconSvg}</div>
      <div class="toast-message">${escapeHtml(message)}</div>
    `;

    container.appendChild(toast);

    setTimeout(() => {
      toast.style.transition = 'all 0.3s ease-out';
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(50px)';
      setTimeout(() => toast.remove(), 300);
    }, 3500);
  }

  // --- Helper Functions ---
  function getGradeInfo(marks) {
    if (marks >= 80) return { letter: 'A+', color: '#10b981', badgeClass: 'grade-pill-A' };
    if (marks >= 65) return { letter: 'B', color: '#3b82f6', badgeClass: 'grade-pill-B' };
    if (marks >= 50) return { letter: 'C', color: '#f59e0b', badgeClass: 'grade-pill-C' };
    if (marks >= 40) return { letter: 'D', color: '#fb923c', badgeClass: 'grade-pill-D' };
    return { letter: 'F', color: '#ef4444', badgeClass: 'grade-pill-F' };
  }

  function getAvatarColor(name) {
    const palette = [
      'linear-gradient(135deg, #6366f1, #a855f7)',
      'linear-gradient(135deg, #3b82f6, #06b6d4)',
      'linear-gradient(135deg, #10b981, #059669)',
      'linear-gradient(135deg, #f59e0b, #d97706)',
      'linear-gradient(135deg, #ec4899, #f43f5e)',
    ];
    let hash = 0;
    for (let i = 0; i < (name || '').length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
    return palette[Math.abs(hash) % palette.length];
  }

  function getInitials(name) {
    if (!name) return 'S';
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].substring(0, 2).toUpperCase();
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }

  function formatPhone(phone) {
    if (!phone || phone.length !== 10) return phone || '';
    return `${phone.substring(0, 5)} ${phone.substring(5)}`;
  }

  function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  window.copyToClipboard = function (text) {
    navigator.clipboard.writeText(text).then(
      () => showToast(`Copied ${text} to clipboard`, 'info'),
      () => showToast('Failed to copy', 'error')
    );
  };
})();
