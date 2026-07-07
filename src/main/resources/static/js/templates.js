/**
 * Templates page: template CRUD with live preview, email sending and send history.
 * All dynamic content is rendered through DOM APIs (textContent) to prevent XSS.
 */
Auth.requireLogin();

const TEMPLATES_API = '/api/templates';
const EMAILS_API = '/api/emails';

const templateList = document.getElementById('templateList');
const templatesEmpty = document.getElementById('templatesEmpty');
const messageList = document.getElementById('messageList');
const messagesEmpty = document.getElementById('messagesEmpty');

const STATUS_LABELS = {
    PENDING: { label: 'Pendente', className: 'badge-pending' },
    RETRYING: { label: 'Reenviando', className: 'badge-retrying' },
    SENT: { label: 'Enviado', className: 'badge-sent' },
    FAILED: { label: 'Falhou', className: 'badge-failed' }
};

/* --------------------------------------------------------------------------
   Bootstrap
   -------------------------------------------------------------------------- */

document.getElementById('userEmail').textContent = Auth.email();
document.getElementById('logoutBtn').addEventListener('click', () => Auth.logout());
document.getElementById('newTemplateBtn').addEventListener('click', () => openTemplateModal(null));
document.getElementById('emptyNewTemplateBtn').addEventListener('click', () => openTemplateModal(null));
document.getElementById('refreshMessagesBtn').addEventListener('click', loadMessages);
document.getElementById('tabBtnHtml').addEventListener('click', () => switchTab('html'));
document.getElementById('tabBtnPreview').addEventListener('click', () => switchTab('preview'));
document.getElementById('updatePreviewBtn').addEventListener('click', updatePreview);
document.getElementById('htmlSource').addEventListener('input', syncPreviewVariables);

document.querySelectorAll('[data-close]').forEach((button) => {
    button.addEventListener('click', () => closeModal(button.dataset.close));
});

loadTemplates();
loadMessages();

/* --------------------------------------------------------------------------
   Helpers
   -------------------------------------------------------------------------- */

function formatDate(value) {
    return value ? new Date(value).toLocaleString('pt-BR') : '—';
}

function extractVariables(html) {
    const vars = {};
    const regex = /\$\{\s*([a-zA-Z_][a-zA-Z0-9_]*)\s*}/g;
    let match;
    while ((match = regex.exec(html)) !== null) {
        vars[match[1]] = vars[match[1]] || '';
    }
    return vars;
}

function parseJsonOrNull(text) {
    try {
        return JSON.parse(text || '{}');
    } catch (error) {
        return null;
    }
}

function showToast(message, type) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast show toast-' + type;
    clearTimeout(showToast.timer);
    showToast.timer = setTimeout(() => { toast.className = 'toast'; }, 3500);
}

function openModal(id) {
    document.getElementById(id).classList.add('show');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('show');
}

function actionButton(label, className, handler) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn btn-sm ' + className;
    button.textContent = label;
    button.addEventListener('click', handler);
    return button;
}

/* --------------------------------------------------------------------------
   Templates
   -------------------------------------------------------------------------- */

async function loadTemplates() {
    try {
        const response = await apiFetch(TEMPLATES_API);
        const templates = await response.json();

        templateList.replaceChildren();
        templatesEmpty.hidden = templates.length > 0;

        for (const template of templates) {
            const row = document.createElement('tr');

            const nameCell = document.createElement('td');
            const nameTitle = document.createElement('div');
            nameTitle.className = 'cell-title';
            nameTitle.textContent = template.name;
            const nameSub = document.createElement('div');
            nameSub.className = 'cell-sub';
            nameSub.textContent = template.id;
            nameCell.append(nameTitle, nameSub);

            const subjectCell = document.createElement('td');
            subjectCell.textContent = template.subject;

            const createdCell = document.createElement('td');
            createdCell.textContent = formatDate(template.createdAt);

            const actionsCell = document.createElement('td');
            const actions = document.createElement('div');
            actions.className = 'actions';
            actions.append(
                actionButton('Enviar', 'btn-primary', () => openSendModal(template)),
                actionButton('Editar', 'btn-outline', () => openTemplateModal(template)),
                actionButton('Excluir', 'btn-danger-outline', () => deleteTemplate(template))
            );
            actionsCell.appendChild(actions);

            row.append(nameCell, subjectCell, createdCell, actionsCell);
            templateList.appendChild(row);
        }
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function openTemplateModal(template) {
    document.getElementById('modalTitle').textContent = template ? 'Editar template' : 'Novo template';
    document.getElementById('templateId').value = template ? template.id : '';
    document.getElementById('name').value = template ? template.name : '';
    document.getElementById('subject').value = template ? template.subject : '';
    document.getElementById('htmlSource').value = template ? template.htmlContent : '';
    syncPreviewVariables();
    switchTab('html');
    openModal('templateModal');
}

function switchTab(tab) {
    const isPreview = tab === 'preview';
    document.getElementById('tabBtnHtml').classList.toggle('active', !isPreview);
    document.getElementById('tabBtnPreview').classList.toggle('active', isPreview);
    document.getElementById('tabHtml').hidden = isPreview;
    document.getElementById('tabPreview').hidden = !isPreview;
    if (isPreview) {
        updatePreview();
    }
}

function syncPreviewVariables() {
    const detected = extractVariables(document.getElementById('htmlSource').value);
    const current = parseJsonOrNull(document.getElementById('previewVars').value) || {};
    const merged = { ...detected, ...current };

    const relevant = {};
    for (const key of Object.keys(detected)) {
        relevant[key] = merged[key];
    }
    document.getElementById('previewVars').value = JSON.stringify(relevant, null, 2);
}

async function updatePreview() {
    const html = document.getElementById('htmlSource').value;
    const variables = parseJsonOrNull(document.getElementById('previewVars').value);
    const frame = document.getElementById('previewFrame');

    if (variables === null) {
        showToast('JSON de variáveis inválido', 'error');
        return;
    }

    try {
        const response = await apiFetch(TEMPLATES_API + '/preview', {
            method: 'POST',
            body: JSON.stringify({ html, variables })
        });
        frame.srcdoc = await response.text();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

document.getElementById('templateForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const saveBtn = document.getElementById('saveTemplateBtn');
    saveBtn.disabled = true;

    const id = document.getElementById('templateId').value;
    const payload = {
        name: document.getElementById('name').value.trim(),
        subject: document.getElementById('subject').value.trim(),
        htmlContent: document.getElementById('htmlSource').value
    };

    try {
        await apiFetch(id ? `${TEMPLATES_API}/${id}` : TEMPLATES_API, {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify(payload)
        });
        showToast('Template salvo com sucesso', 'success');
        closeModal('templateModal');
        await loadTemplates();
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        saveBtn.disabled = false;
    }
});

async function deleteTemplate(template) {
    if (!confirm(`Excluir o template "${template.name}"?`)) {
        return;
    }
    try {
        await apiFetch(`${TEMPLATES_API}/${template.id}`, { method: 'DELETE' });
        showToast('Template excluído', 'success');
        await loadTemplates();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

/* --------------------------------------------------------------------------
   Sending emails
   -------------------------------------------------------------------------- */

function openSendModal(template) {
    document.getElementById('sendTemplateId').value = template.id;
    document.getElementById('toEmail').value = '';
    document.getElementById('sendVars').value =
        JSON.stringify(extractVariables(template.htmlContent), null, 2);
    openModal('sendModal');
}

document.getElementById('sendForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const sendBtn = document.getElementById('sendEmailBtn');

    const variables = parseJsonOrNull(document.getElementById('sendVars').value);
    if (variables === null) {
        showToast('JSON de variáveis inválido', 'error');
        return;
    }

    sendBtn.disabled = true;
    try {
        await apiFetch(EMAILS_API + '/send', {
            method: 'POST',
            body: JSON.stringify({
                templateId: document.getElementById('sendTemplateId').value,
                toEmail: document.getElementById('toEmail').value.trim(),
                variables
            })
        });
        showToast('E-mail enfileirado para envio', 'success');
        closeModal('sendModal');
        await loadMessages();
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        sendBtn.disabled = false;
    }
});

/* --------------------------------------------------------------------------
   Send history
   -------------------------------------------------------------------------- */

async function loadMessages() {
    try {
        const response = await apiFetch(EMAILS_API);
        const messages = await response.json();

        messageList.replaceChildren();
        messagesEmpty.hidden = messages.length > 0;

        for (const message of messages) {
            const row = document.createElement('tr');

            const toCell = document.createElement('td');
            toCell.textContent = message.toEmail;

            const templateCell = document.createElement('td');
            templateCell.textContent = message.templateName;

            const statusCell = document.createElement('td');
            const status = STATUS_LABELS[message.status] || { label: message.status, className: 'badge-pending' };
            const badge = document.createElement('span');
            badge.className = 'badge ' + status.className;
            badge.textContent = status.label;
            statusCell.appendChild(badge);
            if (message.errorMessage) {
                const errorInfo = document.createElement('div');
                errorInfo.className = 'cell-sub';
                errorInfo.textContent = message.errorMessage;
                statusCell.appendChild(errorInfo);
            }

            const retryCell = document.createElement('td');
            retryCell.textContent = String(message.retryCount);

            const sentCell = document.createElement('td');
            sentCell.textContent = formatDate(message.sentAt);

            row.append(toCell, templateCell, statusCell, retryCell, sentCell);
            messageList.appendChild(row);
        }
    } catch (error) {
        showToast(error.message, 'error');
    }
}
