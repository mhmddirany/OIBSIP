/* Login and registration pages. */

const errorBox = document.getElementById('error');

function showError(message) {
  errorBox.textContent = message;
  errorBox.classList.remove('hidden');
}

function goToHome(user) {
  window.location.href = user.role === 'ADMIN' ? '/admin.html' : '/app.html';
}

async function login(email, password) {
  const user = await api.post('/api/auth/login', { email, password });
  goToHome(user);
}

// ---------------------------------------------------------------- login page

const loginForm = document.getElementById('login-form');
if (loginForm) {
  // Already logged in? Skip the login page.
  api.get('/api/auth/me').then(goToHome).catch(() => {});

  loginForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    errorBox.classList.add('hidden');
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    if (!email || !password) {
      showError('Please enter your email and password.');
      return;
    }
    const button = document.getElementById('submit');
    button.disabled = true;
    try {
      await login(email, password);
    } catch (e) {
      showError(e.message);
      button.disabled = false;
    }
  });
}

// ------------------------------------------------------------- register page

const registerForm = document.getElementById('register-form');
if (registerForm) {
  registerForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    errorBox.classList.add('hidden');

    const fullName = document.getElementById('fullName').value.trim();
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const confirm = document.getElementById('confirm').value;

    // Client-side checks (the server validates again).
    if (!fullName || !email || !password) {
      showError('All fields are required.');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      showError('Please enter a valid email address.');
      return;
    }
    if (password.length < 8 || !/\d/.test(password)) {
      showError('Password must be at least 8 characters and contain a number.');
      return;
    }
    if (password !== confirm) {
      showError('Passwords do not match.');
      return;
    }

    const button = document.getElementById('submit');
    button.disabled = true;
    try {
      await api.post('/api/auth/register', { fullName, email, password });
      await login(email, password);
    } catch (e) {
      showError(e.message);
      button.disabled = false;
    }
  });
}
