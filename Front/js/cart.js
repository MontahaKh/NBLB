// js/cart.js

async function checkoutCart() {
    try {
        const cart = loadCart();
        if (!cart || cart.length === 0) {
            alert('Your cart is empty');
            return;
        }

        const token = getToken();
        const res = await fetch(`${API_BASE}/order-service/api/checkout`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(token ? { 'Authorization': `Bearer ${token}` } : {})
            },
            body: JSON.stringify({
                items: cart.map(c => ({
                    productId: c.id,
                    name: c.name,
                    price: c.price,
                    quantity: c.quantity
                })),
                total: getCartTotal()
            })
        });

        const data = await res.json().catch(() => null);

        if (!res.ok) {
            const message = data && data.error ? data.error : 'Checkout failed';
            alert(message);
            return;
        }

        if (!data || !data.orderId || typeof data.total !== 'number') {
            alert('Invalid response from server');
            return;
        }

        const orderId = data.orderId;
        const total = data.total;

        saveCart([]);
        updateCartCount();

        window.location.href = `payment.html?orderId=${orderId}&amount=${total}`;
    } catch (e) {
        console.error(e);
        alert('Unexpected error during checkout');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (!getToken()) {
        window.location.href = 'login.html';
        return;
    }
    renderCart();

    document.getElementById('checkoutBtn')
        ?.addEventListener('click', checkoutCart);
});

function renderCart() {
    const tbody = document.getElementById('cartBody');
    const cart = loadCart();

    if (!cart.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">Your cart is empty.</td></tr>';
        document.getElementById('cartTotal').textContent = '0';
        return;
    }

    tbody.innerHTML = '';
    cart.forEach(item => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
      <td>${item.name}</td>
      <td>${item.price.toFixed(2)}</td>
      <td style="max-width:120px;">
        <input type="number" min="1" value="${item.quantity}"
               class="form-control form-control-sm cart-qty"
               data-id="${item.id}">
      </td>
      <td>${(item.price * item.quantity).toFixed(2)}</td>
      <td>
        <button class="btn btn-sm btn-outline-danger remove-item" data-id="${item.id}">Remove</button>
      </td>
    `;
        tbody.appendChild(tr);
    });

    document.getElementById('cartTotal').textContent = getCartTotal().toFixed(2);

    tbody.addEventListener('change', e => {
        if (e.target.classList.contains('cart-qty')) {
            const id = parseInt(e.target.dataset.id, 10);
            const qty = parseInt(e.target.value, 10);
            updateQuantity(id, qty);
            renderCart();
        }
    });

    tbody.addEventListener('click', e => {
        if (e.target.classList.contains('remove-item')) {
            const id = parseInt(e.target.dataset.id, 10);
            removeFromCart(id);
            renderCart();
        }
    });
}
