// js/products.js

document.addEventListener('DOMContentLoaded', () => {
    loadProducts();
});

async function loadProducts() {
    const container = document.getElementById('productsContainer');
    if (!container) return;

    try {
        const res = await fetch(`${API_BASE}/order-service/products`);
        if (!res.ok) {
            container.innerHTML = '<div class="col-12 text-center"><p>Erreur lors du chargement des produits.</p></div>';
            return;
        }

        const products = await res.json();
        if (!products || products.length === 0) {
            container.innerHTML = '<div class="col-12 text-center"><p>Aucun produit disponible pour le moment.</p></div>';
            return;
        }

        container.innerHTML = '';
        products.forEach((p, idx) => {
            const delay = 0.1 + (idx % 4) * 0.2;
            const safeName = String(p.name || '');
            const safeDesc = String(p.description || '');
            const imageUrl = p.imageUrl || 'img/product-1.jpg';

            container.innerHTML += `
        <div class="col-xl-3 col-lg-4 col-md-6 wow fadeInUp" data-wow-delay="${delay}s">
          <div class="product-item">
            <div class="position-relative bg-light overflow-hidden">
              <img class="img-fluid w-100" src="${imageUrl}" alt="${safeName}">
              <div class="bg-secondary rounded text-white position-absolute start-0 top-0 m-4 py-1 px-3">
                ${p.category || 'New'}
              </div>
            </div>
            <div class="text-center p-4">
              <a class="d-block h5 mb-2" href="#">${safeName}</a>
              <span class="text-primary me-1">${p.price} DT</span>
            </div>
            <div class="d-flex border-top">
              <small class="w-50 text-center border-end py-2">
                <span class="text-body"><i class="fa fa-info-circle text-primary me-2"></i>${safeDesc}</span>
              </small>
              <small class="w-50 text-center py-2">
                <a href="#" class="text-body add-to-cart"
                   data-id="${p.id}"
                   data-name="${encodeHtmlAttr(safeName)}"
                   data-price="${p.price}"
                   data-image="${encodeHtmlAttr(p.imageUrl || '')}">
                  <i class="fa fa-shopping-bag text-primary me-2"></i>Add to cart
                </a>
              </small>
            </div>
          </div>
        </div>`;
        });

        // Delegate clicks for dynamically created items
        container.addEventListener('click', (e) => {
            const link = e.target.closest('.add-to-cart');
            if (!link) return;
            e.preventDefault();

            const id = parseInt(link.dataset.id, 10);
            if (!Number.isFinite(id)) return;

            addToCart({
                id,
                name: link.dataset.name || '',
                price: parseFloat(link.dataset.price) || 0,
                imageUrl: link.dataset.image || ''
            });

            alert('Produit ajouté au panier');
        });
    } catch (err) {
        console.error(err);
        container.innerHTML = '<div class="col-12 text-center"><p>Erreur réseau.</p></div>';
    }
}

function encodeHtmlAttr(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;');
}
