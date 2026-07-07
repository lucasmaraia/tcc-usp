if (Auth.isLoggedIn()) {
    window.location.href = '/templates.html';
}

const form = document.getElementById('loginForm');
const errorEl = document.getElementById('error');
const submitBtn = document.getElementById('submitBtn');

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    errorEl.classList.remove('show');
    submitBtn.disabled = true;

    try {
        const response = await apiFetch('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({
                username: document.getElementById('username').value.trim(),
                password: document.getElementById('password').value
            })
        });

        Auth.save(await response.json());
        window.location.href = '/templates.html';
    } catch (error) {
        errorEl.textContent = error.message;
        errorEl.classList.add('show');
    } finally {
        submitBtn.disabled = false;
    }
});
