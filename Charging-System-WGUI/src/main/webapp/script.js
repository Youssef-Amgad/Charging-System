/* =========================================================
 Charging System Voice — script.js
 Now connected to Java Servlets backend with PostgreSQL.
 -----------------------------------------------------------
 All data operations use fetch() calls to REST endpoints.
 ========================================================= */

(function () {
    "use strict";

    /* ---------------------------------------------------------
     1. DATA LAYER - Now uses Java Servlets via fetch()
     --------------------------------------------------------- */
    const SubscriberService = (function () {
        // Base URL for servlets - adjust to your context path
        const BASE_URL = '/Charging-System-WGUI'; // Change to your app context
        
        return {
            // GET all subscribers - calls servlet that returns JSON
            async getAll() {
                try {
                    const response = await fetch(`${BASE_URL}/SubscriberServlet`);
                    if (!response.ok) throw new Error('Failed to fetch subscribers');
                    return await response.json();
                } catch (error) {
                    console.error('Error fetching subscribers:', error);
                    return [];
                }
            },
            
            // GET single subscriber by MSISDN
            async findByMsisdn(msisdn) {
                try {
                    const response = await fetch(`${BASE_URL}/SubscriberServlet?msisdn=${encodeURIComponent(msisdn)}`);
                    if (!response.ok) throw new Error('Failed to fetch subscriber');
                    return await response.json();
                } catch (error) {
                    console.error('Error fetching subscriber:', error);
                    return null;
                }
            },
            
            // POST - Add new subscriber
            async add(subscriber) {
                try {
                    const response = await fetch(`${BASE_URL}/SubscriberServlet`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({
                            action: 'add',
                            msisdn: subscriber.msisdn,
                            balance: subscriber.balance
                        })
                    });
                    if (!response.ok) throw new Error('Failed to add subscriber');
                    return true;
                } catch (error) {
                    console.error('Error adding subscriber:', error);
                    return false;
                }
            },
            
            // PUT - Update existing subscriber
            async update(msisdn, newData) {
                try {
                    const response = await fetch(`${BASE_URL}/SubscriberServlet`, {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({
                            action: 'update',
                            oldMsisdn: msisdn,
                            newMsisdn: newData.msisdn,
                            balance: newData.balance
                        })
                    });
                    if (!response.ok) throw new Error('Failed to update subscriber');
                    return true;
                } catch (error) {
                    console.error('Error updating subscriber:', error);
                    return false;
                }
            },
            
            // DELETE - Remove subscriber
            async remove(msisdn) {
                try {
                    const response = await fetch(`${BASE_URL}/SubscriberServlet?msisdn=${encodeURIComponent(msisdn)}`, {
                        method: 'DELETE'
                    });
                    if (!response.ok) throw new Error('Failed to delete subscriber');
                    return true;
                } catch (error) {
                    console.error('Error deleting subscriber:', error);
                    return false;
                }
            },
        };
    })();

    /* ---------------------------------------------------------
     2. DOM REFERENCES
     --------------------------------------------------------- */
    const form = document.getElementById("subscriberForm");
    const msisdnInput = document.getElementById("msisdn");
    const balanceInput = document.getElementById("balance");
    const msisdnError = document.getElementById("msisdnError");
    const balanceError = document.getElementById("balanceError");

    const addBtn = document.getElementById("addBtn");
    const updateBtn = document.getElementById("updateBtn");
    const deleteBtn = document.getElementById("deleteBtn");
    const clearBtn = document.getElementById("clearBtn");

    const formModeLabel = document.getElementById("form-mode");
    const tableBody = document.getElementById("subscriberTableBody");
    const emptyState = document.getElementById("emptyState");
    const countLabel = document.getElementById("countLabel");
    const searchInput = document.getElementById("searchInput");
    const loaderOverlay = document.getElementById("loaderOverlay");

    /* Tracks which subscriber (by original MSISDN) is loaded
     into the form for editing. Null = "new entry" mode. */
    let selectedMsisdn = null;

    /* ---------------------------------------------------------
     LOADER FUNCTIONS
     --------------------------------------------------------- */
    function showLoader() {
        if (loaderOverlay) {
            loaderOverlay.classList.add("active");
        }
    }

    function hideLoader() {
        if (loaderOverlay) {
            loaderOverlay.classList.remove("active");
        }
    }

    /* ---------------------------------------------------------
     3. VALIDATION - Kept exactly as before
     --------------------------------------------------------- */
    function validateMsisdn(value, { forUpdateOriginal = null } = {}) {
        const trimmed = (value || "").trim();

        if (!trimmed) {
            return {valid: false, message: "MSISDN is required."};
        }
        if (!/^\d+$/.test(trimmed)) {
            return {valid: false, message: "MSISDN must contain numbers only."};
        }
        if (trimmed.length !== 10 && trimmed.length !== 11) {
            return {valid: false, message: "MSISDN must be exactly 10 or 11 digits."};
        }

        return {valid: true, message: ""};
    }

    function validateBalance(value) {
        const trimmed = (value || "").trim();

        if (!trimmed) {
            return {valid: false, message: "Balance is required."};
        }
        if (!/^\d+(\.\d{1,2})?$/.test(trimmed)) {
            return {valid: false, message: "Balance must be numbers only."};
        }

        const num = Number(trimmed);

        if (num < 0) {
            return {valid: false, message: "Balance cannot be negative."};
        }
        if (num > 999999) {
            return {valid: false, message: "Balance cannot exceed 999,999."};
        }

        return {valid: true, message: ""};
    }

    function showFieldError(inputEl, errorEl, message) {
        if (message) {
            inputEl.classList.add("invalid");
            errorEl.textContent = message;
        } else {
            inputEl.classList.remove("invalid");
            errorEl.textContent = "";
        }
    }

    function clearFieldErrors() {
        showFieldError(msisdnInput, msisdnError, "");
        showFieldError(balanceInput, balanceError, "");
    }

    /* ---------------------------------------------------------
     4. FORM STATE HELPERS
     --------------------------------------------------------- */
    function enterEditMode(subscriber) {
        selectedMsisdn = subscriber.msisdn;
        msisdnInput.value = subscriber.msisdn;
        balanceInput.value = subscriber.balance;
        clearFieldErrors();

        formModeLabel.textContent = `Editing ${subscriber.msisdn}`;
        addBtn.disabled = true;
        updateBtn.disabled = false;
        deleteBtn.disabled = false;

        highlightSelectedRow(subscriber.msisdn);
        msisdnInput.focus();
    }

    function resetForm() {
        selectedMsisdn = null;
        form.reset();
        clearFieldErrors();

        formModeLabel.textContent = "New entry";
        addBtn.disabled = false;
        updateBtn.disabled = true;
        deleteBtn.disabled = true;

        highlightSelectedRow(null);
    }

    function highlightSelectedRow(msisdn) {
        document.querySelectorAll(".sub-table tbody tr").forEach((row) => {
            row.classList.toggle("is-selected", row.dataset.msisdn === msisdn);
        });
    }

    /* ---------------------------------------------------------
     5. RENDERING - Now uses async data from servlet
     --------------------------------------------------------- */
    function getBalanceTier(balance) {
        if (balance <= 0)
            return 0;
        if (balance < 50)
            return 1;
        if (balance < 500)
            return 2;
        if (balance < 5000)
            return 3;
        return 4;
    }

    function formatBalance(balance) {
        return Number(balance).toLocaleString("en-US", {
            minimumFractionDigits: 0,
            maximumFractionDigits: 2,
        });
    }

    async function renderTable(filterText = "") {
        const query = filterText.trim();
        const all = await SubscriberService.getAll();
        const filtered = query
                ? all.filter((s) => s.msisdn.includes(query))
                : all;

        tableBody.innerHTML = "";

        filtered.forEach((sub) => {
            const tier = getBalanceTier(sub.balance);
            const row = document.createElement("tr");
            row.dataset.msisdn = sub.msisdn;

            row.innerHTML = `
        <td data-label="MSISDN" class="msisdn-cell">${sub.msisdn}</td>
        <td data-label="Balance">
          <div class="balance-cell">
            <span class="balance-value">${formatBalance(sub.balance)}</span>
            <span class="signal-bars" data-tier="${tier}" title="Balance tier">
              <span></span><span></span><span></span><span></span>
            </span>
          </div>
        </td>
        <td data-label="Actions" class="actions-cell">
          <button type="button" class="btn btn--accent btn--icon-sm" data-action="edit" data-msisdn="${sub.msisdn}">Edit</button>
          <button type="button" class="btn btn--danger btn--icon-sm" data-action="delete" data-msisdn="${sub.msisdn}">Delete</button>
        </td>
      `;

            tableBody.appendChild(row);
        });

        emptyState.classList.toggle("show", filtered.length === 0);
        countLabel.textContent =
                all.length === 1 ? "1 record" : `${all.length} records`;

        if (selectedMsisdn)
            highlightSelectedRow(selectedMsisdn);
    }

    /* ---------------------------------------------------------
     6. EVENT HANDLERS - Now use async/await for servlet calls
     --------------------------------------------------------- */
    addBtn.addEventListener("click", async () => {
        const msisdnResult = validateMsisdn(msisdnInput.value);
        const balanceResult = validateBalance(balanceInput.value);

        showFieldError(msisdnInput, msisdnError, msisdnResult.message);
        showFieldError(balanceInput, balanceError, balanceResult.message);

        if (!msisdnResult.valid || !balanceResult.valid)
            return;

        showLoader();

        // Check if MSISDN already exists in DB
        const existing = await SubscriberService.findByMsisdn(msisdnInput.value.trim());
        if (existing) {
            showFieldError(msisdnInput, msisdnError, "This MSISDN already exists.");
            hideLoader();
            return;
        }

        const success = await SubscriberService.add({
            msisdn: msisdnInput.value.trim(),
            balance: Number(balanceInput.value.trim()),
        });

        hideLoader();

        if (success) {
            await renderTable(searchInput.value);
            resetForm();
        } else {
            alert('Failed to add subscriber. Please try again.');
        }
    });

    updateBtn.addEventListener("click", async () => {
        if (!selectedMsisdn)
            return;

        const msisdnResult = validateMsisdn(msisdnInput.value);
        const balanceResult = validateBalance(balanceInput.value);

        showFieldError(msisdnInput, msisdnError, msisdnResult.message);
        showFieldError(balanceInput, balanceError, balanceResult.message);

        if (!msisdnResult.valid || !balanceResult.valid)
            return;

        showLoader();

        // If MSISDN changed, check if new MSISDN already exists
        if (selectedMsisdn !== msisdnInput.value.trim()) {
            const existing = await SubscriberService.findByMsisdn(msisdnInput.value.trim());
            if (existing) {
                showFieldError(msisdnInput, msisdnError, "This MSISDN already exists.");
                hideLoader();
                return;
            }
        }

        const success = await SubscriberService.update(selectedMsisdn, {
            msisdn: msisdnInput.value.trim(),
            balance: Number(balanceInput.value.trim()),
        });

        hideLoader();

        if (success) {
            await renderTable(searchInput.value);
            resetForm();
        } else {
            alert('Failed to update subscriber. Please try again.');
        }
    });

    deleteBtn.addEventListener("click", async () => {
        if (!selectedMsisdn)
            return;

        const confirmed = window.confirm(
                `Delete subscriber ${selectedMsisdn}? This cannot be undone.`
                );
        if (!confirmed)
            return;

        showLoader();

        const success = await SubscriberService.remove(selectedMsisdn);
        
        hideLoader();

        if (success) {
            await renderTable(searchInput.value);
            resetForm();
        } else {
            alert('Failed to delete subscriber. Please try again.');
        }
    });

    clearBtn.addEventListener("click", resetForm);

    // Live-clear the "already exists" / format errors as the user types.
    msisdnInput.addEventListener("input", () => {
        if (msisdnInput.classList.contains("invalid")) {
            showFieldError(msisdnInput, msisdnError, "");
        }
    });
    balanceInput.addEventListener("input", () => {
        if (balanceInput.classList.contains("invalid")) {
            showFieldError(balanceInput, balanceError, "");
        }
    });

    // Row actions (event delegation — works for dynamically added rows).
    tableBody.addEventListener("click", async (e) => {
        const btn = e.target.closest("button[data-action]");
        if (!btn)
            return;

        e.preventDefault();
        e.stopPropagation();

        const msisdn = btn.dataset.msisdn;
        const action = btn.dataset.action;

        if (action === "edit") {
            showLoader();
            const subscriber = await SubscriberService.findByMsisdn(msisdn);
            hideLoader();
            if (subscriber) {
                enterEditMode(subscriber);
            } else {
                alert('Failed to load subscriber details. Please try again.');
            }
        } else if (action === "delete") {
            const confirmed = window.confirm(`Delete subscriber ${msisdn}? This cannot be undone.`);
            if (!confirmed)
                return;
            
            showLoader();
            const success = await SubscriberService.remove(msisdn);
            hideLoader();

            if (success) {
                if (selectedMsisdn === msisdn)
                    resetForm();
                await renderTable(searchInput.value);
            } else {
                alert('Failed to delete subscriber. Please try again.');
            }
        }
    });

    searchInput.addEventListener("input", () => {
        renderTable(searchInput.value);
    });

    /* ---------------------------------------------------------
     7. INIT - Load subscribers from DB on page load
     --------------------------------------------------------- */
    async function init() {
        await renderTable();
        resetForm();
    }
    
    init();
})();