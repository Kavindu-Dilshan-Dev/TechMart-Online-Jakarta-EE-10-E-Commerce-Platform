function renderAdminSidebar(active) {
  const host = document.getElementById('tm-admin-sidebar');
  if (!host) return;
  const links = [
    { key: 'dashboard', label: 'Dashboard', href: APP_BASE + 'admin/dashboard.html', icon: '📊' },
    { key: 'products', label: 'Products', href: APP_BASE + 'admin/products.html', icon: '📦' },
    { key: 'orders', label: 'Orders', href: APP_BASE + 'admin/orders.html', icon: '🧾' },
    { key: 'inventory', label: 'Inventory', href: APP_BASE + 'admin/inventory.html', icon: '🏬' },
    { key: 'metrics', label: 'Metrics', href: APP_BASE + 'admin/metrics.html', icon: '📈' }
  ];
  host.className = 'dash-sidebar';
  host.innerHTML =
    '<div class="text-uppercase text-secondary small fw-bold px-3 pt-3 pb-2">Admin Console</div>' +
    links.map(l =>
      '<a class="' + (l.key === active ? 'active' : '') + '" href="' + l.href + '">' +
      '<span class="me-2">' + l.icon + '</span>' + escapeHtml(l.label) + '</a>'
    ).join('');
}

function adminFillTable(tbodyId, rows, cols, emptyMsg) {
  const tb = document.getElementById(tbodyId);
  if (!tb) return;
  if (!rows || rows.length === 0) {
    tb.innerHTML = '<tr><td colspan="' + cols + '" class="text-center text-muted py-4">' +
      escapeHtml(emptyMsg || 'Nothing to show.') + '</td></tr>';
    return;
  }
  tb.innerHTML = rows.join('');
}

function adminTableLoading(tbodyId, cols) {
  const tb = document.getElementById(tbodyId);
  if (!tb) return;
  tb.innerHTML = '<tr><td colspan="' + cols + '" class="text-center text-muted py-4">' +
    '<span class="spinner-border spinner-border-sm me-2"></span>Loading…</td></tr>';
}

function adminMetricCard(label, value, sub, grad) {
  return '<div class="metric-card bg-grad-' + (grad || 'blue') + '">' +
    '<div class="metric-label">' + escapeHtml(label) + '</div>' +
    '<div class="metric-value">' + escapeHtml(value) + '</div>' +
    (sub ? '<div class="metric-sub">' + escapeHtml(sub) + '</div>' : '') +
    '</div>';
}

const ADMIN_ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

function adminStatusOptions(selected, includeAll) {
  let html = includeAll ? '<option value="">All statuses</option>' : '';
  html += ADMIN_ORDER_STATUSES.map(s =>
    '<option value="' + s + '"' + (s === selected ? ' selected' : '') + '>' + escapeHtml(s) + '</option>'
  ).join('');
  return html;
}
