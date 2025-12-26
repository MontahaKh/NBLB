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
              <button class="btn btn-outline-primary add-to-cart" data-id="${p.id}">
                Add to Cart
              </button>
            </div>
          </div>
        </div>
      `;
    });

    attachAddToCart();
  } catch (err) {
    console.error(err);
  }
}

function attachAddToCart() {
  document.querySelectorAll('.add-to-cart').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.preventDefault();
      if (!getToken()) {
        window.location.href = 'login.html';
        return;
      }

      const productId = btn.dataset.id;
      try {
        const res = await fetch(`${API_BASE}/order-service/cart`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + getToken()
          },
          body: JSON.stringify({ productId, quantity: 1 })
        });

        if (res.ok) {
          alert('Produit ajouté au panier');
        } else {
          alert('Erreur lors de l’ajout au panier');
        }
      } catch (err) {
        console.error(err);
        alert('Erreur réseau');
      }
    });
  });


    document.addEventListener('click', e => {
        if (e.target.classList.contains('add-to-cart')) {
            const btn = e.target;
            addToCart({
                id: parseInt(btn.dataset.id),
                name: btn.dataset.name,
                price: parseFloat(btn.dataset.price),
                imageUrl: btn.dataset.image || ''
            });
        }
    });

}
