// js/payment.js

document.addEventListener('DOMContentLoaded', () => {
    const params = new URLSearchParams(window.location.search);
    const orderId = params.get('orderId');
    const amount = parseFloat(params.get('amount'));

    console.log('URL Search Params:', window.location.search);
    console.log('Order ID:', orderId);
    console.log('Amount (raw):', params.get('amount'));
    console.log('Amount (parsed):', amount);

    if (!orderId || isNaN(amount)) {
        console.error('Missing payment information. orderId:', orderId, 'amount:', amount);
        // Redirect to cart instead of showing alert
        window.location.href = 'cart.html';
        return;
    }

    const amountInput = document.getElementById('paymentAmount');
    if (amountInput) {
        amountInput.value = amount.toFixed(2);
        console.log('Amount input set to:', amountInput.value);
    } else {
        console.error('paymentAmount input element not found');
    }

    const form = document.getElementById('paymentForm');
    const errorEl = document.getElementById('paymentError');
    const successEl = document.getElementById('paymentSuccess');

    if (!form) {
        console.error('paymentForm not found');
        return;
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        errorEl.style.display = 'none';
        successEl.style.display = 'none';

        const method = document.querySelector('input[name="paymentMethod"]:checked')?.value || 'CARD';
        const token = getToken();

        try {
            // Process the payment
            const paymentRes = await fetch(`${API_BASE}/payment/api/process`, {
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

            if (!paymentRes.ok) {
                errorEl.style.display = 'block';
                return;
            }

            // After successful payment, reduce stock for each item in the cart
            try {
                const cart = JSON.parse(localStorage.getItem('cart')) || [];
                if (cart && cart.length > 0) {
                    // Send stock reduction request to backend
                    const stockRes = await fetch(`${API_BASE}/order-service/api/reduce-stock`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            ...(token ? { 'Authorization': `Bearer ${token}` } : {})
                        },
                        body: JSON.stringify({
                            items: cart.map(item => ({
                                productId: item.id,
                                quantity: item.quantity
                            }))
                        })
                    });

                    if (!stockRes.ok) {
                        console.error('Warning: Stock reduction failed, but payment was successful');
                        // Continue anyway - payment is more important than stock update
                    }

                    // Clear the cart after stock reduction attempt
                    localStorage.removeItem('cart');
                }
            } catch (stockErr) {
                console.error('Error reducing stock:', stockErr);
                // Continue to orders page - payment was successful
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
