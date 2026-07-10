/* ==========================================================================
   validation.js — Client-side input validation for the customer form
   ========================================================================== */

const Validation = {
  msisdnPattern: /^\d{10,20}$/,

  validateMsisdn(value) {
    if (!value || value.trim() === '') {
      return { valid: false, message: 'MSISDN is required.' };
    }
    const cleaned = value.trim();
    if (!this.msisdnPattern.test(cleaned)) {
      return { valid: false, message: 'Enter 10-20 digits, numbers only.' };
    }
    return { valid: true, message: '' };
  },

  validateBalance(value) {
    if (!value || value.trim() === '') {
      return { valid: false, message: 'Balance is required.' };
    }
    const num = Number(value);
    if (Number.isNaN(num)) {
      return { valid: false, message: 'Enter a valid number.' };
    }
    if (num < 0) {
      return { valid: false, message: 'Balance cannot be negative.' };
    }
    if (!/^\d+(\.\d{1,2})?$/.test(value.trim())) {
      return { valid: false, message: 'Use up to 2 decimal places.' };
    }
    return { valid: true, message: '' };
  },

  showFieldError(inputEl, errorEl, message) {
    inputEl.classList.add('invalid');
    inputEl.classList.remove('valid');
    errorEl.querySelector('span').textContent = message;
    errorEl.classList.add('show');
  },

  showFieldValid(inputEl, errorEl) {
    inputEl.classList.remove('invalid');
    inputEl.classList.add('valid');
    errorEl.classList.remove('show');
  },

  clearFieldState(inputEl, errorEl) {
    inputEl.classList.remove('invalid', 'valid');
    errorEl.classList.remove('show');
  }
};
