/**
 * Ares Store Authentication JavaScript
 * Handles Countdown timers (110s, 119s), OTP / Gmail verification code generation,
 * Rate-limiting prevention, form validation, and step transitions for Forgot Password.
 * 
 * Note: Mỗi lần load lại trang, nút gửi mã luôn sẵn sàng (không lưu countdown qua reload).
 * Không hiển thị popup thông báo gửi thành công (âm thầm bắt đầu đếm ngược).
 */

// Toast notification helper for warnings and errors
function showToast({ title, message, type = 'info', duration = 5000 }) {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = 'toast';

  const iconMap = {
    info: '📩',
    success: '✅',
    warning: '⚠️',
    error: '❌'
  };

  toast.innerHTML = `
    <div class="toast-icon">${iconMap[type] || '🔔'}</div>
    <div class="toast-content">
      <div class="toast-title">${title}</div>
      <div class="toast-message">${message}</div>
    </div>
    <button class="toast-close" onclick="this.parentElement.remove()">&times;</button>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add('toast-hiding');
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

// Toggle password visibility (show/hide)
function togglePasswordVisibility(inputId, btn) {
  const input = document.getElementById(inputId);
  if (!input) return;

  const eyeSlash = btn.querySelector('.eye-slash');
  const eyeOpen = btn.querySelector('.eye-open');

  if (input.type === 'password') {
    input.type = 'text';
    if (eyeSlash) eyeSlash.style.display = 'none';
    if (eyeOpen) eyeOpen.style.display = 'block';
    btn.setAttribute('aria-label', 'Ẩn mật khẩu');
  } else {
    input.type = 'password';
    if (eyeSlash) eyeSlash.style.display = 'block';
    if (eyeOpen) eyeOpen.style.display = 'none';
    btn.setAttribute('aria-label', 'Hiện mật khẩu');
  }
}

// Timer Controller - Đếm ngược trong phiên hiện tại, reset ngay khi reload trang
class CountdownTimer {
  constructor(buttonId, indicatorId, initialSeconds) {
    this.btn = document.getElementById(buttonId);
    this.indicator = document.getElementById(indicatorId);
    this.initialSeconds = initialSeconds;
    this.remaining = initialSeconds;
    this.timerId = null;

    // Reset về trạng thái ban đầu khi tải/load lại trang
    this.stop();
  }

  start(customSeconds = null) {
    if (this.timerId) clearInterval(this.timerId);
    this.remaining = customSeconds !== null ? customSeconds : this.initialSeconds;

    this.updateUI();
    if (this.btn) {
      this.btn.disabled = true;
      this.btn.textContent = 'Gửi lại';
    }

    this.timerId = setInterval(() => {
      this.remaining--;
      if (this.remaining <= 0) {
        this.stop();
      } else {
        this.updateUI();
      }
    }, 1000);
  }

  updateUI() {
    if (this.indicator) {
      this.indicator.textContent = `${this.remaining}s`;
      this.indicator.style.display = 'block';
    }
  }

  stop() {
    if (this.timerId) clearInterval(this.timerId);
    this.timerId = null;
    if (this.indicator) {
      this.indicator.textContent = '';
      this.indicator.style.display = 'none';
    }
    if (this.btn) {
      this.btn.disabled = false;
      this.btn.textContent = 'Gửi lại';
    }
  }
}

// Global flag to prevent concurrent multiple clicks in the same page
let isCodeRequestPending = false;

// Request OTP / Code via REST API
async function sendVerificationCode(targetInputId, timerInstance, defaultTarget = '') {
  if (isCodeRequestPending) {
    return;
  }

  const inputEl = document.getElementById(targetInputId);
  const target = inputEl ? inputEl.value.trim() : defaultTarget;

  if (!target) {
    showToast({
      title: 'Thiếu thông tin',
      message: 'Vui lòng nhập Email hoặc Số điện thoại trước khi nhận mã!',
      type: 'warning'
    });
    if (inputEl) inputEl.focus();
    return;
  }

  // Khóa nút khi đang gửi để tránh click nhiều lần liên tiếp
  isCodeRequestPending = true;
  const btn = timerInstance ? timerInstance.btn : null;
  const originalBtnText = btn ? btn.textContent : 'Gửi lại';
  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Đang gửi...';
  }

  const isEmail = target.includes('@');

  try {
    const response = await fetch('/api/auth/send-code', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ target: target, isEmail: isEmail })
    });

    const data = await response.json();

    if (response.ok && data.success) {
      // Đã gửi thành công: kích hoạt đếm ngược, KHÔNG hiển thị toast popup làm phiền
      if (timerInstance) timerInstance.start();
    } else {
      // Xử lý khi có lỗi hoặc cảnh báo
      if (timerInstance) timerInstance.stop();
      showToast({
        title: 'Chưa thể gửi mã',
        message: data.message || 'Không thể gửi mã xác nhận. Vui lòng thử lại sau.',
        type: 'warning'
      });
    }
  } catch (err) {
    console.error('Send code error:', err);
    if (timerInstance) timerInstance.stop();
    showToast({
      title: 'Lỗi kết nối',
      message: 'Không thể kết nối đến máy chủ. Hãy thử lại.',
      type: 'error'
    });
  } finally {
    isCodeRequestPending = false;
    if (btn && !btn.disabled) {
      btn.textContent = originalBtnText;
    }
  }
}

// Initialize components when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  // Xoá bỏ các khoá session cũ nếu có
  sessionStorage.removeItem('ares_cooldown_register');
  sessionStorage.removeItem('ares_cooldown_forgot');

  // 0. LOGIN FORM (API POST /api/auth/login)
  const loginForm = document.getElementById('loginForm');
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const usernameInput = document.getElementById('username');
      const passwordInput = document.getElementById('password');
      const username = usernameInput ? usernameInput.value.trim() : '';
      const password = passwordInput ? passwordInput.value : '';

      if (!username || !password) {
        showToast({
          title: 'Thiếu thông tin',
          message: 'Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu!',
          type: 'warning'
        });
        return;
      }

      const submitBtn = loginForm.querySelector('button[type="submit"]');
      const originalText = submitBtn ? submitBtn.textContent : 'Login';
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Đang đăng nhập...';
      }

      try {
        const res = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: username, password: password })
        });
        const resData = await res.json();

        if (res.ok && resData.success) {
          window.location.href = '/home';
        } else {
          showToast({
            title: 'Đăng nhập không thành công',
            message: resData.message || 'Tên đăng nhập hoặc mật khẩu không chính xác!',
            type: 'error'
          });
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
          }
        }
      } catch (err) {
        console.error('API login failed, fallback to form submit:', err);
        loginForm.submit();
      }
    });
  }

  // 1. REGISTER PAGE
  const registerForm = document.getElementById('registerForm');
  const btnResendRegister = document.getElementById('btnResendRegister');
  let regTimer = null;

  if (btnResendRegister) {
    regTimer = new CountdownTimer('btnResendRegister', 'registerTimerIndicator', 110);
    btnResendRegister.addEventListener('click', (e) => {
      e.preventDefault();
      sendVerificationCode('emailOrPhone', regTimer);
    });
  }

  if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const password = document.getElementById('password')?.value;
      const confirmPassword = document.getElementById('confirmPassword')?.value;
      const termsCheck = document.getElementById('termsAgreement');

      if (password !== confirmPassword) {
        showToast({
          title: 'Mật khẩu không khớp',
          message: 'Mật khẩu nhập lại phải trùng với mật khẩu đã chọn!',
          type: 'error'
        });
        return;
      }

      if (termsCheck && !termsCheck.checked) {
        showToast({
          title: 'Điều khoản dịch vụ',
          message: 'Vui lòng đánh dấu đồng ý với chính sách bảo mật và điều khoản dịch vụ!',
          type: 'warning'
        });
        return;
      }

      const formData = new FormData(registerForm);
      const payload = Object.fromEntries(formData.entries());

      try {
        const res = await fetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        const resData = await res.json();

        if (resData.success) {
          window.location.href = '/login?registered=true';
        } else {
          showToast({
            title: 'Đăng ký thất bại',
            message: resData.message || 'Vui lòng kiểm tra lại thông tin.',
            type: 'error'
          });
        }
      } catch (err) {
        console.error('Register submit error:', err);
        registerForm.submit();
      }
    });
  }

  // 2. FORGOT PASSWORD PAGE (Step 1 -> Step 2)
  const forgotFormStep1 = document.getElementById('forgotFormStep1');
  const forgotFormStep2 = document.getElementById('forgotFormStep2');
  const btnResendForgot = document.getElementById('btnResendForgot');
  let forgotTimer = null;

  if (btnResendForgot) {
    forgotTimer = new CountdownTimer('btnResendForgot', 'forgotTimerIndicator', 119);
    btnResendForgot.addEventListener('click', (e) => {
      e.preventDefault();
      sendVerificationCode('phoneOrEmail', forgotTimer);
    });
  }

  if (forgotFormStep1) {
    forgotFormStep1.addEventListener('submit', async (e) => {
      e.preventDefault();
      const phoneOrEmail = document.getElementById('phoneOrEmail')?.value.trim();
      const code = document.getElementById('forgotCode')?.value.trim();

      if (!phoneOrEmail || !code) {
        showToast({
          title: 'Thiếu thông tin',
          message: 'Vui lòng nhập đầy đủ Phone/Email và Mã xác nhận!',
          type: 'warning'
        });
        return;
      }

      try {
        const res = await fetch('/api/auth/verify-code', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ target: phoneOrEmail, code: code })
        });
        const resData = await res.json();

        if (resData.success) {
          // Transition to Step 2
          document.getElementById('step1-pane').classList.remove('active');
          document.getElementById('step2-pane').classList.add('active');
          document.getElementById('resetTarget').value = phoneOrEmail;
          document.getElementById('resetCode').value = code;
          document.getElementById('newPassword')?.focus();
        } else {
          showToast({
            title: 'Mã xác nhận không đúng',
            message: resData.message || 'Mã xác nhận không hợp lệ hoặc đã hết hạn. Vui lòng kiểm tra lại.',
            type: 'error'
          });
        }
      } catch (err) {
        console.error('Verify error:', err);
      }
    });
  }

  if (forgotFormStep2) {
    forgotFormStep2.addEventListener('submit', async (e) => {
      e.preventDefault();
      const newPassword = document.getElementById('newPassword')?.value;
      const confirmNewPassword = document.getElementById('confirmNewPassword')?.value;
      const resetTarget = document.getElementById('resetTarget')?.value;
      const resetCode = document.getElementById('resetCode')?.value;

      if (!newPassword || newPassword.length < 4) {
        showToast({
          title: 'Mật khẩu quá ngắn',
          message: 'Mật khẩu phải có ít nhất 4 ký tự.',
          type: 'warning'
        });
        return;
      }

      if (newPassword !== confirmNewPassword) {
        showToast({
          title: 'Mật khẩu không khớp',
          message: 'Mật khẩu xác minh phải trùng khớp với mật khẩu mới!',
          type: 'error'
        });
        return;
      }

      try {
        const res = await fetch('/api/auth/reset-password', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            target: resetTarget,
            code: resetCode,
            newPassword: newPassword
          })
        });
        const resData = await res.json();

        if (resData.success) {
          window.location.href = '/login?resetSuccess=true';
        } else {
          showToast({
            title: 'Lỗi đặt lại mật khẩu',
            message: resData.message || 'Không thể đặt lại mật khẩu.',
            type: 'error'
          });
        }
      } catch (err) {
        console.error('Reset submit error:', err);
      }
    });
  }
});
