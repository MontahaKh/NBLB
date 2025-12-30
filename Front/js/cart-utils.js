// js/cart-utils.js

// Keep the cart key consistent across pages
const CART_KEY = 'cart';

function loadCart() {
    const raw = localStorage.getItem(CART_KEY);
    if (!raw) return [];
    try {
        return JSON.parse(raw);
    } catch (e) {
        console.error('Invalid cart data', e);
        return [];
    }
}

function saveCart(cart) {
    localStorage.setItem(CART_KEY, JSON.stringify(cart));
}

// product = {id, name, price, imageUrl}
function addToCart(product) {
    let cart = loadCart();
    const existing = cart.find(p => p.id === product.id);
    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({ ...product, quantity: 1 });
    }
    saveCart(cart);
    updateCartCount();
    if (typeof updateCartBadge === 'function') updateCartBadge();
    showCartMessage('Product added to cart');
}

function showCartMessage(message) {
    try {
        let container = document.getElementById('cartMessageContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'cartMessageContainer';
            container.style.position = 'fixed';
            container.style.top = '100px';
            container.style.right = '20px';
            container.style.zIndex = '9999';
            container.style.maxWidth = '350px';
            document.body.appendChild(container);
        }

        const alertEl = document.createElement('div');
        alertEl.className = 'alert alert-success border-0 shadow-lg';
        alertEl.style.backgroundColor = '#28a745';
        alertEl.style.color = 'white';
        alertEl.style.padding = '12px 16px';
        alertEl.style.borderRadius = '6px';
        alertEl.style.marginBottom = '12px';
        alertEl.style.display = 'flex';
        alertEl.style.alignItems = 'center';
        alertEl.style.gap = '10px';
        alertEl.style.animation = 'slideIn 0.3s ease-out';

        // Add icon and message
        const icon = document.createElement('i');
        icon.className = 'fa fa-check-circle';
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

        // Add slide-in animation if not already defined
        if (!document.getElementById('cartNotificationStyles')) {
            const style = document.createElement('style');
            style.id = 'cartNotificationStyles';
            style.textContent = `
                @keyframes slideIn {
                    from {
                        transform: translateX(400px);
                        opacity: 0;
                    }
                    to {
                        transform: translateX(0);
                        opacity: 1;
                    }
                }
                @keyframes slideOut {
                    from {
                        transform: translateX(0);
                        opacity: 1;
                    }
                    to {
                        transform: translateX(400px);
                        opacity: 0;
                    }
                }
            `;
            document.head.appendChild(style);
        }

        setTimeout(() => {
            alertEl.style.animation = 'slideOut 0.3s ease-in forwards';
            setTimeout(() => {
                alertEl.remove();
                if (container && container.childElementCount === 0) {
                    container.remove();
                }
            }, 300);
        }, 2500);
    } catch (error) {
        // Silently fail - the notification system may have an issue but don't show alert
        console.error('Cart notification error:', error);
    }
}

function removeFromCart(productId) {
    let cart = loadCart().filter(p => p.id !== productId);
    saveCart(cart);
    updateCartCount();
    if (typeof updateCartBadge === 'function') updateCartBadge();
}

function updateQuantity(productId, quantity) {
    let cart = loadCart();
    const item = cart.find(p => p.id === productId);
    if (item) {
        item.quantity = quantity;
        if (item.quantity <= 0) {
            cart = cart.filter(p => p.id !== productId);
        }
        saveCart(cart);
        updateCartCount();
        if (typeof updateCartBadge === 'function') updateCartBadge();
    }
}

function getCartTotal() {
    return loadCart().reduce((sum, p) => sum + p.price * p.quantity, 0);
}

function updateCartCount() {
    const countEl = document.getElementById('cartCount');
    if (!countEl) return;
    const totalQty = loadCart().reduce((sum, p) => sum + p.quantity, 0);
    countEl.textContent = totalQty;
}

document.addEventListener('DOMContentLoaded', updateCartCount);
