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

            let actionBtns = `<button class="btn btn-sm btn-info text-white me-2 view-btn" 
                            data-oid="${o.id}" 
                            data-products='${JSON.stringify(o.productNames || []).replace(/'/g, "&apos;")}'
                            data-status="${o.status || ''}"
                            data-total="${totalText}"
                            data-canpay="${canPay}">
                            <i class="fa fa-eye"></i> View
                        </button>`;

            if (canPay) {
                actionBtns += `<button class="btn btn-sm btn-success pay-now" data-id="${o.id}" data-amount="${totalText}">Pay</button>`;
            }

            tr.innerHTML = `
                <td>${o.id}</td>
                <td>${dateText}</td>
                <td>${totalText} DT</td>
                <td>${o.status || ''}</td>
                <td>${actionBtns}</td>
            `;

            tbody.appendChild(tr);
        });

        // Event delegation
        tbody.onclick = (e) => {
            const payBtn = e.target.closest('.pay-now');
            const viewBtn = e.target.closest('.view-btn');

            if (payBtn) {
                const orderId = payBtn.getAttribute('data-id');
                const amount = payBtn.getAttribute('data-amount');
                window.location.href = `payment.html?orderId=${encodeURIComponent(orderId)}&amount=${encodeURIComponent(amount)}`;
                return;
            }

            if (viewBtn) {
                showOrderDetails(viewBtn.dataset);
            }
        };

    } catch (err) {
        console.error(err);
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Network error.</td></tr>';
    }
}

function showOrderDetails(data) {
    document.getElementById('modalOrderId').textContent = '#' + data.oid;
    document.getElementById('modalOrderStatus').textContent = data.status;
    document.getElementById('modalOrderTotal').textContent = data.total + ' DT';

    const list = document.getElementById('modalProductList');
    list.innerHTML = '';

    let products = [];
    try {
        products = JSON.parse(data.products);
    } catch (e) { console.error('Error parsing products', e); }

    if (products.length === 0) {
        list.innerHTML = '<li class="list-group-item">No product information available</li>';
    } else {
        products.forEach(p => {
            const li = document.createElement('li');
            li.className = 'list-group-item';
            li.textContent = p;
            list.appendChild(li);
        });
    }

    // Configure Pay button in modal
    const payBtn = document.getElementById('modalPayBtn');
    if (data.canpay === 'true') {
        payBtn.classList.remove('d-none');
        payBtn.onclick = () => {
            window.location.href = `payment.html?orderId=${data.oid}&amount=${data.total}`;
        };
    } else {
        payBtn.classList.add('d-none');
    }

    const modal = new bootstrap.Modal(document.getElementById('orderModal'));
    modal.show();
}
