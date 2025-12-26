function getCart() {
    try {
        return JSON.parse(localStorage.getItem('cart')) || [];
    } catch {
        return [];
    }
}

function saveCart(cart) {
    localStorage.setItem('cart', JSON.stringify(cart));
}

async function addToCart(productId) {
    // Ici on peut recharger le produit complet (au cas où) ou juste stocker l'id
    const cart = getCart();
    const existing = cart.find(item => item.productId === productId);
    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({ productId, quantity: 1 });
    }
    saveCart(cart);
    alert('Product added to cart'); // à remplacer par un bandeau plus joli
}
