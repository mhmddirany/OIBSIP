/* Member dashboard: catalogue, my books, reservations, contact form. */

let searchTimer = null;

async function init() {
  const me = await requireLogin('USER');
  if (!me) return;

  setupTabs((tab) => {
    if (tab === 'catalogue') loadBooks();
    if (tab === 'loans') loadLoans();
    if (tab === 'reservations') loadReservations();
  });

  document.getElementById('search').addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(loadBooks, 250); // wait until the user stops typing
  });
  document.getElementById('category').addEventListener('change', loadBooks);
  document.getElementById('contact-form').addEventListener('submit', sendMessage);

  // Button clicks inside the lists (event delegation).
  document.getElementById('book-grid').addEventListener('click', onBookAction);
  document.getElementById('loans-body').addEventListener('click', onLoanAction);
  document.getElementById('reservations-body').addEventListener('click', onReservationAction);

  await Promise.all([loadCategories(), loadSummary(), loadBooks()]);
}

// ------------------------------------------------------------------ summary

async function loadSummary() {
  const s = await api.get('/api/me/summary');
  document.getElementById('rules').textContent =
    `Loan period: ${s.loanDays} days · Late fee: ${money(s.finePerDay)} per day · ` +
    `You hold ${s.activeLoans} of ${s.maxActiveLoans} books`;

  const alerts = [];
  if (s.overdueLoans > 0) {
    alerts.push(`<div class="alert alert-danger">You have <strong>${s.overdueLoans}</strong> overdue book(s). ` +
      `Fine so far: <strong>${money(s.accruingFines)}</strong>. Please return them to issue new books.</div>`);
  }
  if (Number(s.unpaidFines) > 0) {
    alerts.push(`<div class="alert alert-warn">Unpaid fines: <strong>${money(s.unpaidFines)}</strong>. ` +
      'Please pay at the library desk. You cannot issue new books until they are paid.</div>');
  }
  document.getElementById('summary-alert').innerHTML = alerts.join('');
}

// ---------------------------------------------------------------- catalogue

async function loadCategories() {
  const categories = await api.get('/api/books/categories');
  const select = document.getElementById('category');
  select.innerHTML = '<option value="">All categories</option>' +
    categories.map((c) => `<option value="${esc(c)}">${esc(c)}</option>`).join('');
}

async function loadBooks() {
  const q = document.getElementById('search').value.trim();
  const category = document.getElementById('category').value;
  const params = new URLSearchParams();
  if (q) params.set('q', q);
  if (category) params.set('category', category);

  const grid = document.getElementById('book-grid');
  try {
    const books = await api.get('/api/books?' + params.toString());
    if (books.length === 0) {
      grid.innerHTML = '<div class="card empty">No books match your search.</div>';
      return;
    }
    grid.innerHTML = books.map(bookCard).join('');
  } catch (e) {
    grid.innerHTML = `<div class="card empty">${esc(e.message)}</div>`;
  }
}

function bookCard(b) {
  const available = b.availableQuantity > 0;
  const badge = available
    ? `<span class="badge badge-ok">${b.availableQuantity} of ${b.totalQuantity} available</span>`
    : `<span class="badge badge-danger">All ${b.totalQuantity} issued</span>`;
  const waiting = b.waitingReservations > 0
    ? `<div class="small muted">${b.waitingReservations} waiting in queue</div>` : '';
  const button = available
    ? `<button class="btn btn-sm" data-action="issue" data-id="${b.id}">Issue</button>`
    : `<button class="btn btn-sm btn-warn" data-action="reserve" data-id="${b.id}">Reserve</button>`;
  return `
    <article class="card book-card">
      <span class="cat">${esc(b.category)}</span>
      <h3>${esc(b.title)}</h3>
      <div class="author">${esc(b.author)}</div>
      <div class="isbn">ISBN ${esc(b.isbn)}</div>
      ${waiting}
      <div class="foot">${badge}${button}</div>
    </article>`;
}

function onBookAction(event) {
  const button = event.target.closest('button[data-action]');
  if (!button) return;
  const bookId = Number(button.dataset.id);

  withButton(button, async () => {
    if (button.dataset.action === 'issue') {
      const loan = await api.post('/api/me/loans', { bookId });
      toast(`"${loan.bookTitle}" issued. Due on ${formatDate(loan.dueDate)}.`);
    } else {
      const r = await api.post('/api/me/reservations', { bookId });
      toast(`"${r.bookTitle}" reserved. You are #${r.queuePosition} in the queue.`);
    }
    await Promise.all([loadBooks(), loadSummary()]);
  });
}

// ------------------------------------------------------------------ my books

async function loadLoans() {
  const body = document.getElementById('loans-body');
  try {
    const loans = await api.get('/api/me/loans');
    if (loans.length === 0) {
      body.innerHTML = emptyRow(7, 'You have not borrowed any books yet. Browse the catalogue to get started!');
      return;
    }
    body.innerHTML = loans.map((l) => {
      let fineText = l.fine > 0 ? money(l.fine) : '—';
      if (l.fine > 0 && l.status === 'RETURNED') {
        fineText += l.finePaid ? ' <span class="badge badge-ok">Paid</span>' : ' <span class="badge badge-warn">Unpaid</span>';
      } else if (l.status === 'OVERDUE') {
        fineText += ` <div class="small muted">${l.overdueDays} day(s) late</div>`;
      }
      const action = l.status === 'RETURNED' ? ''
        : `<button class="btn btn-sm btn-secondary" data-action="return" data-id="${l.id}">Return</button>`;
      return `
        <tr>
          <td><strong>${esc(l.bookTitle)}</strong><div class="small muted">${esc(l.bookAuthor)}</div></td>
          <td>${formatDate(l.issueDate)}</td>
          <td>${formatDate(l.dueDate)}</td>
          <td>${formatDate(l.returnDate)}</td>
          <td>${statusBadge(l.status)}</td>
          <td class="num">${fineText}</td>
          <td>${action}</td>
        </tr>`;
    }).join('');
  } catch (e) {
    body.innerHTML = emptyRow(7, e.message);
  }
}

function onLoanAction(event) {
  const button = event.target.closest('button[data-action="return"]');
  if (!button) return;
  if (!confirm('Return this book?')) return;

  withButton(button, async () => {
    const loan = await api.post(`/api/me/loans/${button.dataset.id}/return`);
    if (loan.fine > 0) {
      toast(`Book returned ${loan.overdueDays} day(s) late. Fine: ${money(loan.fine)}.`, 'error');
    } else {
      toast('Book returned on time. Thank you!');
    }
    await Promise.all([loadLoans(), loadSummary()]);
  });
}

// ------------------------------------------------------------- reservations

async function loadReservations() {
  const body = document.getElementById('reservations-body');
  try {
    const list = await api.get('/api/me/reservations');
    if (list.length === 0) {
      body.innerHTML = emptyRow(5, 'No reservations. When a book is fully issued, you can reserve it from the catalogue.');
      return;
    }
    body.innerHTML = list.map((r) => `
      <tr>
        <td><strong>${esc(r.bookTitle)}</strong><div class="small muted">${esc(r.bookAuthor)}</div></td>
        <td>${formatDateTime(r.reservedAt)}</td>
        <td>${statusBadge(r.status)}</td>
        <td>${r.status === 'WAITING' ? '#' + r.queuePosition : '—'}</td>
        <td>${r.status === 'WAITING'
          ? `<button class="btn btn-sm btn-secondary" data-action="cancel" data-id="${r.id}">Cancel</button>` : ''}</td>
      </tr>`).join('');
  } catch (e) {
    body.innerHTML = emptyRow(5, e.message);
  }
}

function onReservationAction(event) {
  const button = event.target.closest('button[data-action="cancel"]');
  if (!button) return;
  if (!confirm('Cancel this reservation?')) return;

  withButton(button, async () => {
    await api.del(`/api/me/reservations/${button.dataset.id}`);
    toast('Reservation cancelled.');
    await loadReservations();
  });
}

// ------------------------------------------------------------------ contact

function sendMessage(event) {
  event.preventDefault();
  const subject = document.getElementById('subject').value.trim();
  const message = document.getElementById('message').value.trim();
  if (!subject || !message) {
    toast('Please fill in both the subject and the message.', 'error');
    return;
  }
  const button = document.getElementById('contact-submit');
  withButton(button, async () => {
    await api.post('/api/me/messages', { subject, message });
    toast('Message sent. The library team will get back to you.');
    event.target.reset();
  });
}

init();
