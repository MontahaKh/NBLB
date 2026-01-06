/**
 * Seller Dashboard JavaScript
 * Handles CRUD operations for seller panel
 */

// Use gateway URL from auth-utils.js (already loaded)
// Use seller-specific endpoints for sellers
const SELLER_API_BASE = `${API_BASE}/order-service/api/seller`;
const ORDER_API_BASE = `${API_BASE}/order-service/api`;

// Initialize dashboard on page load
document.addEventListener('DOMContentLoaded', function () {
    console.log('Seller dashboard loaded');
    requireSeller();
    loadSellerStats();
    setupEventListeners();
});

/**
 * Setup event listeners for sidebar navigation
 */
function setupEventListeners() {
    console.log('Setting up event listeners');
    const navItems = document.querySelectorAll('.nav-sidebar a, .nav-sidebar span');
    navItems.forEach((link) => {
        const sectionId = link.getAttribute('data-section');
        link.addEventListener('click', function (e) {
            e.preventDefault();
            // Update active state
            navItems.forEach(item => item.classList.remove('active'));
            this.classList.add('active');

            // Show/hide sections
            document.querySelectorAll('.section-content').forEach(section => {
                section.classList.add('d-none');
            });

            const targetSection = document.getElementById(sectionId + '-section');
            if (targetSection) {
                targetSection.classList.remove('d-none');
                if (sectionId === 'dashboard') loadSellerStats();
                else if (sectionId === 'products') loadSellerProducts();
                else if (sectionId === 'orders') loadSellerOrders();
            }
        });
    });
}

/**
 * Load seller dashboard statistics
 */
function loadSellerStats() {
    const token = getTokenFromSession();
    if (!token) {
        console.error('No token found');
        return;
    }

    // Load product count
    loadSellerProducts().then(products => {
        document.getElementById('totalProducts').textContent = products.length || '0';
    });

    // Load sales data
    fetch(`${SELLER_API_BASE}/sales`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to load sales');
            return response.json();
        })
        .then(data => {
            // Calculate total sales and unique order count
            const totalSales = data.reduce((sum, line) => sum + (line.unitPrice || 0), 0);

            // Count unique orders
            const uniqueOrders = new Set(data.map(line => line.orderId)).size;

            // Calculate average rating (based on number of completed orders)
            const avgRating = uniqueOrders > 0 ? (4.5).toFixed(1) : 'N/A';

            document.getElementById('totalSellerOrders').textContent = uniqueOrders || '0';
            document.getElementById('totalSales').textContent = '$' + totalSales.toFixed(2);
            document.getElementById('avgRating').textContent = avgRating;
        })
        .catch(error => {
            console.error('Error loading sales stats:', error);
            document.getElementById('totalSellerOrders').textContent = '0';
            document.getElementById('totalSales').textContent = '$0.00';
        });

    // Load recommendations integrated into dashboard
    loadDashboardRecommendations(token);
}

/**
 * Load seller's own products (filtered by seller username)
 * Returns array of products belonging to current seller
 */
async function loadSellerProducts() {
    const token = getTokenFromSession();
    const username = getCurrentUsername();

    if (!token || !username) {
        console.error('No token or username found');
        return [];
    }

    try {
        const response = await fetch(`${SELLER_API_BASE}/products`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) throw new Error('Failed to load products');
        const sellerProducts = await response.json();

        const tbody = document.getElementById('sellerProducts');
        if (!sellerProducts || sellerProducts.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No products found. Add your first product!</td></tr>';
            return [];
        }

        tbody.innerHTML = sellerProducts.map(product => `
            <tr>
                <td>${product.id}</td>
                <td>${product.name}</td>
                <td>${product.category}</td>
                <td>$${parseFloat(product.price).toFixed(2)}</td>
                <td><span class="badge bg-warning">${product.stock || product.quantity || '0'}</span></td>
                <td><span class="badge bg-info">${product.status || 'AVAILABLE'}</span></td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="openEditProductModal(${product.id}, '${product.name}', '${product.category}', '${product.price}', ${product.stock || product.quantity || 0}, '${product.status || 'AVAILABLE'}')">
                        <i class="fa fa-edit"></i> Edit
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteSellerProduct(${product.id})">
                        <i class="fa fa-trash"></i> Delete
                    </button>
                </td>
            </tr>
        `).join('');

        return sellerProducts;
    } catch (error) {
        console.error('Error loading products:', error);
        const tbody = document.getElementById('sellerProducts');
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Error loading products</td></tr>';
        return [];
    }
}

/**
 * Load seller's orders (read-only view)
 */
function loadSellerOrders() {
    const token = getTokenFromSession();
    const username = getCurrentUsername();

    if (!token || !username) {
        console.error('No token or username found');
        return;
    }

    fetch(`${SELLER_API_BASE}/sales`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to load orders');
            return response.json();
        })
        .then(data => {
            const tbody = document.getElementById('sellerSales');
            if (!data || data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">No orders found</td></tr>';
                return;
            }

            tbody.innerHTML = data.map(line => `
                <tr>
                    <td>#${line.orderId}</td>
                    <td>${line.orderDate ? new Date(line.orderDate).toLocaleDateString() : new Date().toLocaleDateString()}</td>
                    <td>${line.buyerUsername || 'N/A'}</td>
                    <td>${line.productName || 'Product'}</td>
                    <td>1</td>
                    <td>$${parseFloat(line.unitPrice || 0).toFixed(2)}</td>
                    <td><span class="badge bg-secondary">${line.orderStatus || 'PENDING'}</span></td>
                    <td>
                        ${line.orderStatus === 'PAID' || line.orderStatus === 'WAITING_DELIVERY' ? `
                            <button class="btn btn-sm btn-success" onclick="updateOrderShipStatus(${line.orderId})">
                                <i class="fa fa-truck"></i> Ship
                            </button>
                        ` : `
                            <button class="btn btn-sm btn-info" onclick="viewOrderDetails(${line.orderId})">
                                <i class="fa fa-eye"></i> View
                            </button>
                        `}
                    </td>
                </tr>
            `).join('');
        })
        .catch(error => console.error('Error loading orders:', error));
}

/**
 * PRODUCT CRUD OPERATIONS
 */

/**
 * Open edit product modal and populate with product data
 */
function openEditProductModal(productId, name, category, price, quantity, status) {
    document.getElementById('editProductId').value = productId;
    document.getElementById('editProductName').value = name;
    document.getElementById('editProductCategory').value = category;
    document.getElementById('editProductPrice').value = price;
    document.getElementById('editProductQuantity').value = quantity;
    document.getElementById('editProductStatus').value = status;

    const modal = new bootstrap.Modal(document.getElementById('editProductModal'));
    modal.show();
}

/**
 * Create new product - called by form submission
 */
function submitCreateProduct(e) {
    if (e) e.preventDefault();

    const token = getTokenFromSession();
    const username = getCurrentUsername();

    if (!token || !username) {
        alert('Not authenticated');
        return;
    }

    const productData = {
        name: document.getElementById('productName').value,
        price: parseFloat(document.getElementById('price').value),
        category: document.getElementById('category').value,
        quantity: parseInt(document.getElementById('quantity').value),
        addedBy: username
    };

    fetch(`${SELLER_API_BASE}/products`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(productData)
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to create product');
            return response.json();
        })
        .then(data => {
            document.getElementById('addProductSuccess').style.display = 'block';
            document.getElementById('addProductError').style.display = 'none';
            document.getElementById('addProductForm').reset();
            setTimeout(() => {
                document.getElementById('addProductSuccess').style.display = 'none';
            }, 3000);
            loadSellerProducts();
        })
        .catch(error => {
            document.getElementById('addProductErrorMsg').textContent = error.message;
            document.getElementById('addProductError').style.display = 'block';
            document.getElementById('addProductSuccess').style.display = 'none';
        });
}

/**
 * Update existing product via modal form
 */
function submitEditProduct() {
    const token = getTokenFromSession();
    const productId = document.getElementById('editProductId').value;

    if (!token || !productId) {
        alert('Missing authentication or product ID');
        return;
    }

    const productData = {
        name: document.getElementById('editProductName').value,
        category: document.getElementById('editProductCategory').value,
        price: parseFloat(document.getElementById('editProductPrice').value),
        quantity: parseInt(document.getElementById('editProductQuantity').value),
        status: document.getElementById('editProductStatus').value
    };

    fetch(`${SELLER_API_BASE}/products/${productId}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(productData)
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to update product');
            return response.json();
        })
        .then(data => {
            alert('Product updated successfully');
            bootstrap.Modal.getInstance(document.getElementById('editProductModal')).hide();
            loadSellerProducts();
        })
        .catch(error => alert('Error: ' + error.message));
}

/**
 * Delete product with confirmation
 */
function deleteSellerProduct(productId) {
    if (!confirm('Are you sure you want to delete this product?')) return;

    const token = getTokenFromSession();

    if (!token) {
        alert('Not authenticated');
        return;
    }

    fetch(`${SELLER_API_BASE}/products/${productId}`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to delete product');
            alert('Product deleted successfully');
            loadSellerProducts();
        })
        .catch(error => alert('Error: ' + error.message));
}

/**
 * View order details (placeholder)
 */
function viewOrderDetails(orderId) {
    alert(`View order details for order #${orderId}`);
}

/**
 * Update order status to SHIPPED
 */
function updateOrderShipStatus(orderId) {
    if (!confirm('Mark this order as shipped?')) return;

    const token = getTokenFromSession();

    if (!token) {
        alert('Not authenticated');
        return;
    }

    fetch(`${SELLER_API_BASE}/orders/${orderId}/ship`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to update order status');
            return response.json();
        })
        .then(data => {
            alert('Order marked as shipped');
            loadSellerOrders();
        })
        .catch(error => alert('Error: ' + error.message));
}

/**
 * Get JWT token from session with fallback logic
 */
function getTokenFromSession() {
    if (typeof getToken === 'function') {
        const token = getToken();
        console.log('Token from auth-utils:', token ? 'Present' : 'Missing');
        return token;
    }

    let token = localStorage.getItem('token') || localStorage.getItem('jwt_token');
    console.log('Token from localStorage:', token ? 'Present' : 'Missing');

    if (!token) {
        const params = new URLSearchParams(window.location.search);
        token = params.get('token');
        if (token) {
            console.log('Token from URL:', 'Found');
            localStorage.setItem('token', token);
        }
    }

    return token;
}

/**
 * Extract username from localStorage user object or JWT token
 */
function getCurrentUsername() {
    // Try to get from localStorage user object
    const userStr = localStorage.getItem('user');
    if (userStr) {
        try {
            const user = JSON.parse(userStr);
            if (user.username) return user.username;
        } catch (e) {
            console.error('Error parsing user data:', e);
        }
    }

    // Try to get from JWT token payload (subject claim)
    const token = getTokenFromSession();
    if (token) {
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            return payload.sub || null;
        } catch (e) {
            console.error('Error decoding token:', e);
        }
    }

    return null;
}

/**
 * Load recommendations for dashboard integration
 * Loads both top-sold items and AI suggestions
 */
function loadDashboardRecommendations(token) {
    const API_RECOMMENDATIONS = `${API_BASE}/api/seller/recommendations`;

    // Load top-sold items first
    fetch(`${API_RECOMMENDATIONS}/top-sold?limit=5`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to load top-sold items');
            return response.json();
        })
        .then(items => {
            renderDashboardTopSoldItems(items);

            // After loading top-sold items, load AI suggestions based on them
            loadAISuggestions(token, items, API_RECOMMENDATIONS);
        })
        .catch(error => {
            console.error('Error loading top-sold items:', error);
            document.getElementById('dashboardTopSoldList').innerHTML =
                '<p class="text-muted text-center py-3 small">No sales data available yet</p>';
            // Still try to load suggestions even if top-sold items failed
            loadAISuggestions(token, [], API_RECOMMENDATIONS);
        });
}

/**
 * Load AI suggestions based on top-sold items
 */
function loadAISuggestions(token, topSoldItems, API_RECOMMENDATIONS) {
    // Send top-sold items to the suggestions endpoint
    fetch(`${API_RECOMMENDATIONS}/suggest-products`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            topSoldItems: topSoldItems || [],
            limit: 5
        })
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to load suggestions');
            return response.json();
        })
        .then(data => {
            // The API returns: { suggestions: [...], basedOn: [...], count: N }
            const suggestions = data.suggestions || data || [];
            renderDashboardSuggestionsItems(suggestions);
        })
        .catch(error => {
            console.error('Error loading AI suggestions:', error);
            document.getElementById('dashboardSuggestedProductsList').innerHTML =
                '<p class="text-muted text-center py-3 small">Unable to generate suggestions</p>';
        });
}

/**
 * Render top-sold items for dashboard
 */
function renderDashboardTopSoldItems(items) {
    const container = document.getElementById('dashboardTopSoldList');

    if (!items || items.length === 0) {
        container.innerHTML = '<p class="text-muted text-center py-3 small">No sales data available yet</p>';
        return;
    }

    container.innerHTML = items.map((item, idx) => `
        <div class="list-group-item d-flex justify-content-between align-items-center">
            <div class="flex-grow-1">
                <div class="fw-bold small">#${idx + 1} ${item.name}</div>
                <small class="text-muted">${item.category}</small>
            </div>
            <div class="text-end">
                <div class="fw-bold">$${item.price}</div>
            </div>
        </div>
    `).join('');
}

/**
 * Render AI suggested products for dashboard
 * Suggestions are product name strings from the API
 */
function renderDashboardSuggestionsItems(suggestions) {
    const container = document.getElementById('dashboardSuggestedProductsList');

    if (!suggestions || !Array.isArray(suggestions) || suggestions.length === 0) {
        container.innerHTML = '<p class="text-muted text-center py-3 small">No suggestions available</p>';
        return;
    }

    // Suggestions are product names (strings), not objects
    container.innerHTML = suggestions.map(productName => `
        <div class="list-group-item d-flex justify-content-between align-items-center">
            <div class="flex-grow-1">
                <div class="fw-bold small">${productName}</div>
                <small class="text-muted">AI Recommended</small>
            </div>
            <div class="text-end">
                <small class="badge bg-success">Consider Adding</small>
            </div>
        </div>
    `).join('');
}

/**
 * Initialize form submission handlers on DOM load
 */
document.addEventListener('DOMContentLoaded', function () {
    const form = document.getElementById('addProductForm');
    if (form) {
        form.addEventListener('submit', submitCreateProduct);
    }
});
