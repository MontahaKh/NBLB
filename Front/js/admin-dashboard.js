/**
 * Admin Dashboard JavaScript
 * Handles data loading and display for admin panel
 */

// Use gateway URL from auth-utils.js (already loaded)
const ADMIN_API_BASE = `${API_BASE}/order-service/api/admin`;
const AUTH_API_BASE = `${API_BASE}/auth/api`;

// Initialize dashboard on page load
document.addEventListener('DOMContentLoaded', function () {
    console.log('Admin dashboard loaded');

    // Require admin access
    requireAdmin();

    loadDashboardStats();
    setupEventListeners();
});

/**
 * Load dashboard statistics
 */
function loadDashboardStats() {
    const token = getTokenFromSession();

    if (!token) {
        console.error('No token found');
        return;
    }

    fetch(`${ADMIN_API_BASE}/dashboard/stats`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to load stats');
            return response.json();
        })
        .then(data => {
            document.getElementById('totalUsers').textContent = data.totalUsers || '0';
            document.getElementById('totalSellers').textContent = data.totalSellers || '0';
            document.getElementById('totalOrders').textContent = data.totalOrders || '0';
            document.getElementById('totalRevenue').textContent = data.totalRevenue || '$0.00';
        })
        .catch(error => {
            console.error('Error loading stats:', error);
            document.getElementById('totalUsers').textContent = 'Error';
            document.getElementById('totalSellers').textContent = 'Error';
            document.getElementById('totalOrders').textContent = 'Error';
            document.getElementById('totalRevenue').textContent = 'Error';
        });
}

/**
 * Setup event listeners for sidebar navigation
 */
function setupEventListeners() {
    console.log('Setting up event listeners');

    const navItems = document.querySelectorAll('.nav-sidebar a, .nav-sidebar span');
    console.log('Found nav items:', navItems.length);

    navItems.forEach((link, index) => {
        const sectionId = link.getAttribute('data-section');
        console.log(`Nav item ${index}: section=${sectionId}`);

        link.addEventListener('click', function (e) {
            e.preventDefault();
            console.log('Clicked section:', sectionId);

            // Update active state
            navItems.forEach(item => {
                item.classList.remove('active');
            });
            this.classList.add('active');

            // Show/hide sections
            document.querySelectorAll('.section-content').forEach(section => {
                section.classList.add('d-none');
            });

            const targetSection = document.getElementById(sectionId + '-section');
            console.log('Target section:', sectionId + '-section', targetSection);

            if (targetSection) {
                targetSection.classList.remove('d-none');

                // Load data based on section
                if (sectionId === 'users') {
                    loadUsers();
                } else if (sectionId === 'sellers') {
                    loadSellers();
                } else if (sectionId === 'products') {
                    loadProducts();
                } else if (sectionId === 'orders') {
                    loadOrders();
                }
            }
        });
    });
}

/**
 * Load and display users
 */
function loadUsers() {
    const token = getTokenFromSession();
    const tbody = document.getElementById('usersTable');

    tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Loading users...</td></tr>';

    if (!token) {
        console.error('No token available');
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Error: No authentication token. Please login first.</td></tr>';
        return;
    }

    const url = `${AUTH_API_BASE}/users`;
    console.log('Loading users from:', url);
    console.log('Token:', token.substring(0, 20) + '...');

    fetch(url, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            console.log('Users response status:', response.status);
            console.log('Response headers:', {
                'content-type': response.headers.get('content-type'),
                'authorization': response.headers.get('authorization')
            });
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(`HTTP ${response.status}: ${text}`);
                });
            }
            return response.json();
        })
        .then(users => {
            console.log('Users loaded:', users);
            if (users.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No users found</td></tr>';
                return;
            }

            tbody.innerHTML = users.map(user => `
            <tr>
                <td>${user.id}</td>
                <td>${user.username}</td>
                <td>${user.email}</td>
                <td><span class="badge bg-primary">${user.role}</span></td>
                <td><span class="badge bg-success">Active</span></td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="editUser('${user.username}')">
                        <i class="fa fa-edit"></i>
                    </button>
                </td>
            </tr>
        `).join('');
        })
        .catch(error => {
            console.error('Error loading users:', error);
            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger">Error: ${error.message}</td></tr>`;
        });
}

/**
 * Load and display sellers
 */
function loadSellers() {
    const token = getTokenFromSession();
    const tbody = document.getElementById('sellersTable');

    tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">Loading sellers...</td></tr>';

    fetch(`${ADMIN_API_BASE}/sellers`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to load sellers');
            return response.json();
        })
        .then(sellers => {
            const tbody = document.getElementById('sellersTable');
            if (sellers.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No sellers found</td></tr>';
                return;
            }

            tbody.innerHTML = sellers.map(seller => `
            <tr>
                <td>${seller.username}</td>
                <td>${seller.email}</td>
                <td><span class="badge bg-info">${seller.productCount}</span></td>
                <td><span class="badge bg-success">Active</span></td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="editSeller('${seller.username}')">
                        <i class="fa fa-edit"></i>
                    </button>
                </td>
            </tr>
        `).join('');
        })
        .catch(error => {
            console.error('Error loading sellers:', error);
            document.getElementById('sellersTable').innerHTML = `
            <tr><td colspan="5" class="text-center text-danger">Error loading sellers</td></tr>
        `;
        });
}

/**
 * Load and display products
 */
function loadProducts() {
    const token = getTokenFromSession();
    const tbody = document.getElementById('productsTable');

    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">Loading products...</td></tr>';

    fetch(`${ADMIN_API_BASE}/products`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to load products');
            return response.json();
        })
        .then(products => {
            if (products.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No products found</td></tr>';
                return;
            }

            tbody.innerHTML = products.map(product => `
            <tr>
                <td>${product.id}</td>
                <td>${product.name}</td>
                <td>${product.seller}</td>
                <td>${product.category}</td>
                <td>$${parseFloat(product.price).toFixed(2)}</td>
                <td>
                    <span class="badge ${product.stock > 0 ? 'bg-success' : 'bg-danger'}">
                        ${product.stock}
                    </span>
                </td>
                <td>
                    <button class="btn btn-sm btn-info" onclick="viewProduct(${product.id})">
                        <i class="fa fa-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteProduct(${product.id})">
                        <i class="fa fa-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');
        })
        .catch(error => {
            console.error('Error loading products:', error);
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Error loading products</td></tr>';
        });
}

/**
 * Load and display orders
 */
function loadOrders() {
    const token = getTokenFromSession();
    const tbody = document.getElementById('ordersTable');

    tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Loading orders...</td></tr>';

    fetch(`${ADMIN_API_BASE}/orders`, {
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
        .then(orders => {
            if (orders.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No orders found</td></tr>';
                return;
            }

            tbody.innerHTML = orders.map(order => `
            <tr>
                <td>${order.id}</td>
                <td>${order.customer}</td>
                <td>${new Date(order.date).toLocaleDateString()}</td>
                <td>${order.total}</td>
                <td>
                    <span class="badge ${getStatusColor(order.status)}">
                        ${order.status}
                    </span>
                </td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="viewOrder(${order.id})">
                        <i class="fa fa-eye"></i>
                    </button>
                </td>
            </tr>
        `).join('');
        })
        .catch(error => {
            console.error('Error loading orders:', error);
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Error loading orders</td></tr>';
        });
}

/**
 * Get status badge color
 */
function getStatusColor(status) {
    const colors = {
        'PENDING': 'bg-warning',
        'CONFIRMED': 'bg-info',
        'SHIPPED': 'bg-primary',
        'DELIVERED': 'bg-success',
        'CANCELLED': 'bg-danger'
    };
    return colors[status] || 'bg-secondary';
}

/**
 * Get JWT token from session/localStorage
 */
function getTokenFromSession() {
    // Use getToken() from auth-utils.js if available
    if (typeof getToken === 'function') {
        const token = getToken();
        console.log('Token from auth-utils:', token ? 'Present' : 'Missing');
        return token;
    }

    // Fallback: Try to get from localStorage
    let token = localStorage.getItem('token') || localStorage.getItem('jwt_token');
    console.log('Token from localStorage:', token ? 'Present' : 'Missing');

    // If not found, try to extract from URL
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
 * Action handlers
 */
function editUser(username) {
    alert(`Edit user: ${username}`);
}

function editSeller(username) {
    alert(`Edit seller: ${username}`);
}

function viewProduct(productId) {
    alert(`View product: ${productId}`);
}

function deleteProduct(productId) {
    if (confirm('Are you sure you want to delete this product?')) {
        alert(`Delete product: ${productId}`);
    }
}

function viewOrder(orderId) {
    alert(`View order: ${orderId}`);
}
