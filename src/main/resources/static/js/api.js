const Auth = {
    save(data) {
        localStorage.setItem('token', data.token);
        localStorage.setItem('username', data.username);
        localStorage.setItem('email', data.email);
    },

    token() {
        return localStorage.getItem('token');
    },

    email() {
        return localStorage.getItem('email') || '';
    },

    isLoggedIn() {
        return Boolean(this.token());
    },

    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('username');
        localStorage.removeItem('email');
        window.location.href = '/login.html';
    },

    requireLogin() {
        if (!this.isLoggedIn()) {
            window.location.href = '/login.html';
        }
    }
};

async function apiFetch(url, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (Auth.token()) {
        headers['Authorization'] = 'Bearer ' + Auth.token();
    }

    const response = await fetch(url, { ...options, headers });

    if ((response.status === 401 || response.status === 403) && Auth.isLoggedIn()) {
        Auth.logout();
        throw new Error('Sessão expirada');
    }

    if (!response.ok) {
        let message = 'Erro ' + response.status;
        try {
            const problem = await response.json();
            message = problem.detail || problem.message || message;
        } catch (ignored) {
        }
        throw new Error(message);
    }

    return response;
}
