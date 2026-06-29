async function login(username, password) {
  const user = await apiCall('POST', '/auth/login', { username, password });
  setToken(user.token);
  setCurrentUser(user);
  return user;
}

async function register(payload) {

  return apiCall('POST', '/auth/register', payload);
}

async function logout() {
  try { await apiCall('POST', '/auth/logout'); } catch (e) {  }
  clearSession();
  location.href = APP_BASE + 'login.html';
}

function roleHome(role) {
  switch (role) {
    case 'ADMIN': return APP_BASE + 'admin/dashboard.html';
    case 'DEVELOPER': return APP_BASE + 'developer/metrics.html';
    default: return APP_BASE + 'index.html';
  }
}
