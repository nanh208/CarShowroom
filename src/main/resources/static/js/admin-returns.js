const tokenInput = document.getElementById('adminToken');
const messageBox = document.getElementById('adminMessage');
const returnList = document.getElementById('returnList');

tokenInput.value = sessionStorage.getItem('ares_admin_token') || '';

async function adminRequest(url, options = {}) {
  const token = tokenInput.value.trim();
  if (!token) throw new Error('Nhập mã quản trị để tiếp tục.');
  sessionStorage.setItem('ares_admin_token', token);
  const response = await fetch(url, {
    ...options,
    headers: { Accept: 'application/json', 'Content-Type': 'application/json', 'X-Admin-Token': token, ...(options.headers || {}) }
  });
  const result = await response.json();
  if (!response.ok || !result.success) throw new Error(result.message || 'Yêu cầu quản trị thất bại.');
  return result.data;
}

function statusLabel(status) {
  return ({ PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối', COMPLETED: 'Đã xử lý xong' })[status] || status;
}

function makeText(tag, text, className) {
  const node = document.createElement(tag);
  node.textContent = text || '';
  if (className) node.className = className;
  return node;
}

function renderReturns(items) {
  returnList.replaceChildren();
  if (!items.length) {
    returnList.appendChild(makeText('p', 'Chưa có yêu cầu đổi trả.'));
    return;
  }
  items.forEach((item) => {
    const card = document.createElement('article');
    card.className = 'return-card';
    const title = makeText('h2', `${item.requestCode} · đơn ${item.orderCode}`);
    const meta = makeText('p', `Khách: ${item.customerName} · ${item.phone}\nLoại: ${item.kind === 'REFUND' ? 'Hoàn tiền' : 'Đổi sản phẩm'} · ${statusLabel(item.status)}\nGửi lúc: ${new Date(item.createdAt).toLocaleString('vi-VN')} · Giá trị đề xuất hoàn: ${Number(item.estimatedRefundAmount).toLocaleString('vi-VN')} ₫`, 'return-meta');
    const reason = makeText('p', `Lý do: ${item.reason}`);
    const lines = document.createElement('div');
    lines.className = 'return-lines';
    item.items.forEach(line => lines.appendChild(makeText('div', `${line.productName} (${line.productCode}) × ${line.purchasedQuantity} · ${Number(line.unitPrice).toLocaleString('vi-VN')} ₫`, 'return-line')));
    const priorNote = makeText('p', item.adminNote ? `Ghi chú xử lý trước: ${item.adminNote}` : '');
    const actions = document.createElement('div');
    actions.className = 'return-actions';
    const status = document.createElement('select');
    status.setAttribute('aria-label', `Trạng thái ${item.requestCode}`);
    const allowed = item.status === 'PENDING' ? ['APPROVED', 'REJECTED'] : item.status === 'APPROVED' ? ['COMPLETED', 'REJECTED'] : [];
    [item.status, ...allowed].forEach(value => {
      const option = document.createElement('option');
      option.value = value;
      option.textContent = statusLabel(value);
      status.appendChild(option);
    });
    const note = document.createElement('textarea');
    note.maxLength = 1000;
    note.placeholder = 'Ghi chú cho khách (bắt buộc khi từ chối)';
    note.value = item.adminNote || '';
    const save = document.createElement('button');
    save.type = 'button';
    save.className = 'cart-btn-checkout';
    save.textContent = 'Lưu trạng thái';
    save.disabled = !allowed.length;
    save.addEventListener('click', () => updateReturn(item.requestCode, status.value, note.value, save));
    actions.append(status, save);
    card.append(title, meta, reason, lines, priorNote, note, actions);
    returnList.appendChild(card);
  });
}

async function loadReturns() {
  messageBox.textContent = 'Đang tải danh sách…';
  try {
    const items = await adminRequest('/api/admin/returns');
    renderReturns(items || []);
    messageBox.textContent = `Đã tải ${items.length} yêu cầu.`;
  } catch (error) {
    messageBox.textContent = error.message || 'Không tải được danh sách.';
    returnList.replaceChildren();
  }
}

async function updateReturn(requestCode, status, note, button) {
  button.disabled = true;
  messageBox.textContent = 'Đang lưu trạng thái…';
  try {
    await adminRequest(`/api/admin/returns/${encodeURIComponent(requestCode)}`, {
      method: 'PATCH', body: JSON.stringify({ status, note: note.trim() })
    });
    messageBox.textContent = `Đã cập nhật ${requestCode}: ${statusLabel(status)}.`;
    await loadReturns();
  } catch (error) {
    messageBox.textContent = error.message || 'Không lưu được trạng thái.';
    button.disabled = false;
  }
}

document.getElementById('loadReturns').addEventListener('click', loadReturns);
document.getElementById('forgetAdminToken').addEventListener('click', () => {
  tokenInput.value = '';
  sessionStorage.removeItem('ares_admin_token');
  returnList.replaceChildren();
  messageBox.textContent = 'Đã xóa mã quản trị khỏi phiên trình duyệt.';
});
