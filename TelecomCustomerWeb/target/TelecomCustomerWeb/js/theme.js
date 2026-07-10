/* ==========================================================================
   theme.js — Dark/Light mode toggle + responsive sidebar behavior
   ========================================================================== */

(function () {
  const THEME_KEY = 'netops.theme';
  const body = document.body;
  const themeToggle = document.getElementById('themeToggle');
  const themeIcon = document.getElementById('themeIcon');

  const sunPath = '<circle cx="12" cy="12" r="4"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>';
  const moonPath = '<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>';

  function applyTheme(theme) {
    if (theme === 'light') {
      body.classList.add('light-mode');
      themeIcon.innerHTML = sunPath;
    } else {
      body.classList.remove('light-mode');
      themeIcon.innerHTML = moonPath;
    }
  }

  function getSavedTheme() {
    try {
      return localStorage.getItem(THEME_KEY);
    } catch (e) {
      return null;
    }
  }

  function saveTheme(theme) {
    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch (e) {
      /* storage unavailable — theme just won't persist across reloads */
    }
  }

  // Initialize: saved preference > OS preference > dark default
  const saved = getSavedTheme();
  if (saved) {
    applyTheme(saved);
  } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) {
    applyTheme('light');
  } else {
    applyTheme('dark');
  }

  themeToggle.addEventListener('click', () => {
    const isLight = body.classList.contains('light-mode');
    const next = isLight ? 'dark' : 'light';
    applyTheme(next);
    saveTheme(next);
  });

  // ---- Responsive sidebar ----
  const sidebar = document.getElementById('sidebar');
  const menuToggle = document.getElementById('menuToggle');
  const scrim = document.getElementById('sidebarScrim');

  function openSidebar() {
    sidebar.classList.add('open');
    scrim.classList.add('show');
  }
  function closeSidebar() {
    sidebar.classList.remove('open');
    scrim.classList.remove('show');
  }

  menuToggle.addEventListener('click', () => {
    sidebar.classList.contains('open') ? closeSidebar() : openSidebar();
  });
  scrim.addEventListener('click', closeSidebar);

  document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', closeSidebar);
  });

  // ---- Page loader fade-out ----
  window.addEventListener('load', () => {
    setTimeout(() => {
      document.getElementById('pageLoader').classList.add('loaded');
    }, 350);
  });
})();
