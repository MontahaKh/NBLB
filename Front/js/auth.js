// js/auth.js

// -------------------------
// LOGIN avec /auth/api/login (JSON)
// -------------------------
const loginForm = document.getElementById('loginForm');
const loginError = document.getElementById('loginError');

if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value.trim();
        if (loginError) loginError.style.display = 'none';

        try {
            const res = await fetch(`${API_BASE}/auth/api/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })   // correspond à @RequestBody User
            });

            if (!res.ok) {
                if (loginError) loginError.style.display = 'block';
                return;
            }

            const data = await res.json(); // attendu: { token, role, username? }

            // Supporte différents formats possibles: {role}, {roles:[...]}, {authorities:[...]}
            const roleCandidate = (data && (data.role || (Array.isArray(data.roles) ? data.roles[0] : null) || (Array.isArray(data.authorities) ? data.authorities[0] : null))) || '';
            const role = normalizeRole(roleCandidate);

            // Sauvegarde dans localStorage (défini dans auth-utils.js)
            saveAuth({
                token: data.token,
                role,
                username: data.username || username
            });

            // Redirection selon le rôle (front)
            if (role === 'ADMIN') {
                window.location.href = frontUrl('admin-dashboard.html');
            } else if (role === 'SHOP' || role === 'SELLER') {
                window.location.href = frontUrl('seller-dashboard.html');
            } else {
                window.location.href = frontUrl('index.html');
            }
        } catch (err) {
            console.error(err);
            if (loginError) loginError.style.display = 'block';
        }
    });
}

// -------------------------
// REGISTER avec /auth/api/register (JSON)
// -------------------------
const registerForm = document.getElementById('registerForm');
const registerError = document.getElementById('registerError');
const registerSuccess = document.getElementById('registerSuccess');

if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const username = document.getElementById('regUsername').value.trim();
        const email = document.getElementById('regEmail').value.trim();
        const password = document.getElementById('regPassword').value.trim();

        // Récupérer la valeur du <select id="regRole">
        // Les values doivent être EXACTEMENT ce que ton backend attend : CLIENT / SHOP / ADMIN
        const roleSelect = document.getElementById('regRole');
        const role = roleSelect ? roleSelect.value : 'CLIENT';

        if (registerError) registerError.style.display = 'none';
        if (registerSuccess) registerSuccess.style.display = 'none';

        try {
            const res = await fetch(`${API_BASE}/auth/api/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, password, role })
            });

            if (!res.ok) {
                if (registerError) registerError.style.display = 'block';
                return;
            }

            if (registerSuccess) registerSuccess.style.display = 'block';
            setTimeout(() => window.location.href = 'login.html', 2000);
        } catch (err) {
            console.error(err);
            if (registerError) registerError.style.display = 'block';
        }
    });
}
