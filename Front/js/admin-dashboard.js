/**
 * Admin Dashboard JavaScript
 * Handles CRUD operations for admin panel
 */

// Use gateway URL from auth-utils.js (already loaded)
const ADMIN_API_BASE = `${API_BASE}/order-service/api/admin`;
const AUTH_API_BASE = `${API_BASE}/auth/api`;

// Initialize dashboard on page load
document.addEventListener('DOMContentLoaded', function () {
    console.log('Admin dashboard loaded');
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
                if (sectionId === 'users') loadUsers();
                else if (sectionId === 'sellers') loadSellers();
                else if (sectionId === 'products') loadProducts();
                else if (sectionId === 'orders') loadOrders();
            }
        });
    });
}

/**
 * Load all users
 */
function loadUsers() {
    const token = getTokenFromSession();
    fetch(`${AUTH_API_BASE}/users`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to load users');
            return response.json();
        })
        .then(data => {
            const tbody = document.getElementById('usersTable');
            if (!data || data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No users found</td></tr>';
                return;
            }

            tbody.innerHTML = data.map(user => `
                <tr>
                    <td>${user.id}</td>
                    <td>${user.username}</td>
                    <td>${user.email}</td>
                    <td><span class="badge bg-info">${user.role}</span></td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="openEditUserModal(${user.id}, '${user.username}', '${user.email}', '${user.role}')">
                            <i class="fa fa-edit"></i> Edit
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteUser(${user.id}, '${user.username}')">
                            <i class="fa fa-trash"></i> Delete
                        </button>
                    </td>
                </tr>
            `).join('');
        })
        .catch(error => console.error('Error loading users:', error));
}

/**
 * Load all sellers
 */
function loadSellers() {
    const token = getTokenFromSession();
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
        .then(data => {
            const tbody = document.getElementById('sellersTable');
            if (!data || data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No sellers found</td></tr>';
                return;
            }

            tbody.innerHTML = data.map(seller => `
                <tr>
                    <td>${seller.username}</td>
                    <td>${seller.email}</td>
                    <td><span class="badge bg-success">${seller.productCount || 0} products</span></td>
                    <td>
                        <button class="btn btn-sm btn-info" onclick="viewSeller('${seller.username}')">
                            <i class="fa fa-eye"></i> View
                        </button>
                    </td>
                </tr>
            `).join('');
        })
        .catch(error => console.error('Error loading sellers:', error));
}

/**
 * Load all products
 */
function loadProducts() {
    const token = getTokenFromSession();
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
        .then(data => {
            const tbody = document.getElementById('productsTable');
            if (!data || data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No products found</td></tr>';
                return;
            }

            tbody.innerHTML = data.map(product => `
                <tr>
                    <td>${product.id}</td>
                    <td>${product.name}</td>
                    <td>${product.seller}</td>
                    <td>${product.category}</td>
                    <td>$${parseFloat(product.price).toFixed(2)}</td>
                    <td><span class="badge bg-warning">${product.stock}</span></td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="openEditProductModal(${product.id}, '${product.name}', '${product.price}', '${product.category}', ${product.stock}, '${product.status}')">
                            <i class="fa fa-edit"></i> Edit
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteProduct(${product.id})">
                            <i class="fa fa-trash"></i> Delete
                        </button>
                    </td>
                </tr>
            `).join('');
        })
        .catch(error => console.error('Error loading products:', error));
}

/**
 * Load all orders
 */
function loadOrders() {
    const token = getTokenFromSession();
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
        .then(data => {
            const tbody = document.getElementById('ordersTable');
            if (!data || data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No orders found</td></tr>';
                return;
            }

            tbody.innerHTML = data.map(order => `
                <tr>
                    <td>${order.id}</td>
                    <td>${order.customer}</td>
                    <td>${order.date}</td>
                    <td>${order.total}</td>
                    <td><span class="badge bg-secondary">${order.status}</span></td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="openUpdateOrderStatusModal(${order.id}, '${order.status}')">
                            <i class="fa fa-edit"></i> Update Status
                        </button>
                    </td>
                </tr>
            `).join('');
        })
        .catch(error => console.error('Error loading orders:', error));
}

/**
 * USER CRUD FUNCTIONS
 */

function openEditUserModal(userId, username, email, role) {
    document.getElementById('editUserId').value = userId;
    document.getElementById('editUsername').value = username;
    document.getElementById('editUserEmail').value = email;
    document.getElementById('editUserRole').value = role;
    const modal = new bootstrap.Modal(document.getElementById('editUserModal'));
    modal.show();
}

function submitCreateUser() {
    const token = getTokenFromSession();
    const userData = {
        username: document.getElementById('newUsername').value,
        email: document.getElementById('newEmail').value,
        password: document.getElementById('newPassword').value,
        role: document.getElementById('newRole').value
    };

    fetch(`${AUTH_API_BASE}/users`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(userData)
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to create user');
            return response.json();
        })
        .then(data => {
            alert('User created successfully');
            bootstrap.Modal.getInstance(document.getElementById('createUserModal')).hide();
            document.getElementById('createUserForm').reset();
            loadUsers();
        })
        .catch(error => alert('Error: ' + error.message));
}

function submitEditUser() {
    const token = getTokenFromSession();
    const userId = document.getElementById('editUserId').value;
    const userData = {
        email: document.getElementById('editUserEmail').value,
        password: document.getElementById('editUserPassword').value || undefined,
        role: document.getElementById('editUserRole').value
    };

    // Remove empty password
    if (!userData.password) delete userData.password;

    fetch(`${AUTH_API_BASE}/users/${userId}`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(userData)
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to update user');
            return response.json();
        })
        .then(data => {
            alert('User updated successfully');
            bootstrap.Modal.getInstance(document.getElementById('editUserModal')).hide();
            loadUsers();
        })
        .catch(error => alert('Error: ' + error.message));
}

function deleteUser(userId, username) {
    if (username.toLowerCase() === 'admin') {
        alert('Cannot delete admin user');
        return;
    }

    if (!confirm(`Delete user "${username}"?`)) return;

    const token = getTokenFromSession();
    fetch(`${AUTH_API_BASE}/users/${userId}`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to delete user');
            alert('User deleted successfully');
            loadUsers();
        })
        .catch(error => alert('Error: ' + error.message));
}

/**
 * PRODUCT CRUD FUNCTIONS
 */

function openEditProductModal(productId, name, price, category, quantity, status) {
    document.getElementById('editProductId').value = productId;
    document.getElementById('editProductName').value = name;
    document.getElementById('editProductPrice').value = price;
    document.getElementById('editProductCategory').value = category;
    document.getElementById('editProductQuantity').value = quantity;
    document.getElementById('editProductStatus').value = status;
    const modal = new bootstrap.Modal(document.getElementById('editProductModal'));
    modal.show();
}

function submitCreateProduct() {
    const token = getTokenFromSession();
    const productData = {
        name: document.getElementById('newProductName').value,
        price: document.getElementById('newProductPrice').value,
        category: document.getElementById('newProductCategory').value,
        quantity: document.getElementById('newProductQuantity').value,
        addedBy: document.getElementById('newProductSeller').value
    };

    fetch(`${ADMIN_API_BASE}/products`, {
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
            alert('Product created successfully');
            bootstrap.Modal.getInstance(document.getElementById('createProductModal')).hide();
            document.getElementById('createProductForm').reset();
            loadProducts();
        })
        .catch(error => alert('Error: ' + error.message));
}

function submitEditProduct() {
    const token = getTokenFromSession();
    const productId = document.getElementById('editProductId').value;
    const productData = {
        name: document.getElementById('editProductName').value,
        price: document.getElementById('editProductPrice').value,
        category: document.getElementById('editProductCategory').value,
        quantity: document.getElementById('editProductQuantity').value,
        status: document.getElementById('editProductStatus').value
    };

    fetch(`${ADMIN_API_BASE}/products/${productId}`, {
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
            loadProducts();
        })
        .catch(error => alert('Error: ' + error.message));
}

function deleteProduct(productId) {
    if (!confirm('Delete this product?')) return;

    const token = getTokenFromSession();
    fetch(`${ADMIN_API_BASE}/products/${productId}`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to delete product');
            alert('Product deleted successfully');
            loadProducts();
        })
        .catch(error => alert('Error: ' + error.message));
}

/**
 * ORDER FUNCTIONS
 */

function openUpdateOrderStatusModal(orderId, currentStatus) {
    document.getElementById('updateOrderId').value = orderId;
    document.getElementById('updateOrderStatus').value = currentStatus;
    const modal = new bootstrap.Modal(document.getElementById('updateOrderStatusModal'));
    modal.show();
}

function submitUpdateOrderStatus() {
    const token = getTokenFromSession();
    const orderId = document.getElementById('updateOrderId').value;
    const statusData = {
        status: document.getElementById('updateOrderStatus').value
    };

    fetch(`${ADMIN_API_BASE}/orders/${orderId}/status`, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(statusData)
    })
        .then(response => {
            if (!response.ok) throw new Error('Failed to update order status');
            return response.json();
        })
        .then(data => {
            alert('Order status updated successfully');
            bootstrap.Modal.getInstance(document.getElementById('updateOrderStatusModal')).hide();
            loadOrders();
        })
        .catch(error => alert('Error: ' + error.message));
}

/**
 * HELPER FUNCTIONS
 */

function viewSeller(username) {
    alert(`View seller: ${username}`);
}

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
