// js/seller.js

document.addEventListener('DOMContentLoaded', () => {
    requireSeller();
    loadSellerProducts();
    loadSellerSales();
});

// --------- Liste des produits du vendeur ---------
async function loadSellerProducts() {
    const container = document.getElementById('sellerProducts');
    if (!container) return;

    try {
        const res = await fetch(`${API_BASE}/order-service/my-products`, {
            headers: {
                'Authorization': 'Bearer ' + getToken()
            }
        });

        if (!res.ok) {
            container.innerHTML = '<tr><td colspan="5">Erreur lors du chargement des produits.</td></tr>';
            return;
        }

        const products = await res.json();

        if (!products || products.length === 0) {
            container.innerHTML = '<tr><td colspan="5">Aucun produit trouvé.</td></tr>';
            return;
        }

        container.innerHTML = '';
        products.forEach(p => {
            container.innerHTML += `
        <tr>
          <td>${p.id}</td>
          <td>${p.name}</td>
          <td>${p.category}</td>
          <td>${p.price} DT</td>
          <td>${p.quantity}</td>
        </tr>
      `;
        });
    } catch (err) {
        console.error(err);
        container.innerHTML = '<tr><td colspan="5">Erreur réseau.</td></tr>';
    }
}

// --------- Produits vendus (commandes payées / en livraison / expédiées) ---------
async function loadSellerSales() {
    const tbody = document.getElementById('sellerSales');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="6" class="text-center">Loading...</td></tr>';

    try {
        const res = await fetch(`${API_BASE}/order-service/api/seller/sales`, {
            headers: {
                'Authorization': 'Bearer ' + getToken()
            }
        });

        if (!res.ok) {
            const msg = await res.text().catch(() => null);
            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger">Failed to load sales${msg ? ': ' + msg : ''}</td></tr>`;
            return;
        }

        const sales = await res.json();
        if (!sales || sales.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">No sales yet.</td></tr>';
            return;
        }

        tbody.innerHTML = '';
        sales.forEach(s => {
            const date = s.orderDate ? new Date(s.orderDate).toLocaleString() : '';
            tbody.innerHTML += `
        <tr>
          <td>${s.orderId ?? ''}</td>
          <td>${date}</td>
          <td>${s.buyerUsername ?? ''}</td>
          <td>${s.productName ?? ''}</td>
          <td>${(s.unitPrice ?? 0)} DT</td>
          <td>${s.orderStatus ?? ''}</td>
        </tr>
      `;
        });
    } catch (err) {
        console.error(err);
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Network error.</td></tr>';
    }
}

// --------- Ajout de produit ---------
const addProductForm = document.getElementById('addProductForm');
const addProductError = document.getElementById('addProductError');
const addProductSuccess = document.getElementById('addProductSuccess');

if (addProductForm) {
    addProductForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const product = {
            name: document.getElementById('productName').value.trim(),
            price: parseFloat(document.getElementById('price').value),
            description: document.getElementById('description').value.trim(),
            quantity: parseInt(document.getElementById('quantity').value, 10),
            expiryDate: document.getElementById('expiryDate').value,
            category: document.getElementById('category').value,
            imageUrl: document.getElementById('imageUrl').value.trim()
        };

        if (addProductError) {
            addProductError.style.display = 'none';
            addProductError.textContent = 'Error while saving the product.';
        }
        if (addProductSuccess) {
            addProductSuccess.style.display = 'none';
        }

        try {
            const res = await fetch(`${API_BASE}/order-service/products`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + getToken()
                },
                body: JSON.stringify(product)
            });

            if (!res.ok) {
                const msg = await res.text().catch(() => null);
                if (addProductError) {
                    addProductError.style.display = 'block';
                    if (msg) addProductError.textContent = msg;
                }
                return;
            }

            if (addProductSuccess) addProductSuccess.style.display = 'block';
            addProductForm.reset();
            loadSellerProducts();
        } catch (err) {
            console.error(err);
            if (addProductError) addProductError.style.display = 'block';
        }
    });
}
