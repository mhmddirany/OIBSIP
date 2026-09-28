/* Admin dashboard: stats, books CRUD, issued books, members, fines, reservations, messages. */

let books = [];
let searchTimer = null;

const loaders = {
  dashboard: loadStats,
  books: loadBooks,
  loans: loadLoans,
  members: loadMembers,
  fines: loadFines,
  reservations: loadReservations,
  messages: loadMessages,
};

async function init() {
  const me = await requireLogin('ADMIN');
  if (!me) return;

  setupTabs((tab) => loaders[tab]());

  document.getElementById('add-book-btn').addEventListener('click', () => openBookDialog(null));
  document.getElementById('book-cancel').addEventListener('click', () => document.getElementById('book-dialog').close());
  document.getElementById('book-form').addEventListener('submit', saveBook);
  document.getElementById('book-search').addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(loadBooks, 250);
  });
  document.getElementById('loan-filter').addEventListener('change', loadLoans);

  document.getElementById('books-body').addEventListener('click', onBookAction);
  document.getElementById('loans-body').addEventListener('click', onLoanAction);
  document.getElementById('members-body').addEventListener('click', onMemberAction);
  document.getElementById('fines-body').addEventListener('click', onFineAction);
  document.getElementById('messages-list').addEventListener('click', onMessageAction);

  await loadStats();
}

// ---------------------------------------------------------------- dashboard

async function loadStats() {
  const s = await api.get('/api/admin/stats');
  const cards = [
    ['Book titles', s.titles],
    ['Total copies', s.totalCopies],
    ['Copies on shelf', s.availableCopies],
    ['Books issued', s.activeLoans],
    ['Overdue', s.overdueLoans, s.overdueLoans > 0 ? 'danger' : ''],
    ['Members', s.members],
    ['Waiting reservations', s.waitingReservations],
    ['Unpaid fines', money(s.unpaidFines), Number(s.unpaidFines) > 0 ? 'warn' : ''],
    ['Open messages', s.openMessages, s.openMessages > 0 ? 'warn' : ''],
  ];
  document.getElementById('stats').innerHTML = cards.map(([label, value, cls]) => `
    <div class="card stat ${cls || ''}">
      <div class="label">${esc(label)}</div>
      <div class="value">${esc(value)}</div>
    </div>`).join('');
  updateMessageCount(s.openMessages);
}

function updateMessageCount(n) {
  const badge = document.getElementById('msg-count');
  badge.textContent = n;
  badge.classList.toggle('hidden', !n);
}

// -------------------------------------------------------------------- books

async function loadBooks() {
  const q = document.getElementById('book-search').value.trim();
  const body = document.getElementById('books-body');
  try {
    books = await api.get('/api/books' + (q ? '?q=' + encodeURIComponent(q) : ''));
    if (books.length === 0) {
      body.innerHTML = emptyRow(7, 'No books found.');
      return;
    }
    body.innerHTML = books.map((b) => `
      <tr>
        <td><strong>${esc(b.title)}</strong></td>
        <td>${esc(b.author)}</td>
        <td>${esc(b.isbn)}</td>
        <td>${esc(b.category)}</td>
        <td class="num">${b.availableQuantity} / ${b.totalQuantity}</td>
        <td class="num">${b.waitingReservations || '—'}</td>
        <td><div class="actions">
          <button class="btn btn-sm btn-secondary" data-action="edit" data-id="${b.id}">Edit</button>
          <button class="btn btn-sm btn-danger" data-action="delete" data-id="${b.id}">Delete</button>
        </div></td>
      </tr>`).join('');
  } catch (e) {
    body.innerHTML = emptyRow(7, e.message);
  }
}

async function openBookDialog(book) {
  document.getElementById('book-dialog-title').textContent = book ? 'Edit book' : 'Add book';
  document.getElementById('book-error').classList.add('hidden');
  document.getElementById('book-id').value = book ? book.id : '';
  document.getElementById('book-title').value = book ? book.title : '';
  document.getElementById('book-author').value = book ? book.author : '';
  document.getElementById('book-isbn').value = book ? book.isbn : '';
  document.getElementById('book-category').value = book ? book.category : '';
  document.getElementById('book-quantity').value = book ? book.totalQuantity : 1;

  try {
    const categories = await api.get('/api/books/categories');
    document.getElementById('category-options').innerHTML =
      categories.map((c) => `<option value="${esc(c)}">`).join('');
  } catch (e) { /* suggestions are optional */ }

  document.getElementById('book-dialog').showModal();
  document.getElementById('book-title').focus();
}

async function saveBook(event) {
  event.preventDefault();
  const errorBox = document.getElementById('book-error');
  errorBox.classList.add('hidden');

  const id = document.getElementById('book-id').value;
  const payload = {
    title: document.getElementById('book-title').value.trim(),
    author: document.getElementById('book-author').value.trim(),
    isbn: document.getElementById('book-isbn').value.trim(),
    category: document.getElementById('book-category').value.trim(),
    totalQuantity: Number(document.getElementById('book-quantity').value),
  };

  if (!payload.title || !payload.author || !payload.isbn || !payload.category) {
    errorBox.textContent = 'All fields are required.';
    errorBox.classList.remove('hidden');
    return;
  }
  if (!Number.isInteger(payload.totalQuantity) || payload.totalQuantity < 1) {
    errorBox.textContent = 'Number of copies must be a whole number of at least 1.';
    errorBox.classList.remove('hidden');
    return;
  }

  const button = document.getElementById('book-save');
  button.disabled = true;
  try {
    if (id) {
      await api.put(`/api/admin/books/${id}`, payload);
      toast('Book updated.');
    } else {
      await api.post('/api/admin/books', payload);
      toast('Book added.');
    }
    document.getElementById('book-dialog').close();
    await loadBooks();
  } catch (e) {
    errorBox.textContent = e.message;
    errorBox.classList.remove('hidden');
  } finally {
    button.disabled = false;
  }
}

function onBookAction(event) {
  const button = event.target.closest('button[data-action]');
  if (!button) return;
  const book = books.find((b) => b.id === Number(button.dataset.id));
  if (!book) return;

  if (button.dataset.action === 'edit') {
    openBookDialog(book);
    return;
  }
  if (!confirm(`Delete "${book.title}"? Its loan history will also be removed.`)) return;
  withButton(button, async () => {
    await api.del(`/api/admin/books/${book.id}`);
    toast('Book deleted.');
    await loadBooks();
  });
}

// ------------------------------------------------------------- issued books

async function loadLoans() {
  const filter = document.getElementById('loan-filter').value;
  const body = document.getElementById('loans-body');
  try {
    const loans = await api.get('/api/admin/loans?filter=' + encodeURIComponent(filter));
    if (loans.length === 0) {
      body.innerHTML = emptyRow(8, filter === 'overdue' ? 'No overdue books. 🎉' : 'No books issued.');
      return;
    }
    body.innerHTML = loans.map((l) => `
      <tr>
        <td><strong>${esc(l.bookTitle)}</strong></td>
        <td>${esc(l.userName)}<div class="small muted">${esc(l.userEmail)}</div></td>
        <td>${formatDate(l.issueDate)}</td>
        <td>${formatDate(l.dueDate)}</td>
        <td>${formatDate(l.returnDate)}</td>
        <td>${statusBadge(l.status)}${l.overdueDays > 0 ? `<div class="small muted">${l.overdueDays} day(s) late</div>` : ''}</td>
        <td class="num">${l.fine > 0 ? money(l.fine) : '—'}</td>
        <td>${l.status === 'RETURNED' ? ''
          : `<button class="btn btn-sm btn-secondary" data-action="return" data-id="${l.id}">Mark returned</button>`}</td>
      </tr>`).join('');
  } catch (e) {
    body.innerHTML = emptyRow(8, e.message);
  }
}

function onLoanAction(event) {
  const button = event.target.closest('button[data-action="return"]');
  if (!button) return;
  if (!confirm('Mark this book as returned?')) return;
  withButton(button, async () => {
    const loan = await api.post(`/api/admin/loans/${button.dataset.id}/return`);
    toast(loan.fine > 0 ? `Returned. Fine of ${money(loan.fine)} recorded.` : 'Returned on time.');
    await loadLoans();
  });
}

// ------------------------------------------------------------------ members

async function loadMembers() {
  const body = document.getElementById('members-body');
  try {
    const members = await api.get('/api/admin/members');
    body.innerHTML = members.map((m) => {
      const isAdmin = m.role === 'ADMIN';
      const status = m.active ? '<span class="badge badge-ok">Active</span>' : '<span class="badge badge-muted">Deactivated</span>';
      const actions = isAdmin ? '' : `
        <div class="actions">
          <button class="btn btn-sm btn-secondary" data-action="toggle" data-id="${m.id}" data-active="${m.active}">
            ${m.active ? 'Deactivate' : 'Activate'}</button>
          <button class="btn btn-sm btn-danger" data-action="delete" data-id="${m.id}" data-name="${esc(m.fullName)}">Delete</button>
        </div>`;
      return `
        <tr>
          <td><strong>${esc(m.fullName)}</strong></td>
          <td>${esc(m.email)}</td>
          <td>${esc(m.role)}</td>
          <td>${formatDate(m.createdAt)}</td>
          <td class="num">${m.activeLoans}</td>
          <td class="num">${Number(m.unpaidFines) > 0 ? money(m.unpaidFines) : '—'}</td>
          <td>${status}</td>
          <td>${actions}</td>
        </tr>`;
    }).join('');
  } catch (e) {
    body.innerHTML = emptyRow(8, e.message);
  }
}

function onMemberAction(event) {
  const button = event.target.closest('button[data-action]');
  if (!button) return;
  const id = button.dataset.id;

  if (button.dataset.action === 'toggle') {
    const makeActive = button.dataset.active !== 'true';
    withButton(button, async () => {
      await api.patch(`/api/admin/members/${id}/status`, { active: makeActive });
      toast(makeActive ? 'Member activated.' : 'Member deactivated. They can no longer log in.');
      await loadMembers();
    });
  } else if (button.dataset.action === 'delete') {
    if (!confirm(`Delete member "${button.dataset.name}"? This cannot be undone.`)) return;
    withButton(button, async () => {
      await api.del(`/api/admin/members/${id}`);
      toast('Member deleted.');
      await loadMembers();
    });
  }
}

// -------------------------------------------------------------------- fines

async function loadFines() {
  const body = document.getElementById('fines-body');
  try {
    const fines = await api.get('/api/admin/fines');
    if (fines.length === 0) {
      body.innerHTML = emptyRow(8, 'No fines recorded.');
      return;
    }
    body.innerHTML = fines.map((f) => `
      <tr>
        <td>${esc(f.userName)}<div class="small muted">${esc(f.userEmail)}</div></td>
        <td>${esc(f.bookTitle)}</td>
        <td>${formatDate(f.dueDate)}</td>
        <td>${formatDate(f.returnDate)}</td>
        <td class="num">${f.overdueDays}</td>
        <td class="num"><strong>${money(f.fine)}</strong></td>
        <td>${f.finePaid ? '<span class="badge badge-ok">Paid</span>' : '<span class="badge badge-warn">Unpaid</span>'}</td>
        <td>${f.finePaid ? ''
          : `<button class="btn btn-sm" data-action="pay" data-id="${f.id}">Mark paid</button>`}</td>
      </tr>`).join('');
  } catch (e) {
    body.innerHTML = emptyRow(8, e.message);
  }
}

function onFineAction(event) {
  const button = event.target.closest('button[data-action="pay"]');
  if (!button) return;
  if (!confirm('Mark this fine as paid?')) return;
  withButton(button, async () => {
    await api.post(`/api/admin/loans/${button.dataset.id}/pay-fine`);
    toast('Fine marked as paid.');
    await loadFines();
  });
}

// ------------------------------------------------------------- reservations

async function loadReservations() {
  const body = document.getElementById('reservations-body');
  try {
    const list = await api.get('/api/admin/reservations');
    if (list.length === 0) {
      body.innerHTML = emptyRow(5, 'No reservations yet.');
      return;
    }
    body.innerHTML = list.map((r) => `
      <tr>
        <td><strong>${esc(r.bookTitle)}</strong></td>
        <td>${esc(r.userName)}<div class="small muted">${esc(r.userEmail)}</div></td>
        <td>${formatDateTime(r.reservedAt)}</td>
        <td>${statusBadge(r.status)}</td>
        <td>${r.status === 'WAITING' ? '#' + r.queuePosition : '—'}</td>
      </tr>`).join('');
  } catch (e) {
    body.innerHTML = emptyRow(5, e.message);
  }
}

// ----------------------------------------------------------------- messages

async function loadMessages() {
  const list = document.getElementById('messages-list');
  try {
    const messages = await api.get('/api/admin/messages');
    updateMessageCount(messages.filter((m) => !m.resolved).length);
    if (messages.length === 0) {
      list.innerHTML = '<div class="card empty">No messages yet.</div>';
      return;
    }
    list.innerHTML = messages.map((m) => `
      <article class="card msg-card ${m.resolved ? 'resolved' : ''}">
        <div class="panel-head" style="margin-bottom:.25rem">
          <h3>${esc(m.subject)}</h3>
          ${m.resolved ? '<span class="badge badge-ok">Resolved</span>' : '<span class="badge badge-warn">Open</span>'}
        </div>
        <div class="meta">From ${esc(m.name)} &lt;${esc(m.email)}&gt; · ${formatDateTime(m.createdAt)}</div>
        <div class="body">${esc(m.message)}</div>
        ${m.resolved ? '' : `<button class="btn btn-sm" data-action="resolve" data-id="${m.id}">Mark resolved</button>`}
      </article>`).join('');
  } catch (e) {
    list.innerHTML = `<div class="card empty">${esc(e.message)}</div>`;
  }
}

function onMessageAction(event) {
  const button = event.target.closest('button[data-action="resolve"]');
  if (!button) return;
  withButton(button, async () => {
    await api.patch(`/api/admin/messages/${button.dataset.id}/resolve`);
    toast('Message marked as resolved.');
    await loadMessages();
  });
}

init();
