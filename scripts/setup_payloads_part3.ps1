# setup_payloads_part3.ps1 - Generates UI payloads for Days 17 to 20
$base = "c:\Users\koreka\CarShowroom"
$payloadsRoot = Join-Path $base "scripts\payloads"

function Create-PayloadDir($dayNum) {
    $dir = Join-Path $payloadsRoot ("day{0:D2}" -f $dayNum)
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
    }
    return $dir
}

# --- DAY 17: chat-widget.html ---
$d17 = Create-PayloadDir 17
$d17Tpl = Join-Path $d17 "src\main\resources\templates\fragments"
$d17Ctrl = Join-Path $d17 "src\main\java\com\ares\controller"
New-Item -ItemType Directory -Force -Path $d17Tpl | Out-Null
New-Item -ItemType Directory -Force -Path $d17Ctrl | Out-Null

@'
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<div th:fragment="chatWidget">
    <style>
        .chat-widget-toggle { position: fixed; bottom: 25px; right: 25px; width: 62px; height: 62px; border-radius: 50%; background: linear-gradient(135deg, #0284c7, #2563eb); color: white; display: flex; align-items: center; justify-content: center; cursor: pointer; box-shadow: 0 10px 25px rgba(2, 132, 199, 0.4); z-index: 9999; transition: transform 0.2s, box-shadow 0.2s; border: 2px solid rgba(255, 255, 255, 0.2); }
        .chat-widget-toggle:hover { transform: scale(1.08); }
        .chat-widget-box { position: fixed; bottom: 100px; right: 25px; width: 360px; height: 500px; background: #0f172a; border: 1px solid #334155; border-radius: 18px; box-shadow: 0 20px 40px rgba(0,0,0,0.6); display: none; flex-direction: column; z-index: 9999; overflow: hidden; font-family: 'Segoe UI', sans-serif; }
        .chat-header { background: #1e293b; padding: 14px 18px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #334155; }
        .chat-header h4 { margin: 0; color: #38bdf8; font-size: 1.05rem; }
        .chat-body { flex: 1; padding: 14px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; }
        .chat-msg { max-width: 80%; padding: 10px 14px; border-radius: 14px; font-size: 0.9rem; line-height: 1.4; word-break: break-word; }
        .chat-msg.customer { align-self: flex-end; background: #0284c7; color: #fff; border-bottom-right-radius: 2px; }
        .chat-msg.admin { align-self: flex-start; background: #1e293b; color: #f1f5f9; border-bottom-left-radius: 2px; border: 1px solid #334155; }
        .chat-quick-btns { display: flex; flex-wrap: wrap; gap: 6px; padding: 6px 14px; background: #1e293b; border-top: 1px solid #334155; }
        .btn-quick-reply { background: #0f172a; color: #38bdf8; border: 1px solid #334155; border-radius: 12px; padding: 5px 10px; font-size: 0.8rem; cursor: pointer; }
        .btn-quick-reply:hover { background: #0284c7; color: #fff; }
        .chat-footer { padding: 10px 14px; background: #1e293b; display: flex; gap: 8px; }
        .chat-input { flex: 1; background: #0f172a; border: 1px solid #475569; color: #fff; padding: 8px 12px; border-radius: 10px; font-size: 0.9rem; }
        .btn-chat-send { background: #0284c7; color: #fff; border: none; padding: 8px 14px; border-radius: 10px; cursor: pointer; font-weight: 600; }
    </style>

    <div class="chat-widget-toggle" onclick="toggleChatWidget()">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
    </div>

    <div id="chatWidgetBox" class="chat-widget-box">
        <div class="chat-header">
            <h4>Ares CSKH & Trợ Lý AI</h4>
            <span style="cursor:pointer;color:#94a3b8;font-size:1.2rem;" onclick="toggleChatWidget()">&times;</span>
        </div>
        <div id="chatBody" class="chat-body">
            <div class="chat-msg admin">Xin chào! Em có thể giúp gì cho quý khách về giá xe, trả góp hoặc đặt lịch lái thử hôm nay ạ?</div>
        </div>
        <div class="chat-quick-btns">
            <button class="btn-quick-reply" onclick="sendQuick('Bảng giá xe mới nhất')">Bảng giá xe</button>
            <button class="btn-quick-reply" onclick="sendQuick('Tư vấn mua trả góp 0%')">Mua trả góp</button>
            <button class="btn-quick-reply" onclick="sendQuick('Đăng ký lái thử xe')">Đăng ký lái thử</button>
        </div>
        <div class="chat-footer">
            <input type="text" id="widgetInput" class="chat-input" placeholder="Nhập câu hỏi của bạn..." onkeypress="handleWidgetKey(event)">
            <button class="btn-chat-send" onclick="sendWidgetMsg()">Gửi</button>
        </div>
    </div>

    <script>
        let chatSessionId = localStorage.getItem('ares_chat_session') || ('s_' + Math.random().toString(36).substring(2, 10));
        localStorage.setItem('ares_chat_session', chatSessionId);

        function toggleChatWidget() {
            const box = document.getElementById('chatWidgetBox');
            box.style.display = (box.style.display === 'flex') ? 'none' : 'flex';
        }

        function handleWidgetKey(e) {
            if (e.key === 'Enter') sendWidgetMsg();
        }

        function sendQuick(text) {
            document.getElementById('widgetInput').value = text;
            sendWidgetMsg();
        }

        async function sendWidgetMsg() {
            const inp = document.getElementById('widgetInput');
            const text = inp.value.trim();
            if (!text) return;

            appendMsg(text, 'customer');
            inp.value = '';

            try {
                await fetch(`/api/chat/${chatSessionId}`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ customerName: 'Khách hàng', content: text, fromAdmin: false })
                });
                setTimeout(pollWidgetMessages, 600);
            } catch(e) { console.error(e); }
        }

        function appendMsg(text, type) {
            const body = document.getElementById('chatBody');
            const d = document.createElement('div');
            d.className = `chat-msg ${type}`;
            d.innerText = text;
            body.appendChild(d);
            body.scrollTop = body.scrollHeight;
        }

        async function pollWidgetMessages() {
            try {
                const res = await fetch(`/api/chat/${chatSessionId}`);
                const r = await res.json();
                if (r.data && r.data.length > 0) {
                    const body = document.getElementById('chatBody');
                    body.innerHTML = '';
                    r.data.forEach(m => appendMsg(m.content, m.fromAdmin ? 'admin' : 'customer'));
                }
            } catch(e) {}
        }
        setInterval(pollWidgetMessages, 4000);
    </script>
</div>
</body>
</html>
'@ | Set-Content (Join-Path $d17Tpl "chat-widget.html") -Encoding UTF8

# --- DAY 18: admin-chat.html ---
$d18 = Create-PayloadDir 18
$d18Tpl = Join-Path $d18 "src\main\resources\templates"
New-Item -ItemType Directory -Force -Path $d18Tpl | Out-Null

@'
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Hộp Thư CSKH Trực Tuyến - Admin Ares</title>
    <style>
        body { background: #090d16; color: #f1f5f9; font-family: 'Segoe UI', sans-serif; margin: 0; padding: 20px; display: flex; height: 90vh; gap: 20px; }
        .session-panel { width: 300px; background: #1e293b; border-radius: 14px; border: 1px solid #334155; padding: 15px; overflow-y: auto; }
        .session-item { padding: 12px; background: #0f172a; border-radius: 10px; margin-bottom: 8px; cursor: pointer; border: 1px solid #334155; }
        .session-item.active { border-color: #38bdf8; background: #1e293b; }
        .chat-main { flex: 1; background: #1e293b; border-radius: 14px; border: 1px solid #334155; display: flex; flex-direction: column; }
        .chat-log { flex: 1; padding: 20px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; }
        .msg { padding: 10px 15px; border-radius: 12px; max-width: 70%; font-size: 0.95rem; }
        .msg.admin { align-self: flex-end; background: #0284c7; color: #fff; }
        .msg.cust { align-self: flex-start; background: #334155; color: #fff; }
        .chat-reply-bar { padding: 15px; background: #0f172a; display: flex; gap: 10px; border-top: 1px solid #334155; }
        .reply-input { flex: 1; background: #1e293b; border: 1px solid #475569; color: #fff; padding: 10px; border-radius: 8px; }
        .btn-reply { background: #0284c7; color: #fff; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer; font-weight: 600; }
    </style>
</head>
<body>
    <div class="session-panel">
        <h3 style="margin-top:0;color:#38bdf8;">Hội thoại khách hàng</h3>
        <div id="sessionList">Đang tải danh sách...</div>
    </div>
    <div class="chat-main">
        <div id="chatLog" class="chat-log">
            <p style="color:#64748b;text-align:center;">Chọn một phiên chat từ danh sách bên trái để phản hồi.</p>
        </div>
        <div class="chat-reply-bar">
            <input type="text" id="adminReplyInput" class="reply-input" placeholder="Soạn tin nhắn trả lời khách hàng...">
            <button class="btn-reply" onclick="sendAdminReply()">Gửi phản hồi</button>
        </div>
    </div>

    <script>
        let currentSession = null;
        async function loadSessions() {
            const res = await fetch('/api/chat/admin/sessions');
            const data = await res.json();
            const list = data.data || [];
            document.getElementById('sessionList').innerHTML = list.map(s => `
                <div class="session-item ${currentSession === s ? 'active' : ''}" onclick="selectSession('${s}')">
                    <strong>Phiên: ${s}</strong>
                    <div style="font-size:0.8rem;color:#94a3b8;">Đang kết nối</div>
                </div>
            `).join('');
        }

        async function selectSession(s) {
            currentSession = s;
            loadSessions();
            loadMessages();
        }

        async function loadMessages() {
            if (!currentSession) return;
            const res = await fetch(`/api/chat/${currentSession}`);
            const data = await res.json();
            const list = data.data || [];
            const log = document.getElementById('chatLog');
            log.innerHTML = list.map(m => `
                <div class="msg ${m.fromAdmin ? 'admin' : 'cust'}">
                    <div style="font-size:0.75rem;opacity:0.8;margin-bottom:3px;">${m.customerName || 'Khách'}</div>
                    ${m.content}
                </div>
            `).join('');
            log.scrollTop = log.scrollHeight;
        }

        async function sendAdminReply() {
            const inp = document.getElementById('adminReplyInput');
            const text = inp.value.trim();
            if (!text || !currentSession) return;
            await fetch(`/api/chat/${currentSession}`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ customerName: 'Chuyên viên Ares', content: text, fromAdmin: true })
            });
            inp.value = '';
            loadMessages();
        }

        loadSessions();
        setInterval(loadMessages, 3000);
    </script>
</body>
</html>
'@ | Set-Content (Join-Path $d18Tpl "admin-chat.html") -Encoding UTF8

# --- DAY 19: admin-crm.html ---
$d19 = Create-PayloadDir 19
$d19Tpl = Join-Path $d19 "src\main\resources\templates"
New-Item -ItemType Directory -Force -Path $d19Tpl | Out-Null

@'
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>CRM Quản Lý Khách Hàng Tiềm Năng - Ares Showroom</title>
    <style>
        body { background: #090d16; color: #f1f5f9; font-family: 'Segoe UI', sans-serif; padding: 25px; }
        .crm-table { width: 100%; border-collapse: collapse; background: #1e293b; border-radius: 12px; overflow: hidden; margin-top: 20px; }
        .crm-table th, .crm-table td { padding: 14px; text-align: left; border-bottom: 1px solid #334155; }
        .crm-table th { background: #0f172a; color: #94a3b8; font-weight: 600; }
        .badge-status { padding: 4px 10px; border-radius: 6px; font-size: 0.8rem; font-weight: 700; }
        .badge-NEW { background: #3b82f6; color: #fff; }
        .badge-IN_PROGRESS { background: #eab308; color: #000; }
        .badge-RESOLVED { background: #10b981; color: #fff; }
    </style>
</head>
<body>
    <h2>HỆ THỐNG CRM CHĂM SÓC KHÁCH HÀNG & LEADS</h2>
    <table class="crm-table">
        <thead>
            <tr>
                <th>Mã Ticket</th>
                <th>Khách Hàng</th>
                <th>Điện Thoại</th>
                <th>Dòng Xe Quan Tâm</th>
                <th>Phân Loại</th>
                <th>Trạng Thái</th>
                <th>Nhân Viên Phụ Trách</th>
            </tr>
        </thead>
        <tbody id="crmBody"></tbody>
    </table>

    <script>
        async function loadCrm() {
            const res = await fetch('/api/crm/admin/tickets');
            const data = await res.json();
            const list = data.data || [];
            document.getElementById('crmBody').innerHTML = list.map(t => `
                <tr>
                    <td><strong>${t.ticketCode}</strong></td>
                    <td>${t.customerName}</td>
                    <td>${t.phone}</td>
                    <td style="color:#38bdf8;">${t.interestCar || 'Chưa chọn'}</td>
                    <td>${t.inquiryType}</td>
                    <td><span class="badge-status badge-${t.status}">${t.status}</span></td>
                    <td>${t.assignedStaff || 'Chưa phân công'}</td>
                </tr>
            `).join('');
        }
        loadCrm();
    </script>
</body>
</html>
'@ | Set-Content (Join-Path $d19Tpl "admin-crm.html") -Encoding UTF8

# --- DAY 20: admin-marketing.html ---
$d20 = Create-PayloadDir 20
$d20Tpl = Join-Path $d20 "src\main\resources\templates"
New-Item -ItemType Directory -Force -Path $d20Tpl | Out-Null

@'
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Quản Lý Chiến Dịch Marketing & Ưu Đãi - Ares</title>
    <style>
        body { background: #090d16; color: #f1f5f9; font-family: 'Segoe UI', sans-serif; padding: 25px; }
        .grid-marketing { display: grid; grid-template-columns: 1fr 1fr; gap: 25px; }
        .card-box { background: #1e293b; padding: 20px; border-radius: 14px; border: 1px solid #334155; }
        .form-group { margin-bottom: 14px; }
        .form-group label { display: block; margin-bottom: 6px; color: #94a3b8; font-size: 0.9rem; }
        .form-input { width: 100%; box-sizing: border-box; background: #0f172a; border: 1px solid #475569; color: #fff; padding: 10px; border-radius: 8px; }
        .btn-broadcast { background: #0284c7; color: #fff; border: none; padding: 12px 20px; border-radius: 8px; cursor: pointer; font-weight: 700; }
        .broadcast-item { padding: 12px; background: #0f172a; border-radius: 10px; margin-bottom: 10px; border: 1px solid #334155; }
    </style>
</head>
<body>
    <h2>TRUNG TÂM TIẾP THỊ & CHIẾN DỊCH KHÁCH HÀNG</h2>
    <div class="grid-marketing">
        <div class="card-box">
            <h3 style="color:#38bdf8;margin-top:0;">Phát thông báo Broadcast toàn hệ thống</h3>
            <div class="form-group">
                <label>Tiêu đề thông báo</label>
                <input type="text" id="bcTitle" class="form-input" placeholder="Ưu đãi bảo dưỡng tháng này...">
            </div>
            <div class="form-group">
                <label>Nội dung chi tiết</label>
                <textarea id="bcMsg" class="form-input" rows="4" placeholder="Giảm 20% phí thay dầu cho khách hàng VIP..."></textarea>
            </div>
            <button class="btn-broadcast" onclick="sendBroadcast()">PHÁT THÔNG BÁO</button>
        </div>

        <div class="card-box">
            <h3 style="color:#38bdf8;margin-top:0;">Lịch sử thông báo gần đây</h3>
            <div id="bcList">Đang tải...</div>
        </div>
    </div>

    <script>
        async function loadBroadcasts() {
            const res = await fetch('/api/marketing/broadcasts');
            const data = await res.json();
            const list = data.data || [];
            document.getElementById('bcList').innerHTML = list.map(b => `
                <div class="broadcast-item">
                    <strong style="color:#38bdf8;">${b.title}</strong>
                    <p style="margin:5px 0 0 0;font-size:0.9rem;color:#cbd5e1;">${b.message}</p>
                </div>
            `).join('');
        }

        async function sendBroadcast() {
            const title = document.getElementById('bcTitle').value;
            const message = document.getElementById('bcMsg').value;
            if (!title || !message) { alert('Vui lòng điền đủ tiêu đề và nội dung'); return; }
            await fetch('/api/marketing/admin/broadcast', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ title, message, target: 'ALL' })
            });
            alert('Phát thông báo thành công!');
            document.getElementById('bcTitle').value = '';
            document.getElementById('bcMsg').value = '';
            loadBroadcasts();
        }

        loadBroadcasts();
    </script>
</body>
</html>
'@ | Set-Content (Join-Path $d20Tpl "admin-marketing.html") -Encoding UTF8

Write-Host "Setup Days 17-20 completed."
