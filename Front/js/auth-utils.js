// js/auth-utils.js

// URL de la gateway Spring Boot
const API_BASE = 'http://localhost:8222';

// ====== Gestion stockage auth (localStorage) ======

function saveAuth(data) {
    // data = { token, role, username }
    localStorage.setItem('token', data.token);
    localStorage.setItem('role', data.role);
    localStorage.setItem('username', data.username || '');
}

function clearAuth() {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('username');
}

function getToken() {
    return localStorage.getItem('token');
}

function getRole() {
    return localStorage.getItem('role');
}

function getUsername() {
    return localStorage.getItem('username');
}

// ====== Helpers pour fetch avec Authorization ======

/**
 * Retourne un objet headers avec JSON + Authorization si token présent.
 */
function authHeaders(isJson = true) {
    const headers = {};
    if (isJson) {
        headers['Content-Type'] = 'application/json';
    }
    const token = getToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    return headers;
}

/**
 * Raccourci pour fetch protégé (renvoie directement la Response).
 */
function authFetch(url, options = {}) {
    const opts = Object.assign({}, options);
    opts.headers = Object.assign(
        {},
        authHeaders(options && options.body !== undefined),
        options.headers || {}
    );
    return fetch(url, opts);
}

// ====== Protection des pages ======

function requireSeller() {
    const role = getRole();
    if (!getToken() || (role !== 'SHOP' && role !== 'SELLER')) {
        window.location.href = 'login.html';
    }
}

function requireClient() {
    const role = getRole();
    if (!getToken() || role !== 'CLIENT') {
        window.location.href = 'login.html';
    }
}

// ====== Menu utilisateur dans la navbar ======

function updateUserMenu() {
    const userMenu = document.getElementById('userMenu');
    if (!userMenu) return;

    const username = getUsername();
    const role = getRole();

    if (getToken() && username) {
        userMenu.innerHTML = `
      <div class="dropdown">
        <a href="#" class="btn btn-outline-secondary rounded-circle dropdown-toggle"
           data-bs-toggle="dropdown">
          <i class="fa fa-user"></i>
        </a>
        <ul class="dropdown-menu dropdown-menu-end">
          <li class="dropdown-header">${username} (${role})</li>
          <li><a class="dropdown-item" href="orders.html">My Orders</a></li>
          <li><a class="dropdown-item" href="cart.html">My Cart</a></li>
          ${role === 'SHOP' || role === 'SELLER'
            ? '<li><a class="dropdown-item" href="seller-dashboard.html">Seller Dashboard</a></li>'
            : ''}
          <li><hr class="dropdown-divider"></li>
          <li><a class="dropdown-item" href="#" id="logoutLink">Logout</a></li>
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
      <a href="login.html" class="btn btn-outline-secondary rounded-circle">
        <i class="fa fa-user"></i>
      </a>
    `;
    }
}

// Appelé automatiquement sur chaque page
document.addEventListener('DOMContentLoaded', updateUserMenu);
