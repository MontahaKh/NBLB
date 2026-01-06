document.addEventListener('DOMContentLoaded', () => {
    loadRecommendations();
});

async function loadRecommendations() {
    const container = document.getElementById('recommendationsContainer');
    const list = document.getElementById('recommendationList');
    const token = localStorage.getItem('token');

    // If not logged in, keep the default "Please login" message
    if (!token) {
        return;
    }

    try {
        // Show loading state
        list.innerHTML = '<div class="col-12 text-center"><div class="spinner-border text-primary" role="status"></div><p>Asking AI...</p></div>';

        const response = await fetch(`${API_BASE}/api/recommendations`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const products = await response.json();
            if (products.length > 0) {
                renderRecommendations(products, container);
            } else {
                list.innerHTML = '<div class="col-12 text-center"><p>No recommendations available at the moment. Try buying something first!</p></div>';
            }
        } else {
            console.warn('Failed to fetch recommendations:', response.status);
            list.innerHTML = '<div class="col-12 text-center"><p class="text-danger">Unable to load recommendations. Please check your connection.</p></div>';
        }
    } catch (error) {
        console.error('Error fetching recommendations:', error);
        if (list) list.innerHTML = `<div class="col-12 text-center"><p class="text-danger">Error: ${error.message}</p></div>`;
    }
}

function renderRecommendations(products, container) {
    const list = document.getElementById('recommendationList');
    list.innerHTML = '';

    products.forEach((product, index) => {
        // First item is the "Most Bought" (if available), distinguish it
        const isMostBought = index === 0;
        const badgeText = isMostBought ? 'Your Favorite' : 'Recommended';
        const badgeClass = isMostBought ? 'bg-danger' : 'bg-secondary'; // Red for favorite, Grey for others

        const productHTML = `
            <div class="col-xl-3 col-lg-4 col-md-6 wow fadeInUp" data-wow-delay="${0.1 + (index * 0.1)}s">
                <div class="product-item">
                    <div class="position-relative bg-light overflow-hidden">
                        <img class="img-fluid w-100" src="${product.imageUrl || 'img/product-1.jpg'}" alt="${product.name}" style="height: 200px; object-fit: cover;">
                        <div class="${badgeClass} rounded text-white position-absolute start-0 top-0 m-4 py-1 px-3">${badgeText}</div>
                    </div>
                    <div class="text-center p-4">
                        <a class="d-block h5 mb-2" href="">${product.name}</a>
                        <span class="text-primary me-1">$${product.price}</span>
                    </div>
                    <div class="d-flex border-top">
                        <small class="w-100 text-center border-end py-2">
                            <a class="text-body" href="#" onclick="addToCart(${product.id}); return false;">
                                <i class="fa fa-shopping-bag text-primary me-2"></i>Add to Cart
                            </a>
                        </small>
                    </div>
                </div>
            </div>
        `;
        list.insertAdjacentHTML('beforeend', productHTML);
    });
}
