let _tmSocket = null;
let _tmReconnectDelay = 1000;
const _TM_MAX_DELAY = 30000;
let _tmPingTimer = null;
let _tmUnread = 0;

function connectNotifications() {
  const token = getToken();
  if (!token) return;
  try {
    _tmSocket = new WebSocket(WS_BASE + '/notifications/' + encodeURIComponent(token));
  } catch (e) {
    scheduleReconnect();
    return;
  }

  _tmSocket.onopen = () => {
    _tmReconnectDelay = 1000;
    setWsStatus(true);
    clearInterval(_tmPingTimer);
    _tmPingTimer = setInterval(() => {
      if (_tmSocket && _tmSocket.readyState === WebSocket.OPEN) _tmSocket.send('ping');
    }, 30000);
  };

  _tmSocket.onmessage = (evt) => {
    let msg;
    try { msg = JSON.parse(evt.data); } catch (e) { return; }
    handleNotification(msg);
  };

  _tmSocket.onclose = () => { setWsStatus(false); clearInterval(_tmPingTimer); scheduleReconnect(); };
  _tmSocket.onerror = () => { if (_tmSocket) _tmSocket.close(); };
}

function scheduleReconnect() {
  if (!getToken()) return;
  setTimeout(connectNotifications, _tmReconnectDelay);
  _tmReconnectDelay = Math.min(_tmReconnectDelay * 2, _TM_MAX_DELAY);
}

function handleNotification(msg) {
  switch (msg.type) {
    case 'PONG':
      return;
    case 'UNREAD_COUNT':
      _tmUnread = msg.count || 0;
      updateNotifBadge(_tmUnread);
      return;
    case 'ORDER_UPDATE':
    case 'STOCK_ALERT':
    case 'SYSTEM':
      _tmUnread += 1;
      updateNotifBadge(_tmUnread);
      showToast((msg.title ? msg.title + ': ' : '') + (msg.message || ''),
        msg.type === 'STOCK_ALERT' ? 'success' : 'info');
      break;
    default:
      break;
  }
  if (typeof window.onTechMartNotification === 'function') {
    try { window.onTechMartNotification(msg); } catch (e) {  }
  }
}

function setWsStatus(connected) {
  const el = document.getElementById('tm-ws-status');
  if (!el) return;
  el.textContent = connected ? 'LIVE' : 'OFFLINE';
  el.className = 'badge bg-' + (connected ? 'success' : 'secondary');
}

document.addEventListener('DOMContentLoaded', () => { if (isLoggedIn()) connectNotifications(); });
