// Seller Recommendations - Top Sold Items & AI Suggestions
// Loaded in seller-dashboard.html

document.addEventListener('DOMContentLoaded', () => {
    const sellerRecommendationsContainer = document.getElementById('sellerRecommendationsContainer');

    if (sellerRecommendationsContainer) {
        loadSellerRecommendations();
    }
});

async function loadSellerRecommendations() {
    const token = localStorage.getItem('token');
    const userRole = localStorage.getItem('role'); // Changed from 'userRole' to 'role'

    // Only show for sellers (SELLER or SHOP) and admins
    console.log('User role:', userRole);
    if (!token || (userRole !== 'SELLER' && userRole !== 'SHOP' && userRole !== 'ADMIN')) {
        console.log('Not a seller/admin, skipping recommendations. Role:', userRole);
        return;
    }

    try {
        // Step 1: Load top-sold items
        await loadTopSoldItems(token);
    } catch (error) {
        console.error('Error loading seller recommendations:', error);
    }
}

async function loadTopSoldItems(token) {
    try {
        const response = await fetch(`${API_BASE}/api/seller/recommendations/top-sold?limit=10`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            console.warn('Failed to fetch top sold items:', response.status);
            document.getElementById('topSoldList').innerHTML = '<p class="text-warning">Unable to load sales data. Status: ' + response.status + '</p>';
            return;
        }

        const topSoldItems = await response.json();
        console.log('Top sold items:', topSoldItems);

        if (!topSoldItems || topSoldItems.length === 0) {
            document.getElementById('topSoldList').innerHTML = '<p class="text-muted">No sales data available yet.</p>';
            return;
        }

        // Render top-sold items
        renderTopSoldItems(topSoldItems);

        // Step 2: Get AI suggestions based on top-sold items
        await getProductSuggestions(token, topSoldItems);

    } catch (error) {
        console.error('Error loading top sold items:', error);
        document.getElementById('topSoldList').innerHTML = '<p class="text-danger">Error loading sales data: ' + error.message + '</p>';
    }
}

function renderTopSoldItems(items) {
    const list = document.getElementById('topSoldList');
    list.innerHTML = '';

    items.forEach((item, index) => {
        const itemHTML = `
            <div class="list-group-item d-flex justify-content-between align-items-center">
                <div>
                    <h6 class="mb-1">#${index + 1} ${item.name || item.productName}</h6>
                    <small class="text-muted">Category: ${item.category || 'N/A'}</small>
                </div>
                <div class="text-end">
                    <span class="badge bg-success">$${(item.price || 0).toFixed(2)}</span>
                </div>
            </div>
        `;
        list.insertAdjacentHTML('beforeend', itemHTML);
    });
}

async function getProductSuggestions(token, topSoldItems) {
    try {
        // Get full catalog for context
        const catalogResponse = await fetch(`${API_BASE}/order-service/products`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        let catalog = [];
        if (catalogResponse.ok) {
            catalog = await catalogResponse.json();
        }

        // Request AI suggestions
        const suggestionsResponse = await fetch(`${API_BASE}/api/seller/recommendations/suggest-products`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                topSoldItems: topSoldItems,
                currentCatalog: catalog
            })
        });

        if (!suggestionsResponse.ok) {
            console.warn('Failed to fetch suggestions:', suggestionsResponse.status);
            renderFallbackSuggestions();
            return;
        }

        const suggestionsData = await suggestionsResponse.json();
        renderProductSuggestions(suggestionsData.suggestions || []);

    } catch (error) {
        console.error('Error getting product suggestions:', error);
        renderFallbackSuggestions();
    }
}

function renderProductSuggestions(suggestions) {
    const list = document.getElementById('suggestedProductsList');
    list.innerHTML = '';

    if (!suggestions || suggestions.length === 0) {
        list.innerHTML = '<p class="text-muted">No suggestions available at this time.</p>';
        return;
    }

    suggestions.forEach((productName, index) => {
        const suggestionHTML = `
            <div class="list-group-item d-flex justify-content-between align-items-center">
                <div class="flex-grow-1">
                    <h6 class="mb-1">${productName}</h6>
                    <small class="text-muted">Suggested based on sales trends</small>
                </div>
                <div class="btn-group" role="group">
                    <button type="button" class="btn btn-sm btn-outline-primary" onclick="addProductModal('${productName.replace(/'/g, "\\'")}')">
                        <i class="fa fa-plus"></i> Add
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-secondary" onclick="dismissSuggestion(this)">
                        <i class="fa fa-times"></i> Dismiss
                    </button>
                </div>
            </div>
        `;
        list.insertAdjacentHTML('beforeend', suggestionHTML);
    });
}

function renderFallbackSuggestions() {
    const fallbacks = [
        'Organic Spice Mix',
        'Premium Olive Oil',
        'Specialty Marinades'
    ];
    renderProductSuggestions(fallbacks);
}

function dismissSuggestion(button) {
    button.closest('.list-group-item').remove();
}

function addProductModal(productName) {
    // Show a modal to add this product
    const input = prompt(`Enter details for new product: "${productName}"\n\nFormat: category,price\nExample: FRESH_PRODUCE,5.99`);

    if (!input) return;

    const [category, price] = input.split(',');

    if (!category || !price) {
        alert('Invalid format. Please use: category,price');
        return;
    }

    addProductToInventory(productName, category.trim(), parseFloat(price));
}

async function addProductToInventory(name, category, price) {
    const token = localStorage.getItem('token');

    try {
        const response = await fetch(`${API_BASE}/order-service/products/add`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                name: name,
                category: category,
                price: price,
                stock: 0 // Seller will set stock separately
            })
        });

        if (response.ok) {
            showNotification('success', `Product "${name}" added successfully!`);
            setTimeout(() => location.reload(), 1500);
        } else {
            showNotification('error', 'Failed to add product. Please try again.');
        }
    } catch (error) {
        console.error('Error adding product:', error);
        showNotification('error', 'Error: ' + error.message);
    }
}
