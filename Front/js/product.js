// js/product.js

document.addEventListener('DOMContentLoaded', () => {
  loadProducts();
});

async function loadProducts() {
  try {
    const res = await fetch(`${API_BASE}/order-service/products`);
    const products = await res.json();

    const container = document.getElementById('productsContainer');
    if (!container) return;

    container.innerHTML = '';
    products.forEach(p => {
      container.innerHTML += `
        <div class="col-lg-3 col-md-6">
          <div class="product-item">
            <div class="position-relative bg-light overflow-hidden">
              <img class="img-fluid w-100" src="${p.imageUrl || 'img/product-placeholder.jpg'}" alt="${p.name}">
            </div>
            <div class="text-center p-4">
              <h5 class="mb-2">${p.name}</h5>
              <p class="mb-2 small">${p.description || ''}</p>
              <h4 class="text-primary mb-3">${p.price} DT</h4>
              <button
                class="btn btn-outline-primary add-to-cart"
                data-id="${p.id}"
                data-name="${encodeHtmlAttr(p.name || '')}"
                data-price="${p.price}"
                data-image="${encodeHtmlAttr(p.imageUrl || '')}">
                Add to Cart
              </button>
            </div>
          </div>
        </div>
      `;
    });

    attachAddToCart(container);
  } catch (err) {
    console.error(err);
  }
}

function attachAddToCart(container) {
  // One delegated handler for dynamic buttons
  container.addEventListener('click', (e) => {
    const btn = e.target.closest('.add-to-cart');
    if (!btn) return;
    e.preventDefault();

    const id = parseInt(btn.dataset.id, 10);
    if (!Number.isFinite(id)) return;

    addToCart({
      id,
      name: btn.dataset.name || '',
      price: parseFloat(btn.dataset.price) || 0,
      imageUrl: btn.dataset.image || ''
    });
    alert('Produit ajouté au panier');
  });
}

function encodeHtmlAttr(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;');
}
