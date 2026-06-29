const TM_LOW_STOCK_THRESHOLD = 5;

const TM_PRODUCT_PLACEHOLDER =
  'data:image/svg+xml;utf8,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300">' +
    '<rect width="100%" height="100%" fill="#e9ecef"/>' +
    '<text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" ' +
    'fill="#adb5bd" font-family="sans-serif" font-size="20">No image</text></svg>');

function stockState(p) {
  const stock = Number(p.totalStock || 0);
  if (!p.inStock || stock <= 0) {
    return { label: 'Out of stock', cls: 'badge-stock-out' };
  }
  if (stock <= TM_LOW_STOCK_THRESHOLD) {
    return { label: 'Low stock (' + stock + ')', cls: 'badge-stock-low' };
  }
  return { label: 'In stock', cls: 'badge-stock-in' };
}

function stockBadgeHtml(p) {
  const s = stockState(p);
  return '<span class="badge ' + s.cls + '">' + escapeHtml(s.label) + '</span>';
}

function priceHtml(p) {
  const original = p.price;
  const discounted = (p.discountedPrice != null) ? p.discountedPrice : null;
  const hasDiscount = discounted != null && original != null &&
    Number(discounted) < Number(original);
  if (hasDiscount) {
    return '<div class="product-price">' + escapeHtml(formatCurrency(discounted)) +
      ' <span class="old">' + escapeHtml(formatCurrency(original)) + '</span></div>';
  }
  const shown = (p.effectivePrice != null) ? p.effectivePrice
    : (discounted != null ? discounted : original);
  return '<div class="product-price">' + escapeHtml(formatCurrency(shown)) + '</div>';
}

function appUrl(rel) { return APP_BASE + rel; }

function productCardHtml(p) {
  const id = p.id;
  const img = p.imageUrl ? escapeHtml(p.imageUrl) : TM_PRODUCT_PLACEHOLDER;
  const outOfStock = !p.inStock || Number(p.totalStock || 0) <= 0;
  const detail = appUrl('product-detail.html?id=' + encodeURIComponent(id));
  return (
    '<div class="col-12 col-sm-6 col-lg-4 col-xl-3 mb-4">' +
      '<div class="card product-card h-100">' +
        '<a href="' + detail + '">' +
          '<img src="' + img + '" class="card-img-top" alt="' + escapeHtml(p.name || '') + '" ' +
            'onerror="this.onerror=null;this.src=\'' + TM_PRODUCT_PLACEHOLDER + '\'">' +
        '</a>' +
        '<div class="card-body d-flex flex-column">' +
          '<div class="text-muted small mb-1">' + escapeHtml(p.brand || '') + '</div>' +
          '<h6 class="card-title mb-2"><a class="text-decoration-none text-dark" href="' + detail + '">' +
            escapeHtml(p.name || '') + '</a></h6>' +
          priceHtml(p) +
          '<div class="my-2">' + stockBadgeHtml(p) + '</div>' +
          '<div class="mt-auto d-flex gap-2">' +
            '<a href="' + detail + '" class="btn btn-outline-secondary btn-sm flex-fill">View</a>' +
            '<button type="button" class="btn btn-primary btn-sm flex-fill" ' +
              'data-add-to-cart="' + escapeHtml(String(id)) + '"' + (outOfStock ? ' disabled' : '') + '>' +
              (outOfStock ? 'Out of stock' : 'Add to cart') + '</button>' +
          '</div>' +
        '</div>' +
      '</div>' +
    '</div>');
}

function renderProductGrid(products, containerEl) {
  if (!containerEl) return;
  if (!products || products.length === 0) {
    containerEl.innerHTML =
      '<div class="col-12"><div class="alert alert-light border text-center text-muted py-5">' +
      'No products found.</div></div>';
    return;
  }
  containerEl.innerHTML = products.map(productCardHtml).join('');
}

function wireAddToCart(rootEl) {
  const root = rootEl || document;
  if (root.__tmCartWired) return;
  root.__tmCartWired = true;
  root.addEventListener('click', (ev) => {
    const btn = ev.target.closest('[data-add-to-cart]');
    if (!btn) return;
    ev.preventDefault();
    const id = btn.getAttribute('data-add-to-cart');
    addToCart(id, 1);
  });
}

async function addToCart(productId, qty) {
  if (!isLoggedIn()) {
    showToast('Please sign in to add items to your cart', 'warning');
    setTimeout(() => location.href = appUrl('login.html'), 600);
    return;
  }
  const quantity = Math.max(1, parseInt(qty, 10) || 1);
  try {
    await apiCall('POST', '/cart/items', { productId: Number(productId), quantity });
    await refreshCartCount();
    showToast('Added to cart', 'success');
  } catch (e) {

  }
}

async function loadCategories() {
  try {
    const cats = await apiCall('GET', '/products/categories');
    return Array.isArray(cats) ? cats : [];
  } catch (e) {
    return [];
  }
}

function buildProductQuery(opts) {
  const o = opts || {};
  const params = new URLSearchParams();
  if (o.page != null) params.set('page', o.page);
  if (o.size != null) params.set('size', o.size);
  if (o.keyword) params.set('keyword', o.keyword);
  if (o.category) params.set('category', o.category);
  if (o.minPrice !== '' && o.minPrice != null) params.set('minPrice', o.minPrice);
  if (o.maxPrice !== '' && o.maxPrice != null) params.set('maxPrice', o.maxPrice);
  const qs = params.toString();
  return qs ? ('?' + qs) : '';
}

function getQueryParam(name) {
  return new URLSearchParams(location.search).get(name);
}
