// js/cart-utils.js

const CART_KEY = 'nblb_cart';

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
