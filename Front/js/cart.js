// js/cart.js

function showError(message) {
    // Show error notification if available, otherwise console
    if (typeof showCartMessage === 'function') {
        // Create error version of showCartMessage
        const container = document.getElementById('cartMessageContainer') || (() => {
            const c = document.createElement('div');
            c.id = 'cartMessageContainer';
            c.style.position = 'fixed';
            c.style.top = '100px';
            c.style.right = '20px';
            c.style.zIndex = '9999';
            c.style.maxWidth = '350px';
            document.body.appendChild(c);
            return c;
        })();

        const alertEl = document.createElement('div');
        alertEl.style.backgroundColor = '#dc3545';
        alertEl.style.color = 'white';
        alertEl.style.padding = '12px 16px';
        alertEl.style.borderRadius = '6px';
        alertEl.style.marginBottom = '12px';
        alertEl.style.display = 'flex';
        alertEl.style.alignItems = 'center';
        alertEl.style.gap = '10px';
        alertEl.style.animation = 'slideIn 0.3s ease-out';

        const icon = document.createElement('i');
        icon.className = 'fa fa-exclamation-circle';
        icon.style.fontSize = '18px';
        icon.style.flexShrink = '0';

        const text = document.createElement('span');
        text.textContent = message;
        text.style.flex = '1';
        text.style.fontSize = '14px';
        text.style.fontWeight = '500';

        alertEl.appendChild(icon);
        alertEl.appendChild(text);
        container.appendChild(alertEl);

        setTimeout(() => {
            alertEl.style.animation = 'slideOut 0.3s ease-in forwards';
            setTimeout(() => {
                alertEl.remove();
                if (container && container.childElementCount === 0) {
                    container.remove();
                }
            }, 300);
        }, 3500);
    } else {
        console.error(message);
    }
}

async function checkoutCart() {
    try {
        const cart = loadCart();
        if (!cart || cart.length === 0) {
            showError('Your cart is empty');
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
            showError(message);
            return;
        }

        if (!data || !data.orderId || typeof data.total !== 'number') {
            showError('Invalid response from server');
            return;
        }

        const orderId = data.orderId;
        const total = data.total;

        saveCart([]);
        updateCartCount();

        window.location.href = `payment.html?orderId=${orderId}&amount=${total}`;
    } catch (e) {
        console.error(e);
        showError('Unexpected error during checkout');
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
        const imageUrl = item.imageUrl || 'img/product-1.jpg';
        tr.innerHTML = `
      <td>
        <img src="${imageUrl}" alt="${item.name}" style="width: 50px; height: 50px; object-fit: contain; border-radius: 4px;">
      </td>
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
