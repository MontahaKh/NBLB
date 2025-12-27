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
        cart.push({...product, quantity: 1});
    }
    saveCart(cart);
    updateCartCount();
    showCartMessage('Product added to cart');
}

function showCartMessage(message) {
    try {
        let container = document.getElementById('cartMessageContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'cartMessageContainer';
            container.style.position = 'fixed';
            container.style.top = '90px';
            container.style.right = '16px';
            container.style.zIndex = '9999';
            document.body.appendChild(container);
        }

        const alertEl = document.createElement('div');
        alertEl.className = 'alert alert-success py-2 px-3 mb-2 shadow-sm';
        alertEl.textContent = message;
        container.appendChild(alertEl);

        setTimeout(() => {
            alertEl.remove();
            if (container && container.childElementCount === 0) {
                container.remove();
            }
        }, 1500);
    } catch {
        // fallback
        alert(message);
    }
}

function removeFromCart(productId) {
    let cart = loadCart().filter(p => p.id !== productId);
    saveCart(cart);
    updateCartCount();
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
