/* Shared helpers: API calls with fetch(), HTML escaping, formatting, toasts, tabs. */

const api = {
  async request(method, url, body) {
    const options = { method, headers: { Accept: 'application/json' }, credentials: 'same-origin' };
    if (body !== undefined) {
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    }

    let response;
    try {
      response = await fetch(url, options);
    } catch (e) {
      throw new Error('Cannot reach the server. Is the application running?');
    }

    const text = await response.text();
    let data = null;
    if (text) {
      try { data = JSON.parse(text); } catch (e) { data = { error: text }; }
    }

    if (!response.ok) {
      // Session expired or not logged in -> back to the login page (except on the login call itself).
      if (response.status === 401 && !url.startsWith('/api/auth/')) {
        window.location.href = '/index.html';
      }
      throw new Error((data && data.error) || `Request failed (${response.status})`);
    }
    return data;
  },
  get(url) { return this.request('GET', url); },
  post(url, body) { return this.request('POST', url, body); },
  put(url, body) { return this.request('PUT', url, body); },
  patch(url, body) { return this.request('PATCH', url, body); },
  del(url) { return this.request('DELETE', url); },
};

/** Escape text before putting it into innerHTML (prevents XSS from book titles, messages, etc.). */
function esc(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatDate(value) {
  if (!value) return '—';
  const d = new Date(value.length === 10 ? value + 'T00:00:00' : value);
  return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function formatDateTime(value) {
  if (!value) return '—';
  return new Date(value).toLocaleString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  });
}

function money(value) {
  return '₹' + Number(value || 0).toFixed(2);
}

function toast(message, type = 'success') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = message;
  container.appendChild(el);
  setTimeout(() => el.remove(), 3500);
}

/** Runs an async action on a button: disables it while running and shows errors as toasts. */
async function withButton(button, action) {
  if (button) button.disabled = true;
  try {
    await action();
  } catch (e) {
    toast(e.message, 'error');
  } finally {
    if (button) button.disabled = false;
  }
}

/**
 * Makes sure someone is logged in (and has the right role) before showing a page.
 * Redirects to the right place otherwise. Returns the current user.
 */
async function requireLogin(expectedRole) {
  let me;
  try {
    me = await api.get('/api/auth/me');
  } catch (e) {
    window.location.href = '/index.html';
    return null;
  }
  if (expectedRole && me.role !== expectedRole) {
    window.location.href = me.role === 'ADMIN' ? '/admin.html' : '/app.html';
    return null;
  }
  document.getElementById('user-name').textContent = me.fullName;
  document.getElementById('user-role').textContent = me.role;
  document.getElementById('logout-btn').addEventListener('click', logout);
  document.body.classList.remove('hidden');
  return me;
}

async function logout() {
  try { await api.post('/api/auth/logout'); } catch (e) { /* ignore */ }
  window.location.href = '/index.html';
}

/** Tab switching. onChange(tabName) is called every time a tab opens. */
function setupTabs(onChange) {
  const tabs = document.querySelectorAll('.tab');
  tabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      tabs.forEach((t) => t.classList.toggle('active', t === tab));
      document.querySelectorAll('.panel').forEach((p) =>
        p.classList.toggle('active', p.id === 'panel-' + tab.dataset.tab));
      onChange(tab.dataset.tab);
    });
  });
}

function statusBadge(status) {
  const map = {
    ACTIVE: 'badge-info',
    OVERDUE: 'badge-danger',
    RETURNED: 'badge-muted',
    WAITING: 'badge-warn',
    FULFILLED: 'badge-ok',
    CANCELLED: 'badge-muted',
  };
  return `<span class="badge ${map[status] || 'badge-muted'}">${esc(status)}</span>`;
}

function emptyRow(colspan, text) {
  return `<tr><td colspan="${colspan}" class="empty">${esc(text)}</td></tr>`;
}
