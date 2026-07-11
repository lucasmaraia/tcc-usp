Auth.requireLogin();

const TEMPLATES_API = '/api/templates';
const EMAILS_API = '/api/emails';

const templateList = document.getElementById('templateList');
const templatesEmpty = document.getElementById('templatesEmpty');
const messageList = document.getElementById('messageList');
const messagesEmpty = document.getElementById('messagesEmpty');

const PAGE_SIZE = 10;
const templatesState = { page: 0, totalPages: 0, search: '' };
const messagesState = { page: 0, totalPages: 0, status: '', toEmail: '' };

const STATUS_LABELS = {
    PENDING: { label: 'Pendente', className: 'badge-pending' },
    RETRYING: { label: 'Reenviando', className: 'badge-retrying' },
    SENT: { label: 'Enviado', className: 'badge-sent' },
    FAILED: { label: 'Falhou', className: 'badge-failed' }
};

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

const templateSearchInput = document.getElementById('templateSearch');
templateSearchInput.addEventListener('input', debounce(() => {
    templatesState.search = templateSearchInput.value.trim();
    templatesState.page = 0;
    loadTemplates();
}, 300));

document.getElementById('templatesPrev').addEventListener('click', () => changePage(templatesState, -1, loadTemplates));
document.getElementById('templatesNext').addEventListener('click', () => changePage(templatesState, 1, loadTemplates));

const messageStatusFilter = document.getElementById('messageStatusFilter');
messageStatusFilter.addEventListener('change', () => {
    messagesState.status = messageStatusFilter.value;
    messagesState.page = 0;
    loadMessages();
});

const messageRecipientFilter = document.getElementById('messageRecipientFilter');
messageRecipientFilter.addEventListener('input', debounce(() => {
    messagesState.toEmail = messageRecipientFilter.value.trim();
    messagesState.page = 0;
    loadMessages();
}, 300));

document.getElementById('messagesPrev').addEventListener('click', () => changePage(messagesState, -1, loadMessages));
document.getElementById('messagesNext').addEventListener('click', () => changePage(messagesState, 1, loadMessages));

document.getElementById('templatesClearFilters').addEventListener('click', () => {
    templateSearchInput.value = '';
    templatesState.search = '';
    templatesState.page = 0;
    loadTemplates();
});

document.getElementById('messagesClearFilters').addEventListener('click', () => {
    messageStatusFilter.value = '';
    messageRecipientFilter.value = '';
    messagesState.status = '';
    messagesState.toEmail = '';
    messagesState.page = 0;
    loadMessages();
});

loadTemplates();
loadMessages();

function formatDate(value) {
    return value ? new Date(value).toLocaleString('pt-BR') : '—';
}

function debounce(fn, delayMs) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delayMs);
    };
}

function changePage(state, delta, reload) {
    const next = state.page + delta;
    if (next < 0 || next >= state.totalPages) {
        return;
    }
    state.page = next;
    reload();
}

function renderPagination(prefix, state, data) {
    state.totalPages = data.totalPages;
    if (state.page >= data.totalPages && data.totalPages > 0) {
        state.page = data.totalPages - 1;
    }
    document.getElementById(prefix + 'Pagination').hidden = data.totalPages <= 1;
    document.getElementById(prefix + 'PageInfo').textContent =
        `Página ${data.page + 1} de ${data.totalPages} — ${data.totalElements} registro(s)`;
    document.getElementById(prefix + 'Prev').disabled = data.page <= 0;
    document.getElementById(prefix + 'Next').disabled = data.page >= data.totalPages - 1;
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

async function loadTemplates() {
    try {
        const params = new URLSearchParams({ page: templatesState.page, size: PAGE_SIZE });
        if (templatesState.search) {
            params.set('search', templatesState.search);
        }
        const response = await apiFetch(`${TEMPLATES_API}?${params}`);
        const data = await response.json();
        const templates = data.content;

        templateList.replaceChildren();
        templatesEmpty.hidden = templates.length > 0;
        document.getElementById('templatesEmptyText').textContent = templatesState.search
            ? 'Nenhum template encontrado para a busca.'
            : 'Nenhum template ainda. Crie o primeiro!';
        document.getElementById('emptyNewTemplateBtn').hidden = Boolean(templatesState.search);
        document.getElementById('templatesClearFilters').hidden = !templatesState.search;
        renderPagination('templates', templatesState, data);

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
        if (templateList.children.length === 1 && templatesState.page > 0) {
            templatesState.page -= 1;
        }
        await loadTemplates();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

const MAX_ATTACHMENTS = 5;
const MAX_ATTACHMENT_TOTAL_BYTES = 10 * 1024 * 1024;

function openSendModal(template) {
    document.getElementById('sendTemplateId').value = template.id;
    document.getElementById('toEmail').value = '';
    document.getElementById('sendVars').value =
        JSON.stringify(extractVariables(template.htmlContent), null, 2);
    document.getElementById('sendAttachments').value = '';
    renderAttachmentList();
    openModal('sendModal');
}

document.getElementById('sendAttachments').addEventListener('change', renderAttachmentList);

function formatFileSize(bytes) {
    if (bytes < 1024) {
        return bytes + ' B';
    }
    if (bytes < 1024 * 1024) {
        return (bytes / 1024).toFixed(1) + ' KB';
    }
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function renderAttachmentList() {
    const files = Array.from(document.getElementById('sendAttachments').files);
    const list = document.getElementById('attachmentList');

    list.replaceChildren();
    list.hidden = files.length === 0;

    for (const file of files) {
        const item = document.createElement('li');
        item.textContent = `${file.name} (${formatFileSize(file.size)})`;
        list.appendChild(item);
    }
}

function readFileAsBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result.split(',')[1]);
        reader.onerror = () => reject(new Error(`Falha ao ler o arquivo "${file.name}"`));
        reader.readAsDataURL(file);
    });
}

async function collectAttachments() {
    const files = Array.from(document.getElementById('sendAttachments').files);
    if (files.length === 0) {
        return [];
    }
    if (files.length > MAX_ATTACHMENTS) {
        throw new Error(`Máximo de ${MAX_ATTACHMENTS} anexos por e-mail`);
    }
    const totalBytes = files.reduce((sum, file) => sum + file.size, 0);
    if (totalBytes > MAX_ATTACHMENT_TOTAL_BYTES) {
        throw new Error('Os anexos ultrapassam o limite total de 10 MB');
    }
    return Promise.all(files.map(async (file) => ({
        filename: file.name,
        contentType: file.type || 'application/octet-stream',
        base64Content: await readFileAsBase64(file)
    })));
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
        const attachments = await collectAttachments();
        await apiFetch(EMAILS_API + '/send', {
            method: 'POST',
            body: JSON.stringify({
                templateId: document.getElementById('sendTemplateId').value,
                toEmail: document.getElementById('toEmail').value.trim(),
                variables,
                attachments
            })
        });
        showToast('E-mail enfileirado para envio', 'success');
        closeModal('sendModal');
        messagesState.page = 0;
        await loadMessages();
    } catch (error) {
        showToast(error.message, 'error');
    } finally {
        sendBtn.disabled = false;
    }
});

async function loadMessages() {
    try {
        const params = new URLSearchParams({ page: messagesState.page, size: PAGE_SIZE });
        if (messagesState.status) {
            params.set('status', messagesState.status);
        }
        if (messagesState.toEmail) {
            params.set('toEmail', messagesState.toEmail);
        }
        const response = await apiFetch(`${EMAILS_API}?${params}`);
        const data = await response.json();
        const messages = data.content;

        messageList.replaceChildren();
        messagesEmpty.hidden = messages.length > 0;
        document.getElementById('messagesEmptyText').textContent =
            messagesState.status || messagesState.toEmail
                ? 'Nenhum envio encontrado com os filtros aplicados.'
                : 'Nenhum e-mail enviado ainda.';
        document.getElementById('messagesClearFilters').hidden =
            !(messagesState.status || messagesState.toEmail);
        renderPagination('messages', messagesState, data);

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

            const attachmentsCell = document.createElement('td');
            attachmentsCell.textContent = message.attachmentNames || '—';

            const retryCell = document.createElement('td');
            retryCell.textContent = String(message.retryCount);

            const sentCell = document.createElement('td');
            sentCell.textContent = formatDate(message.sentAt);

            row.append(toCell, templateCell, statusCell, attachmentsCell, retryCell, sentCell);
            messageList.appendChild(row);
        }
    } catch (error) {
        showToast(error.message, 'error');
    }
}
