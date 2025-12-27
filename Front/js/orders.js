// js/orders.js

document.addEventListener('DOMContentLoaded', () => {
    if (!getToken()) {
        window.location.href = 'login.html';
        return;
    }
    loadOrders();
});

async function loadOrders() {
    const tbody = document.getElementById('ordersBody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="5" class="text-center">Loading...</td></tr>';

    try {
        const res = await fetch(`${API_BASE}/order-service/api/orders/me`, {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + getToken()
            }
        });

        if (!res.ok) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Failed to load orders.</td></tr>';
            return;
        }

        const orders = await res.json();

        if (!orders || orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">No orders found.</td></tr>';
            return;
        }

        tbody.innerHTML = '';
        orders.forEach(o => {
            const tr = document.createElement('tr');

            const total = typeof o.total === 'number' ? o.total : Number(o.total || 0);
            const totalText = Number.isFinite(total) ? total.toFixed(2) : '0.00';

            let dateText = o.orderDate || '';
            try {
                if (o.orderDate) {
                    dateText = new Date(o.orderDate).toLocaleString();
                }
            } catch {
                // keep raw
            }

                        const status = (o.status || '').toUpperCase();
                        const canPay = status === 'PENDING';
                        const payBtn = canPay
                                ? `<button class="btn btn-sm btn-success pay-now" data-id="${o.id}" data-amount="${totalText}">Pay now</button>`
                                : '';

                        tr.innerHTML = `
                <td>${o.id}</td>
                <td>${dateText}</td>
                <td>${totalText} DT</td>
                <td>${o.status || ''}</td>
                <td>${payBtn}</td>
            `;

            tbody.appendChild(tr);
        });

        tbody.addEventListener('click', (e) => {
            const btn = e.target && e.target.classList && e.target.classList.contains('pay-now')
                ? e.target
                : null;
            if (!btn) return;

            const orderId = btn.getAttribute('data-id');
            const amount = btn.getAttribute('data-amount');
            window.location.href = `payment.html?orderId=${encodeURIComponent(orderId)}&amount=${encodeURIComponent(amount)}`;
        }, { once: true });

    } catch (err) {
        console.error(err);
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Network error.</td></tr>';
    }
}
