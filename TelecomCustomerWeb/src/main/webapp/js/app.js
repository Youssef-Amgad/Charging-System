/* ==========================================================================
   app.js — Application logic: navigation, data fetching, CRUD, search,
   sorting, pagination, modals, toasts.
   ========================================================================== */

(function () {
  'use strict';

  const API = {
    dashboard: 'api/dashboard',
    list: 'api/customers',
    search: 'api/customers/search',
    add: 'api/customers/add',
    edit: 'api/customers/edit',
    del: 'api/customers/delete'
  };

  const state = {
    page: 1,
    pageSize: 10,
    sortColumn: 'msisdn',
    sortDir: 'asc',
    searchTerm: '',
    totalPages: 1
  };

  let debounceTimer = null;

  // ---------------------------------------------------------------------
  // Navigation between Dashboard / Customers sections
  // ---------------------------------------------------------------------
  const navItems = document.querySelectorAll('.nav-item');
  const pageTitle = document.getElementById('pageTitle');
  const pageSubtitle = document.getElementById('pageSubtitle');

  const pageMeta = {
    dashboard: { title: 'Dashboard', subtitle: 'Live overview of prepaid customer balances' },
    customers: { title: 'Customers', subtitle: 'Full CRUD management for public.customers' }
  };

  function showPage(pageKey) {
    document.querySelectorAll('.page-section').forEach(sec => sec.classList.remove('active'));
    document.getElementById('page-' + pageKey).classList.add('active');
    navItems.forEach(item => item.classList.toggle('active', item.dataset.page === pageKey));
    pageTitle.textContent = pageMeta[pageKey].title;
    pageSubtitle.textContent = pageMeta[pageKey].subtitle;

    if (pageKey === 'customers') {
      loadCustomerTable();
    } else {
      loadDashboard();
    }
  }

  navItems.forEach(item => {
    item.addEventListener('click', e => {
      e.preventDefault();
      showPage(item.dataset.page);
    });
  });

  document.getElementById('qaViewAll').addEventListener('click', () => showPage('customers'));

  // ---------------------------------------------------------------------
  // Toasts
  // ---------------------------------------------------------------------
  const toastContainer = document.getElementById('toastContainer');

  const toastIcons = {
    success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
    error: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
    info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>'
  };

  function showToast(type, title, message) {
    const toast = document.createElement('div');
    toast.className = 'toast ' + type;
    toast.innerHTML = `
      <div class="toast-icon">${toastIcons[type] || toastIcons.info}</div>
      <div class="toast-body">
        <strong>${escapeHtml(title)}</strong>
        <span>${escapeHtml(message || '')}</span>
      </div>
      <button class="toast-close"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg></button>
    `;
    toastContainer.appendChild(toast);

    const remove = () => {
      toast.classList.add('toast-out');
      setTimeout(() => toast.remove(), 220);
    };
    toast.querySelector('.toast-close').addEventListener('click', remove);
    setTimeout(remove, 4500);
  }

  function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str == null ? '' : String(str);
    return div.innerHTML;
  }

  // ---------------------------------------------------------------------
  // Formatting helpers
  // ---------------------------------------------------------------------
  function formatMoney(value) {
    const num = Number(value || 0);
    return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' L.E.';
  }

  function balanceClass(value) {
    const num = Number(value || 0);
    if (num < 10) return 'balance-low';
    if (num < 50) return 'balance-mid';
    return 'balance-high';
  }

  async function fetchJson(url, options) {
    const response = await fetch(url, options);
    let data;
    try {
      data = await response.json();
    } catch (e) {
      throw new Error('Unexpected server response.');
    }
    if (!response.ok || data.success === false || data.error) {
      throw new Error(data.error || 'Request failed.');
    }
    return data;
  }

  // ---------------------------------------------------------------------
  // Dashboard
  // ---------------------------------------------------------------------
  async function loadDashboard() {
    try {
      const stats = await fetchJson(API.dashboard);
      document.getElementById('statTotal').textContent = stats.totalCustomers;
      document.getElementById('statTotalBalance').textContent = formatMoney(stats.totalBalance);
      document.getElementById('statAvgBalance').textContent = formatMoney(stats.averageBalance);
      document.getElementById('statHighBalance').textContent = formatMoney(stats.highestBalance);
      document.getElementById('statLowBalance').textContent = formatMoney(stats.lowestBalance);
    } catch (err) {
      showToast('error', 'Could not load dashboard', err.message);
    }

    try {
      const listData = await fetchJson(`${API.list}?sort=msisdn&dir=asc&page=1&size=5`);
      renderDashTable(listData.customers);
    } catch (err) {
      showToast('error', 'Could not load recent customers', err.message);
    }
  }

  function renderDashTable(customers) {
    const body = document.getElementById('dashTableBody');
    if (!customers || customers.length === 0) {
      body.innerHTML = `<tr><td colspan="2">${emptyStateHtml('No customers yet', 'Add your first prepaid customer to see it here.')}</td></tr>`;
      return;
    }
    body.innerHTML = customers.map(c => `
      <tr>
        <td><span class="cell-msisdn"><span class="msisdn-dot"></span>${escapeHtml(c.msisdn)}</span></td>
        <td><span class="cell-balance ${balanceClass(c.balance)}">${formatMoney(c.balance)}</span></td>
      </tr>
    `).join('');
  }

  function emptyStateHtml(title, message) {
    return `
      <div class="empty-state">
        <svg class="empty-illustration" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2">
          <path d="M2 20h.01M6 20h.01M2 16a10 10 0 0 1 20 0"/>
          <path d="M9 20a3 3 0 0 1 6 0"/>
          <line x1="4" y1="4" x2="20" y2="20"/>
        </svg>
        <h3>${title}</h3>
        <p>${message}</p>
      </div>
    `;
  }

  document.getElementById('refreshBtn').addEventListener('click', () => {
    document.getElementById('page-dashboard').classList.contains('active') ? loadDashboard() : loadCustomerTable();
    showToast('info', 'Refreshed', 'Latest data has been loaded.');
  });
  document.getElementById('qaRefresh2').addEventListener('click', loadDashboard);
  document.getElementById('qaAddCustomer').addEventListener('click', () => openCustomerModal('add'));

  // ---------------------------------------------------------------------
  // Customers table: fetch + render + sorting + pagination
  // ---------------------------------------------------------------------
  async function loadCustomerTable() {
    const tbody = document.getElementById('customersTableBody');
    tbody.innerHTML = skeletonRows(state.pageSize > 8 ? 6 : state.pageSize);

    const params = new URLSearchParams({
      q: state.searchTerm,
      sort: state.sortColumn,
      dir: state.sortDir,
      page: state.page,
      size: state.pageSize
    });

    const endpoint = state.searchTerm ? API.search : API.list;

    try {
      const data = await fetchJson(`${endpoint}?${params.toString()}`);
      state.totalPages = data.totalPages;
      renderCustomerTable(data.customers);
      renderPagination(data);
      updateSortIndicators();
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="3">${emptyStateHtml('Could not reach the database', err.message)}</td></tr>`;
      document.getElementById('tableSummary').textContent = 'Connection error';
    }
  }

  function skeletonRows(count) {
    let rows = '';
    for (let i = 0; i < count; i++) {
      rows += `
        <tr class="skeleton-row">
          <td><div class="skeleton-bar" style="width:60%"></div></td>
          <td><div class="skeleton-bar" style="width:40%"></div></td>
          <td><div class="skeleton-bar" style="width:70px"></div></td>
        </tr>`;
    }
    return rows;
  }

  function renderCustomerTable(customers) {
    const tbody = document.getElementById('customersTableBody');
    if (!customers || customers.length === 0) {
      tbody.innerHTML = `<tr><td colspan="3">${emptyStateHtml('No matching customers', state.searchTerm ? `No results for "${escapeHtml(state.searchTerm)}".` : 'Add your first prepaid customer to get started.')}</td></tr>`;
      return;
    }
    tbody.innerHTML = customers.map(c => `
      <tr>
        <td><span class="cell-msisdn"><span class="msisdn-dot"></span>${escapeHtml(c.msisdn)}</span></td>
        <td><span class="cell-balance ${balanceClass(c.balance)}">${formatMoney(c.balance)}</span></td>
        <td>
          <div class="row-actions">
            <button class="row-action-btn edit" data-msisdn="${escapeHtml(c.msisdn)}" data-balance="${c.balance}" title="Edit">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4z"/></svg>
            </button>
            <button class="row-action-btn delete" data-msisdn="${escapeHtml(c.msisdn)}" title="Delete">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6M14 11v6M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>
            </button>
          </div>
        </td>
      </tr>
    `).join('');

    tbody.querySelectorAll('.row-action-btn.edit').forEach(btn => {
      btn.addEventListener('click', () => openCustomerModal('edit', btn.dataset.msisdn, btn.dataset.balance));
    });
    tbody.querySelectorAll('.row-action-btn.delete').forEach(btn => {
      btn.addEventListener('click', () => openDeleteModal(btn.dataset.msisdn));
    });
  }

  function renderPagination(data) {
    document.getElementById('tableSummary').textContent =
      `${data.totalRecords} customer${data.totalRecords === 1 ? '' : 's'} total`;
    document.getElementById('paginationInfo').textContent =
      `Page ${data.page} of ${data.totalPages}`;

    const controls = document.getElementById('paginationControls');
    controls.innerHTML = '';

    const prevBtn = document.createElement('button');
    prevBtn.className = 'page-btn';
    prevBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>';
    prevBtn.disabled = data.page <= 1;
    prevBtn.addEventListener('click', () => { state.page--; loadCustomerTable(); });
    controls.appendChild(prevBtn);

    const maxButtons = 5;
    let start = Math.max(1, data.page - Math.floor(maxButtons / 2));
    let end = Math.min(data.totalPages, start + maxButtons - 1);
    start = Math.max(1, end - maxButtons + 1);

    for (let p = start; p <= end; p++) {
      const btn = document.createElement('button');
      btn.className = 'page-btn' + (p === data.page ? ' active' : '');
      btn.textContent = p;
      btn.addEventListener('click', () => { state.page = p; loadCustomerTable(); });
      controls.appendChild(btn);
    }

    const nextBtn = document.createElement('button');
    nextBtn.className = 'page-btn';
    nextBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>';
    nextBtn.disabled = data.page >= data.totalPages;
    nextBtn.addEventListener('click', () => { state.page++; loadCustomerTable(); });
    controls.appendChild(nextBtn);
  }

  function updateSortIndicators() {
    document.querySelectorAll('#customersTable th[data-sort]').forEach(th => {
      th.classList.toggle('sort-active', th.dataset.sort === state.sortColumn);
      th.classList.toggle('sort-desc', th.dataset.sort === state.sortColumn && state.sortDir === 'desc');
    });
  }

  document.querySelectorAll('#customersTable th[data-sort]').forEach(th => {
    th.addEventListener('click', () => {
      const column = th.dataset.sort;
      if (state.sortColumn === column) {
        state.sortDir = state.sortDir === 'asc' ? 'desc' : 'asc';
      } else {
        state.sortColumn = column;
        state.sortDir = 'asc';
      }
      state.page = 1;
      loadCustomerTable();
    });
  });

  document.getElementById('pageSizeSelect').addEventListener('change', e => {
    state.pageSize = parseInt(e.target.value, 10);
    state.page = 1;
    loadCustomerTable();
  });

  // ---------------------------------------------------------------------
  // Live search (both dashboard quick search and full table search)
  // ---------------------------------------------------------------------
  function wireSearch(inputId, clearId, onSearch) {
    const input = document.getElementById(inputId);
    const clearBtn = document.getElementById(clearId);

    input.addEventListener('input', () => {
      clearBtn.style.display = input.value ? 'flex' : 'none';
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => onSearch(input.value.trim()), 300);
    });

    clearBtn.addEventListener('click', () => {
      input.value = '';
      clearBtn.style.display = 'none';
      onSearch('');
      input.focus();
    });
  }

  wireSearch('tableSearchInput', 'tableSearchClear', term => {
    state.searchTerm = term;
    state.page = 1;
    loadCustomerTable();
  });

  wireSearch('dashSearchInput', 'dashSearchClear', async term => {
    try {
      const params = new URLSearchParams({ q: term, sort: 'msisdn', dir: 'asc', page: 1, size: 8 });
      const data = await fetchJson(`${API.search}?${params.toString()}`);
      renderDashTable(data.customers);
    } catch (err) {
      showToast('error', 'Search failed', err.message);
    }
  });

  // ---------------------------------------------------------------------
  // Add / Edit modal
  // ---------------------------------------------------------------------
  const customerModalBackdrop = document.getElementById('customerModalBackdrop');
  const customerForm = document.getElementById('customerForm');
  const msisdnInput = document.getElementById('msisdnInput');
  const balanceInput = document.getElementById('balanceInput');
  const msisdnGroup = document.getElementById('msisdnGroup');
  const msisdnLockedGroup = document.getElementById('msisdnLockedGroup');
  const msisdnLockedValue = document.getElementById('msisdnLockedValue');
  const formMode = document.getElementById('formMode');

  function openCustomerModal(mode, msisdn, balance) {
    formMode.value = mode;
    customerForm.reset();
    Validation.clearFieldState(msisdnInput, document.getElementById('msisdnError'));
    Validation.clearFieldState(balanceInput, document.getElementById('balanceError'));

    if (mode === 'add') {
      document.getElementById('customerModalTitle').textContent = 'Add Customer';
      document.getElementById('customerModalSubtitle').textContent = 'Create a new prepaid account';
      msisdnGroup.style.display = 'block';
      msisdnLockedGroup.style.display = 'none';
      msisdnInput.value = '';
      balanceInput.value = '';
    } else {
      document.getElementById('customerModalTitle').textContent = 'Edit Balance';
      document.getElementById('customerModalSubtitle').textContent = 'MSISDN stays fixed — only balance can change';
      msisdnGroup.style.display = 'none';
      msisdnLockedGroup.style.display = 'block';
      msisdnLockedValue.textContent = '+' + msisdn;
      msisdnInput.value = msisdn;
      balanceInput.value = balance;
    }

    customerModalBackdrop.classList.add('show');
    setTimeout(() => (mode === 'add' ? msisdnInput : balanceInput).focus(), 200);
  }

  function closeCustomerModal() {
    customerModalBackdrop.classList.remove('show');
  }

  document.getElementById('openAddModalBtn').addEventListener('click', () => openCustomerModal('add'));
  document.getElementById('closeCustomerModal').addEventListener('click', closeCustomerModal);
  document.getElementById('cancelCustomerModal').addEventListener('click', closeCustomerModal);
  customerModalBackdrop.addEventListener('click', e => { if (e.target === customerModalBackdrop) closeCustomerModal(); });

  // Live inline validation
  msisdnInput.addEventListener('input', () => {
    const result = Validation.validateMsisdn(msisdnInput.value);
    const errorEl = document.getElementById('msisdnError');
    if (msisdnInput.value.trim() === '') { Validation.clearFieldState(msisdnInput, errorEl); return; }
    result.valid ? Validation.showFieldValid(msisdnInput, errorEl) : Validation.showFieldError(msisdnInput, errorEl, result.message);
  });

  balanceInput.addEventListener('input', () => {
    const result = Validation.validateBalance(balanceInput.value);
    const errorEl = document.getElementById('balanceError');
    if (balanceInput.value.trim() === '') { Validation.clearFieldState(balanceInput, errorEl); return; }
    result.valid ? Validation.showFieldValid(balanceInput, errorEl) : Validation.showFieldError(balanceInput, errorEl, result.message);
  });

  customerForm.addEventListener('submit', async e => {
    e.preventDefault();
    const mode = formMode.value;

    const msisdnResult = Validation.validateMsisdn(msisdnInput.value);
    const balanceResult = Validation.validateBalance(balanceInput.value);

    if (mode === 'add' && !msisdnResult.valid) {
      Validation.showFieldError(msisdnInput, document.getElementById('msisdnError'), msisdnResult.message);
    }
    if (!balanceResult.valid) {
      Validation.showFieldError(balanceInput, document.getElementById('balanceError'), balanceResult.message);
    }
    if ((mode === 'add' && !msisdnResult.valid) || !balanceResult.valid) return;

    const submitBtn = document.getElementById('submitCustomerBtn');
    const submitText = document.getElementById('submitBtnText');
    const spinner = document.getElementById('submitSpinner');
    submitBtn.disabled = true;
    spinner.classList.remove('hidden');
    submitText.textContent = mode === 'add' ? 'Saving…' : 'Updating…';

    const formData = new URLSearchParams();
    formData.set('msisdn', msisdnInput.value.trim());
    formData.set('balance', balanceInput.value.trim());

    try {
      const url = mode === 'add' ? API.add : API.edit;
      const data = await fetchJson(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData.toString()
      });
      showToast('success', mode === 'add' ? 'Customer added' : 'Balance updated', data.message);
      closeCustomerModal();
      loadCustomerTable();
      loadDashboard();
    } catch (err) {
      showToast('error', mode === 'add' ? 'Could not add customer' : 'Could not update customer', err.message);
    } finally {
      submitBtn.disabled = false;
      spinner.classList.add('hidden');
      submitText.textContent = 'Save Customer';
    }
  });

  // ---------------------------------------------------------------------
  // Delete modal
  // ---------------------------------------------------------------------
  const deleteModalBackdrop = document.getElementById('deleteModalBackdrop');
  const deleteMsisdnDisplay = document.getElementById('deleteMsisdnDisplay');
  let pendingDeleteMsisdn = null;

  function openDeleteModal(msisdn) {
    pendingDeleteMsisdn = msisdn;
    deleteMsisdnDisplay.textContent = '+' + msisdn;
    deleteModalBackdrop.classList.add('show');
  }
  function closeDeleteModal() {
    deleteModalBackdrop.classList.remove('show');
    pendingDeleteMsisdn = null;
  }

  document.getElementById('closeDeleteModal').addEventListener('click', closeDeleteModal);
  document.getElementById('cancelDeleteBtn').addEventListener('click', closeDeleteModal);
  deleteModalBackdrop.addEventListener('click', e => { if (e.target === deleteModalBackdrop) closeDeleteModal(); });

  document.getElementById('confirmDeleteBtn').addEventListener('click', async () => {
    if (!pendingDeleteMsisdn) return;
    const btn = document.getElementById('confirmDeleteBtn');
    const text = document.getElementById('deleteBtnText');
    const spinner = document.getElementById('deleteSpinner');
    btn.disabled = true;
    spinner.classList.remove('hidden');
    text.textContent = 'Deleting…';

    const formData = new URLSearchParams();
    formData.set('msisdn', pendingDeleteMsisdn);

    try {
      const data = await fetchJson(API.del, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formData.toString()
      });
      showToast('success', 'Customer deleted', data.message);
      closeDeleteModal();
      loadCustomerTable();
      loadDashboard();
    } catch (err) {
      showToast('error', 'Could not delete customer', err.message);
    } finally {
      btn.disabled = false;
      spinner.classList.add('hidden');
      text.textContent = 'Delete Permanently';
    }
  });

  // ---------------------------------------------------------------------
  // Ripple effect for all .btn elements
  // ---------------------------------------------------------------------
  document.addEventListener('click', e => {
    const btn = e.target.closest('.btn');
    if (!btn) return;
    const rect = btn.getBoundingClientRect();
    const ripple = document.createElement('span');
    const size = Math.max(rect.width, rect.height);
    ripple.className = 'ripple';
    ripple.style.width = ripple.style.height = size + 'px';
    ripple.style.left = (e.clientX - rect.left - size / 2) + 'px';
    ripple.style.top = (e.clientY - rect.top - size / 2) + 'px';
    btn.appendChild(ripple);
    setTimeout(() => ripple.remove(), 600);
  });

  // Escape key closes any open modal
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape') {
      closeCustomerModal();
      closeDeleteModal();
    }
  });

  // ---------------------------------------------------------------------
  // Initial load
  // ---------------------------------------------------------------------
  loadDashboard();
})();
