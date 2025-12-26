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

    tbody.innerHTML = '<tr><td colspan="4" class="text-center">Loading...</td></tr>';

    try {
        const res = await fetch(`${API_BASE}/order-service/api/orders/me`, {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + getToken()
            }
        });

        if (!res.ok) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger">Failed to load orders.</td></tr>';
            return;
        }

        const orders = await res.json();

        if (!orders || orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center">No orders found.</td></tr>';
            return;
        }

        tbody.innerHTML = '';
        orders.forEach(o => {
            const tr = document.createElement('tr');

            tr.innerHTML = `
        <td>${o.id}</td>
        <td>${o.orderDate || ''}</td>
        <td>${o.total || 0} DT</td>
        <td>${o.status || ''}</td>
      `;

            tbody.appendChild(tr);
        });

    } catch (err) {
        console.error(err);
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger">Network error.</td></tr>';
    }
}
