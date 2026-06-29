async function loadCart() {
  const host = document.getElementById('tm-cart');
  if (host) host.innerHTML = '<div class="text-center text-muted py-5">Loading cart…</div>';
  try {
    const cart = await apiCall('GET', '/cart');
    renderCart(cart);
    refreshCartCount();
  } catch (e) {
    if (host) {
      host.innerHTML = '<div class="alert alert-danger">Could not load your cart. Please try again.</div>';
    }
  }
}

function renderCart(cart) {
  const host = document.getElementById('tm-cart');
  if (!host) return;

  const items = (cart && Array.isArray(cart.items)) ? cart.items : [];

  if (items.length === 0) {
    host.innerHTML = `
      <div class="text-center py-5">
        <h4 class="text-muted mb-3">Your cart is empty</h4>
        <p class="text-muted">Browse our catalogue and add something you like.</p>
        <a href="${APP_BASE}products.html" class="btn btn-primary">Shop products</a>
      </div>`;
    return;
  }

  const rows = items.map(it => {
    const productId = it.productId;
    const name = escapeHtml(it.productName || it.name || ('Product #' + productId));
    const img = it.imageUrl || it.productImage || '';
    const unit = (it.unitPrice !== undefined && it.unitPrice !== null) ? it.unitPrice : it.price;
    const qty = (it.quantity !== undefined && it.quantity !== null) ? it.quantity : 1;
    const lineTotal = (it.subtotal !== undefined && it.subtotal !== null)
      ? it.subtotal
      : (Number(unit || 0) * Number(qty));
    const imgCell = img
      ? `<img src="${escapeHtml(img)}" alt="${name}" class="rounded" style="width:56px;height:56px;object-fit:cover">`
      : `<div class="bg-light rounded d-flex align-items-center justify-content-center text-muted" style="width:56px;height:56px">—</div>`;
    return `
      <tr data-product-id="${escapeHtml(String(productId))}">
        <td style="width:72px">${imgCell}</td>
        <td>
          <div class="fw-semibold">${name}</div>
        </td>
        <td class="text-end">${escapeHtml(formatCurrency(unit))}</td>
        <td class="text-center" style="width:170px">
          <div class="input-group input-group-sm justify-content-center" style="max-width:150px;margin:auto">
            <button type="button" class="btn btn-outline-secondary" onclick="changeQty(${Number(productId)}, ${Number(qty) - 1})" ${Number(qty) <= 1 ? 'disabled' : ''}>−</button>
            <input type="text" class="form-control text-center" value="${escapeHtml(String(qty))}" readonly>
            <button type="button" class="btn btn-outline-secondary" onclick="changeQty(${Number(productId)}, ${Number(qty) + 1})">+</button>
          </div>
        </td>
        <td class="text-end fw-semibold">${escapeHtml(formatCurrency(lineTotal))}</td>
        <td class="text-end" style="width:48px">
          <button type="button" class="btn btn-sm btn-outline-danger" title="Remove" onclick="removeItem(${Number(productId)})">&times;</button>
        </td>
      </tr>`;
  }).join('');

  const total = (cart && cart.total !== undefined && cart.total !== null) ? cart.total : 0;

  host.innerHTML = `
    <div class="table-responsive">
      <table class="table align-middle">
        <thead>
          <tr>
            <th></th>
            <th>Product</th>
            <th class="text-end">Unit price</th>
            <th class="text-center">Quantity</th>
            <th class="text-end">Line total</th>
            <th></th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
    <div class="row justify-content-end g-3 mt-2">
      <div class="col-12 col-md-5 col-lg-4">
        <div class="card">
          <div class="card-body">
            <div class="d-flex justify-content-between mb-2">
              <span class="text-muted">Subtotal</span>
              <span>${escapeHtml(formatCurrency(total))}</span>
            </div>
            <div class="d-flex justify-content-between fw-bold fs-5 border-top pt-2">
              <span>Total</span>
              <span>${escapeHtml(formatCurrency(total))}</span>
            </div>
            <div class="d-grid gap-2 mt-3">
              <a href="${APP_BASE}checkout.html" class="btn btn-success">Proceed to checkout</a>
              <button type="button" class="btn btn-outline-danger" onclick="clearCart()">Clear cart</button>
            </div>
          </div>
        </div>
      </div>
    </div>`;
}

async function changeQty(productId, qty) {
  if (qty < 1) return removeItem(productId);
  try {
    const cart = await apiCall('PUT', '/cart/items/' + encodeURIComponent(productId), { quantity: qty });
    renderCart(cart);
    refreshCartCount();
  } catch (e) {  }
}

async function removeItem(productId) {
  try {
    const cart = await apiCall('DELETE', '/cart/items/' + encodeURIComponent(productId));
    renderCart(cart);
    refreshCartCount();
    showToast('Item removed from cart', 'info');
  } catch (e) {  }
}

async function clearCart() {
  try {
    await apiCall('DELETE', '/cart');
    showToast('Cart cleared', 'info');
    loadCart();
  } catch (e) {  }
}
