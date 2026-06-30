function tmPick(obj, keys, fallback) {
  if (!obj) return fallback;
  for (const k of keys) {
    if (obj[k] !== undefined && obj[k] !== null) return obj[k];
  }
  return fallback;
}

function tmNum(v, fallback) {
  const n = Number(v);
  return isNaN(n) ? (fallback === undefined ? 0 : fallback) : n;
}

function tmRound1(v) {
  const n = Number(v);
  if (isNaN(n)) return 0;
  return Math.round(n * 10) / 10;
}

function tmFormatUptime(metrics) {
  const str = tmPick(metrics, ['uptime', 'uptimeString'], null);
  if (typeof str === 'string' && str.trim() !== '') return str;
  let ms = tmPick(metrics, ['uptimeMillis', 'uptimeMs'], null);
  if (ms === null) {
    const secs = tmPick(metrics, ['uptimeSeconds', 'uptimeSecs'], null);
    if (secs !== null) ms = tmNum(secs) * 1000;
  }
  if (ms === null) return '-';
  let total = Math.floor(tmNum(ms) / 1000);
  const d = Math.floor(total / 86400); total -= d * 86400;
  const h = Math.floor(total / 3600); total -= h * 3600;
  const m = Math.floor(total / 60); const s = total - m * 60;
  const parts = [];
  if (d) parts.push(d + 'd');
  if (h || d) parts.push(h + 'h');
  parts.push(m + 'm');
  parts.push(s + 's');
  return parts.join(' ');
}

const TMMetrics = {
  uptime: (m) => tmFormatUptime(m),
  activeUsers: (m) => tmNum(tmPick(m, ['activeUsers', 'activeUserCount'], 0)),
  totalRequests: (m) => tmNum(tmPick(m, ['totalRequests', 'requestCount', 'requests'], 0)),
  avgResponseMs: (m) => tmRound1(tmPick(m, ['averageResponseTimeMs', 'avgResponseMs', 'averageResponseMs', 'avgResponseTimeMs'], 0)),
  heapUsedMb: (m) => tmRound1(tmPick(m, ['heapUsedMb', 'heapUsedMB', 'heapUsed'], 0)),
  heapMaxMb: (m) => tmRound1(tmPick(m, ['heapMaxMb', 'heapMaxMB', 'heapMax'], 0)),
  threadCount: (m) => tmNum(tmPick(m, ['threadCount', 'threads'], 0)),
  gcCount: (m) => tmNum(tmPick(m, ['gcCollectionCount', 'gcCount'], 0)),
  gcTime: (m) => tmNum(tmPick(m, ['gcCollectionTime', 'gcTime', 'gcCollectionTimeMs'], 0)),
  ordersProcessed: (m) => tmNum(tmPick(m, ['totalOrdersProcessed', 'ordersProcessed'], 0)),
  ordersFailed: (m) => tmNum(tmPick(m, ['totalOrdersFailed', 'ordersFailed'], 0))
};

function tmNormalizeSample(s, idx) {
  const ts = tmPick(s, ['timestamp', 'time', 'ts', 'at'], null);
  let label = '#' + (idx + 1);
  if (ts !== null) {
    const d = new Date(typeof ts === 'number' ? ts : ts);
    if (!isNaN(d)) {
      label = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } else if (typeof ts === 'string') {
      label = ts;
    }
  }
  return {
    label,
    avgResponseMs: tmRound1(tmPick(s, ['averageResponseTimeMs', 'avgResponseMs', 'averageResponseMs', 'avgResponseTimeMs'], 0)),
    heapUsedMb: tmRound1(tmPick(s, ['heapUsedMb', 'heapUsedMB', 'heapUsed'], 0))
  };
}

function tmSetText(id, value) {
  const el = document.getElementById(id);
  if (el) el.textContent = value;
}

function tmMetricCard(grad, label, valueId, value, sub) {
  return '' +
    '<div class="metric-card ' + grad + '">' +
      '<div class="metric-label">' + escapeHtml(label) + '</div>' +
      '<div class="metric-value" id="' + escapeHtml(valueId) + '">' + escapeHtml(String(value)) + '</div>' +
      (sub ? '<div class="metric-sub">' + escapeHtml(sub) + '</div>' : '') +
    '</div>';
}

function renderCircuitBreakerTable(tbodyId, states, withReset, onReset) {
  const tbody = document.getElementById(tbodyId);
  if (!tbody) return;
  const entries = states ? Object.keys(states) : [];
  if (!entries.length) {
    tbody.innerHTML = '<tr><td colspan="' + (withReset ? 3 : 2) +
      '" class="text-center text-muted py-3">No circuit breakers reported.</td></tr>';
    return;
  }
  tbody.innerHTML = entries.map(svc => {
    const state = String(states[svc]);
    const stateCell = '<span class="cb-state-' + escapeHtml(state) + '">' + statusBadge(state) + '</span>';
    let row = '<tr><td>' + escapeHtml(svc) + '</td><td>' + stateCell + '</td>';
    if (withReset) {
      row += '<td class="text-end"><button type="button" class="btn btn-sm btn-outline-danger" ' +
        'data-cb-reset="' + escapeHtml(svc) + '">Reset</button></td>';
    }
    return row + '</tr>';
  }).join('');

  if (withReset && typeof onReset === 'function') {
    tbody.querySelectorAll('[data-cb-reset]').forEach(btn => {
      btn.addEventListener('click', () => onReset(btn.getAttribute('data-cb-reset'), btn));
    });
  }
}

function tmCreateSamplesLineChart(canvasId) {
  const ctx = document.getElementById(canvasId);
  if (!ctx || typeof Chart === 'undefined') return null;
  return new Chart(ctx, {
    type: 'line',
    data: {
      labels: [],
      datasets: [
        {
          label: 'Avg response (ms)',
          data: [],
          borderColor: '#0d6efd',
          backgroundColor: 'rgba(13,110,253,.15)',
          yAxisID: 'y',
          tension: .3,
          fill: true,
          pointRadius: 2
        },
        {
          label: 'Heap used (MB)',
          data: [],
          borderColor: '#20c997',
          backgroundColor: 'rgba(32,201,151,.15)',
          yAxisID: 'y1',
          tension: .3,
          fill: false,
          pointRadius: 2
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: { legend: { position: 'top' } },
      scales: {
        y: { type: 'linear', position: 'left', title: { display: true, text: 'ms' }, beginAtZero: true },
        y1: {
          type: 'linear', position: 'right', title: { display: true, text: 'MB' },
          beginAtZero: true, grid: { drawOnChartArea: false }
        }
      }
    }
  });
}

function tmUpdateSamplesLineChart(chart, samples) {
  if (!chart) return;
  const rows = (samples || []).map(tmNormalizeSample);
  chart.data.labels = rows.map(r => r.label);
  chart.data.datasets[0].data = rows.map(r => r.avgResponseMs);
  chart.data.datasets[1].data = rows.map(r => r.heapUsedMb);
  chart.update('none');
}

function tmCreateHeapDoughnut(canvasId) {
  const ctx = document.getElementById(canvasId);
  if (!ctx || typeof Chart === 'undefined') return null;
  return new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Used (MB)', 'Free (MB)'],
      datasets: [{
        data: [0, 0],
        backgroundColor: ['#dc3545', '#198754'],
        borderWidth: 0
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { position: 'bottom' } }
    }
  });
}

function tmUpdateHeapDoughnut(chart, usedMb, maxMb) {
  if (!chart) return;
  const used = tmRound1(usedMb);
  const free = tmRound1(Math.max(0, tmNum(maxMb) - tmNum(usedMb)));
  chart.data.datasets[0].data = [used, free];
  chart.update('none');
}

function tmCreateOrdersBar(canvasId) {
  const ctx = document.getElementById(canvasId);
  if (!ctx || typeof Chart === 'undefined') return null;
  return new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['Processed', 'Failed'],
      datasets: [{
        label: 'Orders',
        data: [0, 0],
        backgroundColor: ['#0d6efd', '#dc3545'],
        borderWidth: 0
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
    }
  });
}

function tmUpdateOrdersBar(chart, processed, failed) {
  if (!chart) return;
  chart.data.datasets[0].data = [tmNum(processed), tmNum(failed)];
  chart.update('none');
}

function startAutoRefresh(fn, intervalMs) {
  const period = intervalMs || 5000;
  let stopped = false;
  let timer = null;

  async function tick() {
    if (stopped) return;
    try { await fn(); } catch (e) {  }
    if (!stopped) timer = setTimeout(tick, period);
  }
  tick();

  const handle = {
    stop() { stopped = true; if (timer) clearTimeout(timer); }
  };

  document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
      if (timer) { clearTimeout(timer); timer = null; }
    } else if (!stopped && timer === null) {
      tick();
    }
  });
  return handle;
}
