// js/navbar.js
// Handles dynamic navbar insertion and role-based navigation updates

/**
 * Inserts navbar template into page and sets up role-based navigation
 */
function initializeNavbar() {
    // Find placeholder for navbar
    const placeholder = document.getElementById('navbar-placeholder');
    if (!placeholder) {
        console.warn('No navbar placeholder found');
        return;
    }

    // Only add navbar if not already present
    if (placeholder.querySelector('.navbar-wrapper')) {
        updateNavbarForRole();
        return;
    }

    fetch('navbar-template.html')
        .then(response => {
            if (!response.ok) throw new Error('Failed to load navbar');
            return response.text();
        })
        .then(html => {
            placeholder.innerHTML = html;
            updateNavbarForRole();
            setActiveNavLink();
            updateCartBadge();
        })
        .catch(error => {
            console.error('Error loading navbar:', error);
            // Fallback: create minimal navbar
            placeholder.innerHTML = '<nav class="navbar navbar-expand-lg navbar-light bg-light"><div class="container"><a href="index.html">NBLB</a></div></nav>';
        });
}

/**
 * Updates navbar visibility based on user role
 */
function updateNavbarForRole() {
    const role = getRole();
    const isLoggedIn = !!getToken();

    console.log('Updating navbar for role:', role, 'LoggedIn:', isLoggedIn);

    // Hide/show role-specific navigation items
    const adminLink = document.getElementById('navAdminLink');
    const sellerLink = document.getElementById('navSellerLink');
    const ordersLink = document.getElementById('navOrdersLink');
    const loginLink = document.getElementById('navLoginLink');

    // Admin link - show only if logged in AND role is ADMIN
    if (adminLink) {
        const shouldShow = isLoggedIn && role === 'ADMIN';
        adminLink.classList.toggle('d-none', !shouldShow);
        console.log('Admin link should show:', shouldShow);
    }

    // Seller link - show only if logged in AND role is SHOP or SELLER
    if (sellerLink) {
        const shouldShow = isLoggedIn && (role === 'SHOP' || role === 'SELLER');
        sellerLink.classList.toggle('d-none', !shouldShow);
        console.log('Seller link should show:', shouldShow);
    }

    // Orders link - show only if logged in AND role is CLIENT
    if (ordersLink) {
        const shouldShow = isLoggedIn && role === 'CLIENT';
        ordersLink.classList.toggle('d-none', !shouldShow);
        console.log('Orders link should show:', shouldShow);
    }

    // Login link - show only if NOT logged in
    if (loginLink) {
        loginLink.classList.toggle('d-none', isLoggedIn);
        console.log('Login link should show:', !isLoggedIn);
    }

    // Update user menu
    updateUserMenu();
}

/**
 * Checks current page and sets active nav link
 */
function setActiveNavLink() {
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    document.querySelectorAll('.navbar-nav .nav-link').forEach(link => {
        const href = link.getAttribute('href');
        if (href === currentPage || (currentPage === '' && href === 'index.html')) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });
}

/**
 * Enhanced user menu with role-based items
 */
function updateUserMenu() {
    const userMenu = document.getElementById('userMenu');
    if (!userMenu) return;

    const username = getUsername();
    const role = getRole();

    if (getToken() && username) {
        const menuItems = [
            `<li class="dropdown-header">${username} <span class="badge bg-primary">${role}</span></li>`,
        ];

        // Add role-specific menu items
        if (role === 'ADMIN') {
            menuItems.push('<li><a class="dropdown-item" href="admin-dashboard.html"><i class="fa fa-cog me-2"></i>Admin Panel</a></li>');
        } else if (role === 'SHOP' || role === 'SELLER') {
            menuItems.push('<li><a class="dropdown-item" href="seller-dashboard.html"><i class="fa fa-store me-2"></i>Seller Dashboard</a></li>');
        }

        if (role === 'CLIENT') {
            menuItems.push('<li><a class="dropdown-item" href="orders.html"><i class="fa fa-list me-2"></i>My Orders</a></li>');
        }

        menuItems.push(
            '<li><a class="dropdown-item" href="orders.html"><i class="fa fa-history me-2"></i>Order History</a></li>',
            '<li><hr class="dropdown-divider"></li>',
            '<li><a class="dropdown-item" href="#" id="logoutLink"><i class="fa fa-sign-out-alt me-2"></i>Logout</a></li>'
        );

        userMenu.innerHTML = `
            <div class="dropdown">
                <button class="btn btn-outline-secondary btn-sm rounded-circle dropdown-toggle" 
                        type="button" data-bs-toggle="dropdown" title="User Menu">
                    <i class="fa fa-user"></i>
                </button>
                <ul class="dropdown-menu dropdown-menu-end">
                    ${menuItems.join('')}
                </ul>
            </div>
        `;

        const logoutLink = document.getElementById('logoutLink');
        if (logoutLink) {
            logoutLink.addEventListener('click', (e) => {
                e.preventDefault();
                clearAuth();
                window.location.href = 'index.html';
            });
        }
    } else {
        userMenu.innerHTML = `
            <a href="login.html" class="btn btn-outline-secondary btn-sm rounded-circle" title="Login">
                <i class="fa fa-user"></i>
            </a>
        `;
    }
}

/**
 * Updates cart count badge in navbar
 */
function updateCartBadge() {
    const cartCountEl = document.getElementById('cartCount');
    if (!cartCountEl) return;

    // Load cart from localStorage
    const CART_KEY = 'cart';
    try {
        const cart = JSON.parse(localStorage.getItem(CART_KEY)) || [];
        const totalQty = cart.reduce((sum, item) => sum + (item.quantity || 0), 0);

        if (totalQty > 0) {
            cartCountEl.textContent = totalQty;
            cartCountEl.style.display = 'inline-block';
        } else {
            cartCountEl.style.display = 'none';
        }
    } catch (e) {
        cartCountEl.style.display = 'none';
    }
}

/**
 * Initialize navbar when DOM is ready
 */
document.addEventListener('DOMContentLoaded', () => {
    // Small delay to ensure auth-utils.js is loaded
    setTimeout(() => {
        if (typeof getRole === 'function') {
            initializeNavbar();
            setActiveNavLink();
        } else {
            console.warn('auth-utils.js not loaded before navbar.js');
        }
    }, 100);
});

// Update navbar when auth status or cart changes
window.addEventListener('storage', () => {
    updateNavbarForRole();
    updateCartBadge();
});
