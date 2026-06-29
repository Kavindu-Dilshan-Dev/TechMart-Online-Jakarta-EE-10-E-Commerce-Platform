const API_BASE = '/techmart/api';
const APP_BASE = '/techmart/';
const WS_BASE = (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/techmart/ws';

function getToken() { return sessionStorage.getItem('tm_token'); }
function setToken(t) { sessionStorage.setItem('tm_token', t); }
function getCurrentUser() {
  const raw = sessionStorage.getItem('tm_user');
  return raw ? JSON.parse(raw) : null;
}
function setCurrentUser(u) { sessionStorage.setItem('tm_user', JSON.stringify(u)); }
function clearSession() { sessionStorage.removeItem('tm_token'); sessionStorage.removeItem('tm_user'); }
function isLoggedIn() { return !!getToken(); }
function hasRole(role) { const u = getCurrentUser(); return !!u && u.role === role; }

async function apiCall(method, path, body) {
  const headers = { 'Accept': 'application/json' };
  if (body !== undefined && body !== null) headers['Content-Type'] = 'application/json';
  const token = getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  let res;
  try {
    res = await fetch(API_BASE + path, {
      method,
      headers,
      body: body !== undefined && body !== null ? JSON.stringify(body) : undefined
    });
  } catch (networkErr) {
    showToast('Network error - is the server running?', 'danger');
    throw networkErr;
  }

  let json = null;
  try { json = await res.json(); } catch (e) {  }

  if (res.status === 401) {
    clearSession();
    if (!location.pathname.endsWith('login.html')) {
      showToast('Please sign in to continue', 'warning');
      setTimeout(() => location.href = APP_BASE + 'login.html', 800);
    }
    throw new Error('Unauthorized');
  }
  if (res.status === 503) {
    showToast((json && json.message) || 'Service temporarily unavailable', 'danger');
    throw new Error('ServiceUnavailable');
  }
  if (!res.ok || !json || json.success === false) {
    const msg = (json && json.message) || ('Request failed (' + res.status + ')');
    showToast(msg, 'danger');
    const err = new Error(msg);
    err.response = json;
    throw err;
  }
  return json.data;
}

function requireAuth() {
  if (!isLoggedIn()) { location.href = APP_BASE + 'login.html'; return false; }
  return true;
}
function requireRole(...roles) {
  if (!requireAuth()) return false;
  const u = getCurrentUser();
  if (!roles.includes(u.role)) {
    document.body.innerHTML = '<div class="container py-5"><div class="alert alert-danger">' +
      '<h4>Access Denied</h4><p>This area requires role: ' + roles.join(' or ') +
      '. You are signed in as ' + u.role + '.</p><a href="' + APP_BASE + 'index.html" class="btn btn-primary">Home</a></div></div>';
    return false;
  }
  return true;
}

function formatCurrency(n) {
  if (n === null || n === undefined) return 'LKR 0.00';
  return 'LKR ' + Number(n).toLocaleString('en-LK', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function formatDate(iso) {
  if (!iso) return '-';
  const d = new Date(iso);
  return isNaN(d) ? iso : d.toLocaleString();
}
function escapeHtml(s) {
  if (s === null || s === undefined) return '';
  return String(s).replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function statusBadge(status) {
  const map = {
    PENDING: 'secondary', CONFIRMED: 'info', PROCESSING: 'primary',
    SHIPPED: 'warning', DELIVERED: 'success', CANCELLED: 'danger',
    COMPLETED: 'success', FAILED: 'danger', REFUNDED: 'dark',
    OPEN: 'danger', HALF_OPEN: 'warning', CLOSED: 'success'
  };
  return '<span class="badge bg-' + (map[status] || 'secondary') + '">' + escapeHtml(status) + '</span>';
}

/* ---------- toast ---------- */
function showToast(message, type = 'info') {
  let host = document.getElementById('tm-toast-host');
  if (!host) {
    host = document.createElement('div');
    host.id = 'tm-toast-host';
    host.className = 'toast-container position-fixed top-0 end-0 p-3';
    host.style.zIndex = '1080';
    document.body.appendChild(host);
  }
  const el = document.createElement('div');
  el.className = 'toast align-items-center text-bg-' + type + ' border-0 show';
  el.role = 'alert';
  el.innerHTML = '<div class="d-flex"><div class="toast-body">' + escapeHtml(message) +
    '</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>';
  host.appendChild(el);
  setTimeout(() => el.remove(), 4500);
  el.querySelector('.btn-close').addEventListener('click', () => el.remove());
}

/* ---------- shared navbar ---------- */
function renderNavbar(active) {
  const u = getCurrentUser();
  const host = document.getElementById('tm-navbar');
  if (!host) return;
  let right;
  if (u) {
    const dash = u.role === 'ADMIN' ? `<a class="nav-link" href="${APP_BASE}admin/dashboard.html">Admin</a>`
      : u.role === 'DEVELOPER' ? `<a class="nav-link" href="${APP_BASE}developer/metrics.html">Dev Console</a>` : '';
    right = `
      ${dash}
      <a class="nav-link position-relative" href="${APP_BASE}cart.html">Cart
        <span id="tm-cart-count" class="badge rounded-pill bg-danger" style="display:none">0</span></a>
      <a class="nav-link position-relative" href="${APP_BASE}order-history.html">Orders
        <span id="tm-notif-count" class="badge rounded-pill bg-warning text-dark" style="display:none">0</span></a>
      <span class="nav-link text-light">Hi, ${escapeHtml(u.firstName || u.username)}</span>
      <a class="nav-link" href="#" onclick="logout();return false;">Logout</a>`;
  } else {
    right = `<a class="nav-link" href="${APP_BASE}login.html">Login</a><a class="nav-link" href="${APP_BASE}register.html">Register</a>`;
  }
  host.innerHTML = `
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
      <div class="container">
        <a class="navbar-brand fw-bold" href="${APP_BASE}index.html">🛒 TechMart<span class="text-primary">Online</span></a>
        <button class="navbar-toggler" data-bs-toggle="collapse" data-bs-target="#tmNav"><span class="navbar-toggler-icon"></span></button>
        <div class="collapse navbar-collapse" id="tmNav">
          <div class="navbar-nav me-auto">
            <a class="nav-link ${active === 'home' ? 'active' : ''}" href="${APP_BASE}index.html">Home</a>
            <a class="nav-link ${active === 'products' ? 'active' : ''}" href="${APP_BASE}products.html">Products</a>
          </div>
          <div class="navbar-nav align-items-lg-center">${right}</div>
        </div>
      </div>
    </nav>`;
}

function updateNotifBadge(count) {
  const b = document.getElementById('tm-notif-count');
  if (b) { b.textContent = count; b.style.display = count > 0 ? 'inline-block' : 'none'; }
}
async function refreshCartCount() {
  if (!isLoggedIn()) return;
  try {
    const cart = await apiCall('GET', '/cart');
    const b = document.getElementById('tm-cart-count');
    if (b) { b.textContent = cart.itemCount; b.style.display = cart.itemCount > 0 ? 'inline-block' : 'none'; }
  } catch (e) { /* ignore */ }
}
