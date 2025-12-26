// js/payment.js

document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const orderId = params.get('orderId');
    const amount = parseFloat(params.get('amount'));

    if (!orderId || isNaN(amount)) {
        alert('Missing payment information.');
        window.location.href = 'cart.html';
        return;
    }

    const amountInput = document.getElementById('paymentAmount');
    amountInput.value = amount.toFixed(2);

    const form = document.getElementById('paymentForm');
    const errorEl = document.getElementById('paymentError');
    const successEl = document.getElementById('paymentSuccess');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        errorEl.style.display = 'none';
        successEl.style.display = 'none';

        const method = document.querySelector('input[name="paymentMethod"]:checked')?.value || 'CARD';
        const token = getToken();

        try {
            const res = await fetch(`${API_BASE}/payment-service/api/process`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
                },
                body: JSON.stringify({
                    orderId: Number(orderId),
                    amount: amount,
                    method: method
                })
            });

            if (!res.ok) {
                errorEl.style.display = 'block';
                return;
            }

            successEl.style.display = 'block';

            setTimeout(() => {
                window.location.href = 'orders.html';
            }, 1500);
        } catch (err) {
            console.error(err);
            errorEl.style.display = 'block';
        }
    });
});
