let _tmCart = null;
let _tmPayContext = null;
let _tmPayModal = null;
let _tmBusy = false;

function initCheckout() {
  refreshCartCount();
  loadSummary();

  const form = document.getElementById('checkout-form');
  if (form) {
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      placeOrderAndPay();
    });
  }

  const payBtn = document.getElementById('payhere-pay-btn');
  const failBtn = document.getElementById('payhere-fail-btn');
  if (payBtn) payBtn.addEventListener('click', () => payHereComplete('SUCCESS'));
  if (failBtn) failBtn.addEventListener('click', () => payHereComplete('FAILED'));
}

async function loadSummary() {
  const host = document.getElementById('summary-host');
  const placeBtn = document.getElementById('place-order-btn');
  try {
    const cart = await apiCall('GET', '/cart');
    _tmCart = cart;
    const items = (cart && cart.items) || [];

    if (items.length === 0) {
      _tmCart = null;
      if (placeBtn) placeBtn.disabled = true;
      host.innerHTML =
        '<div class="text-center text-muted py-4">' +
        '<p class="mb-3">Your cart is empty.</p>' +
        '<a class="btn btn-primary" href="' + APP_BASE + 'products.html">Browse products</a>' +
        '</div>';
      return;
    }

    let rows = '';
    items.forEach((it) => {
      const name = it.productName || it.name || ('Product #' + it.productId);
      const qty = Number(it.quantity) || 0;
      const line = it.subtotal != null ? it.subtotal
        : (it.unitPrice != null ? Number(it.unitPrice) * qty
          : (it.price != null ? Number(it.price) * qty : 0));
      rows +=
        '<li class="list-group-item d-flex justify-content-between align-items-start px-0">' +
        '<div class="me-2">' +
        '<div class="fw-medium">' + escapeHtml(name) + '</div>' +
        '<small class="text-muted">Qty ' + escapeHtml(String(qty)) + '</small>' +
        '</div>' +
        '<span class="text-nowrap">' + escapeHtml(formatCurrency(line)) + '</span>' +
        '</li>';
    });

    host.innerHTML =
      '<ul class="list-group list-group-flush mb-3">' + rows + '</ul>' +
      '<div class="d-flex justify-content-between border-top pt-3">' +
      '<span class="h6 mb-0">Total (' + escapeHtml(String(cart.itemCount)) + ' item' + (cart.itemCount === 1 ? '' : 's') + ')</span>' +
      '<span class="h5 mb-0 text-primary">' + escapeHtml(formatCurrency(cart.total)) + '</span>' +
      '</div>';

    if (placeBtn) placeBtn.disabled = false;
  } catch (e) {

    if (placeBtn) placeBtn.disabled = true;
    host.innerHTML =
      '<div class="text-center text-muted py-4">' +
      '<p class="mb-3">Could not load your cart.</p>' +
      '<button class="btn btn-outline-secondary" type="button" onclick="loadSummary()">Retry</button>' +
      '</div>';
  }
}

async function placeOrderAndPay() {
  if (_tmBusy) return;

  const form = document.getElementById('checkout-form');
  if (form && !form.checkValidity()) {
    form.classList.add('was-validated');
    return;
  }
  if (form) form.classList.remove('was-validated');

  if (!_tmCart || !_tmCart.items || _tmCart.items.length === 0) {
    showToast('Your cart is empty', 'warning');
    return;
  }

  const shippingAddress = buildShippingAddress();

  const items = _tmCart.items.map((it) => ({
    productId: it.productId,
    quantity: it.quantity
  }));

  setBusy(true);
  try {
    const order = await apiCall('POST', '/orders', { items, shippingAddress });
    if (!order || order.id == null) {
      showToast('Order could not be created', 'danger');
      setBusy(false);
      return;
    }
    refreshCartCount();

    const sim = document.getElementById('use-simulated');
    if (sim && sim.checked) {

      const payment = await apiCall('POST', '/payments/initiate', { orderId: order.id, method: 'VISA' });
      _tmPayContext = {
        orderId: order.id,
        reference: payment ? payment.paymentReference : null,
        amount: (payment && payment.amount != null) ? payment.amount
          : (order.totalAmount != null ? order.totalAmount : _tmCart.total)
      };
      openPayHereModal(_tmPayContext);
    } else {

      await startRealPayHere(order);
    }
  } catch (e) {

    setBusy(false);
  }
}

function buildShippingAddress() {
  const v = (id) => {
    const el = document.getElementById(id);
    return el ? el.value.trim() : '';
  };
  const parts = [
    v('ship-name'),
    v('ship-line1'),
    v('ship-line2'),
    [v('ship-city'), v('ship-postal')].filter(Boolean).join(' '),
    v('ship-phone') ? ('Tel: ' + v('ship-phone')) : ''
  ].filter(Boolean);
  return parts.join(', ');
}

async function startRealPayHere(order) {
  if (typeof payhere === 'undefined' || !payhere || typeof payhere.startPayment !== 'function') {
    showToast('PayHere SDK did not load. Tick "Use simulated gateway" to test locally.', 'danger');
    setBusy(false);
    return;
  }

  let c;
  try {
    c = await apiCall('POST', '/payments/payhere/start', { orderId: order.id });
  } catch (e) {
    setBusy(false);
    return;
  }

  payhere.onCompleted = function () {

    showToast('Payment submitted — confirming your order…', 'success');
    location.href = APP_BASE + 'order-confirmation.html?id=' + encodeURIComponent(order.id);
  };
  payhere.onDismissed = function () {
    showToast('Payment cancelled. Your order is awaiting payment.', 'warning');
    setBusy(false);
  };
  payhere.onError = function (error) {
    showToast('PayHere error: ' + error, 'danger');
    setBusy(false);
  };

  const payment = {
    sandbox: c.sandbox,
    merchant_id: c.merchantId,
    return_url: c.returnUrl,
    cancel_url: c.cancelUrl,
    notify_url: c.notifyUrl,
    order_id: c.orderId,
    items: c.items,
    amount: c.amount,
    currency: c.currency,
    hash: c.hash,
    first_name: c.firstName,
    last_name: c.lastName,
    email: c.email,
    phone: c.phone,
    address: c.address,
    city: c.city,
    country: c.country
  };
  payhere.startPayment(payment);
}

function openPayHereModal(ctx) {
  const amtEl = document.getElementById('payhere-amount');
  const refEl = document.getElementById('payhere-ref');
  if (amtEl) amtEl.textContent = formatCurrency(ctx.amount);
  if (refEl) refEl.textContent = ctx.reference || '—';

  const modalEl = document.getElementById('payhere-modal');
  if (!modalEl) return;
  _tmPayModal = bootstrap.Modal.getOrCreateInstance(modalEl);
  _tmPayModal.show();
}

async function payHereComplete(status) {
  if (_tmBusy) return;
  if (!_tmPayContext) return;

  const isSuccess = status === 'SUCCESS';
  const spinnerId = isSuccess ? 'payhere-pay-spinner' : 'payhere-fail-spinner';
  toggleSpinner(spinnerId, true);
  setModalButtonsDisabled(true);
  _tmBusy = true;

  try {
    await apiCall('POST', '/payments/callback', {
      orderId: _tmPayContext.orderId,
      status: status,
      reference: _tmPayContext.reference
    });

    if (isSuccess) {
      const id = _tmPayContext.orderId;
      if (_tmPayModal) _tmPayModal.hide();
      showToast('Payment successful! Redirecting…', 'success');
      location.href = APP_BASE + 'order-confirmation.html?id=' + encodeURIComponent(id);
      return;
    }

    showToast('Payment failed. You can try paying again.', 'danger');
    toggleSpinner(spinnerId, false);
    setModalButtonsDisabled(false);
    _tmBusy = false;
  } catch (e) {

    toggleSpinner(spinnerId, false);
    setModalButtonsDisabled(false);
    _tmBusy = false;
  }
}

function setBusy(busy) {
  _tmBusy = busy;
  const btn = document.getElementById('place-order-btn');
  if (btn) btn.disabled = busy || !_tmCart;
  toggleSpinner('place-order-spinner', busy);
}

function setModalButtonsDisabled(disabled) {
  const pay = document.getElementById('payhere-pay-btn');
  const fail = document.getElementById('payhere-fail-btn');
  if (pay) pay.disabled = disabled;
  if (fail) fail.disabled = disabled;
}

function toggleSpinner(id, show) {
  const el = document.getElementById(id);
  if (el) el.classList.toggle('d-none', !show);
}
