/**
 * Shared API client: authentication storage and a fetch wrapper that
 * understands the backend's RFC 9457 problem-detail error responses.
 */
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

/**
 * Performs a fetch with JSON headers and the Bearer token (when present).
 * Throws an Error with a readable message on non-2xx responses.
 * Redirects to the login page when the session is no longer valid.
 */
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
            // Non-JSON error body: keep the generic message.
        }
        throw new Error(message);
    }

    return response;
}
