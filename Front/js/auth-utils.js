// js/auth-utils.js

// URL de la gateway Spring Boot
// Note: sur certaines machines, `localhost` résout en IPv6 (::1) et peut échouer si le backend n'écoute qu'en IPv4.
// On force donc l'IPv4 loopback quand on est sur localhost / ::1.
const __host = (typeof window !== 'undefined' && window.location && window.location.hostname) ? window.location.hostname : '127.0.0.1';
const __apiHost = (__host === 'localhost' || __host === '::1' || __host === '[::1]') ? '127.0.0.1' : __host;
const API_BASE = `http://${__apiHost}:8222`;

function frontBaseUrl() {
    if (typeof window === 'undefined' || !window.location) return '';
    const port = window.location.port ? `:${window.location.port}` : '';
    const host = (window.location.hostname === 'localhost' || window.location.hostname === '::1' || window.location.hostname === '[::1]')
        ? '127.0.0.1'
        : window.location.hostname;
    return `${window.location.protocol}//${host}${port}`;
}

function frontUrl(path) {
    const base = frontBaseUrl();
    if (!base) return path;
    if (!path) return base;
    return `${base}/${String(path).replace(/^\/+/, '')}`;
}

function normalizeRole(role) {
    const raw = String(role || '').trim().toUpperCase();
    if (!raw) return '';
    return raw.startsWith('ROLE_') ? raw.substring('ROLE_'.length) : raw;
}

// ====== Gestion stockage auth (localStorage) ======

function saveAuth(data) {
    // data = { token, role, username }
    localStorage.setItem('token', data.token);
    localStorage.setItem('role', normalizeRole(data.role));
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
    return normalizeRole(localStorage.getItem('role'));
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
        window.location.href = frontUrl('login.html');
    }
}

function requireClient() {
    const role = getRole();
    if (!getToken() || role !== 'CLIENT') {
        window.location.href = frontUrl('login.html');
    }
}

function requireAdmin() {
    const role = getRole();
    if (!getToken() || role !== 'ADMIN') {
        window.location.href = frontUrl('login.html');
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
