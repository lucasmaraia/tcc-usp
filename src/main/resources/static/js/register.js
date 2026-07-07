if (Auth.isLoggedIn()) {
    window.location.href = '/templates.html';
}

const form = document.getElementById('registerForm');
const errorEl = document.getElementById('error');
const submitBtn = document.getElementById('submitBtn');

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    errorEl.classList.remove('show');

    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (password !== confirmPassword) {
        errorEl.textContent = 'As senhas não coincidem';
        errorEl.classList.add('show');
        return;
    }

    submitBtn.disabled = true;

    try {
        const response = await apiFetch('/api/auth/register', {
            method: 'POST',
            body: JSON.stringify({
                username: document.getElementById('username').value.trim(),
                email: document.getElementById('email').value.trim(),
                password: password
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
