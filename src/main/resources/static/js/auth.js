/**
 * Ares Store Authentication JavaScript
 * Handles Countdown timers (110s, 119s, 60s), OTP / Gmail verification code generation,
 * Rate-limiting prevention, form validation, and step transitions for Forgot Password,
 * Register, and Upgraded Login (Email/Username + Phone OTP + Social OAuth).
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

  // ========================================================================
  // 1. REGISTER PAGE
  // ========================================================================
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

  // ========================================================================
  // 2. FORGOT PASSWORD PAGE (Step 1 -> Step 2)
  // ========================================================================
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

  // ========================================================================
  // 3. LOGIN PAGE LOGIC (PORTED & ENHANCED FROM CAR-COPY)
  // ========================================================================
  const emailLoginPane = document.getElementById('emailLoginPane');
  const phoneLoginPane = document.getElementById('phoneLoginPane');
  const btnSwitchToPhone = document.getElementById('btnSwitchToPhone');
  const btnSwitchToEmail = document.getElementById('btnSwitchToEmail');
  const btnSendPhoneOtp = document.getElementById('btnSendPhoneOtp');
  const btnVerifyPhoneOtp = document.getElementById('btnVerifyPhoneOtp');
  const btnChangePhone = document.getElementById('btnChangePhoneNumber');
  const btnResendPhoneOtp = document.getElementById('btnResendPhoneOtp');
  const phoneStep1 = document.getElementById('phoneStep1');
  const phoneStep2 = document.getElementById('phoneStep2');
  const plPhoneInput = document.getElementById('plPhone');
  const plCodeInput = document.getElementById('plCode');
  const maskedPhoneDisplay = document.getElementById('maskedPhoneDisplay');
  const otpCooldownVal = document.getElementById('otpCooldownVal');
  const clientErrorAlert = document.getElementById('clientErrorAlert');
  const clientErrorMessage = document.getElementById('clientErrorMessage');
  const loginForm = document.getElementById('loginForm');

  function showClientError(msg) {
    if (clientErrorAlert && clientErrorMessage) {
      clientErrorMessage.textContent = msg;
      clientErrorAlert.style.display = 'flex';
    } else {
      showToast({ title: 'Lỗi', message: msg, type: 'error' });
    }
  }

  function clearClientError() {
    if (clientErrorAlert) {
      clientErrorAlert.style.display = 'none';
    }
  }

  // Switch between Email and Phone OTP modes
  if (btnSwitchToPhone && btnSwitchToEmail && emailLoginPane && phoneLoginPane) {
    btnSwitchToPhone.addEventListener('click', () => {
      clearClientError();
      emailLoginPane.classList.remove('active');
      phoneLoginPane.classList.add('active');
      if (plPhoneInput) plPhoneInput.focus();
    });

    btnSwitchToEmail.addEventListener('click', () => {
      clearClientError();
      phoneLoginPane.classList.remove('active');
      emailLoginPane.classList.add('active');
      const uInput = document.getElementById('username');
      if (uInput) uInput.focus();
    });
  }

  // Phone OTP Cooldown Timer (60s)
  let phoneCooldownTimerId = null;
  function startPhoneCooldown(seconds = 60) {
    if (phoneCooldownTimerId) clearInterval(phoneCooldownTimerId);
    let remaining = seconds;
    if (btnResendPhoneOtp) {
      btnResendPhoneOtp.disabled = true;
      if (otpCooldownVal) otpCooldownVal.textContent = `${remaining}s`;
    }
    phoneCooldownTimerId = setInterval(() => {
      remaining--;
      if (remaining <= 0) {
        clearInterval(phoneCooldownTimerId);
        phoneCooldownTimerId = null;
        if (btnResendPhoneOtp) {
          btnResendPhoneOtp.disabled = false;
          btnResendPhoneOtp.textContent = 'Gửi lại mã';
        }
      } else {
        if (otpCooldownVal) otpCooldownVal.textContent = `${remaining}s`;
      }
    }, 1000);
  }

  // Mask phone helper (e.g. 0988888888 -> 098****888)
  function maskPhone(p) {
    if (!p || p.length < 7) return p;
    const start = p.slice(0, 3);
    const end = p.slice(-3);
    return `${start}****${end}`;
  }

  // Send OTP
  async function handleSendPhoneOtp() {
    clearClientError();
    const phone = plPhoneInput ? plPhoneInput.value.trim() : '';
    if (!phone || phone.length < 8) {
      showClientError('Vui lòng nhập số điện thoại hợp lệ (tối thiểu 9-10 chữ số)!');
      if (plPhoneInput) plPhoneInput.focus();
      return;
    }

    if (btnSendPhoneOtp) {
      btnSendPhoneOtp.disabled = true;
      btnSendPhoneOtp.textContent = 'Đang gửi mã...';
    }

    try {
      const res = await fetch('/api/auth/send-code', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ target: phone, isEmail: false })
      });
      const data = await res.json();

      if (res.ok && data.success) {
        if (phoneStep1 && phoneStep2) {
          phoneStep1.style.display = 'none';
          phoneStep2.style.display = 'block';
        }
        if (maskedPhoneDisplay) {
          maskedPhoneDisplay.textContent = maskPhone(phone);
        }
        startPhoneCooldown(60);
        showToast({
          title: 'Đã gửi mã OTP',
          message: data.message || `Mã OTP đã được gửi tới số ${maskPhone(phone)}`,
          type: 'info'
        });
        if (plCodeInput) {
          plCodeInput.value = '';
          plCodeInput.focus();
        }
      } else {
        showClientError(data.message || 'Không thể gửi mã OTP. Vui lòng thử lại sau.');
      }
    } catch (err) {
      console.error('Send OTP error:', err);
      showClientError('Lỗi kết nối máy chủ khi gửi OTP. Vui lòng thử lại!');
    } finally {
      if (btnSendPhoneOtp) {
        btnSendPhoneOtp.disabled = false;
        btnSendPhoneOtp.textContent = 'Gửi mã OTP';
      }
    }
  }

  if (btnSendPhoneOtp) {
    btnSendPhoneOtp.addEventListener('click', handleSendPhoneOtp);
  }

  if (btnResendPhoneOtp) {
    btnResendPhoneOtp.addEventListener('click', (e) => {
      e.preventDefault();
      handleSendPhoneOtp();
    });
  }

  // Change phone number
  if (btnChangePhone) {
    btnChangePhone.addEventListener('click', () => {
      clearClientError();
      if (phoneCooldownTimerId) {
        clearInterval(phoneCooldownTimerId);
        phoneCooldownTimerId = null;
      }
      if (phoneStep1 && phoneStep2) {
        phoneStep2.style.display = 'none';
        phoneStep1.style.display = 'block';
      }
      if (plPhoneInput) plPhoneInput.focus();
    });
  }

  // Verify Phone OTP & Login
  async function handleVerifyPhoneOtp() {
    clearClientError();
    const phone = plPhoneInput ? plPhoneInput.value.trim() : '';
    const code = plCodeInput ? plCodeInput.value.trim() : '';

    if (!phone) {
      showClientError('Thiếu số điện thoại!');
      return;
    }
    if (!code || code.length !== 6) {
      showClientError('Vui lòng nhập đủ 6 chữ số mã OTP!');
      if (plCodeInput) plCodeInput.focus();
      return;
    }

    if (btnVerifyPhoneOtp) {
      btnVerifyPhoneOtp.disabled = true;
      btnVerifyPhoneOtp.textContent = 'Đang xác thực...';
    }

    try {
      const res = await fetch('/api/auth/phone-login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone: phone, code: code })
      });
      const data = await res.json();

      if (res.ok && data.success) {
        showToast({
          title: 'Đăng nhập thành công',
          message: 'Chào mừng bạn đến với Ares Store!',
          type: 'success'
        });
        setTimeout(() => {
          window.location.href = '/home';
        }, 500);
      } else {
        showClientError(data.message || 'Mã OTP không đúng hoặc đã hết hạn!');
        if (plCodeInput) {
          plCodeInput.select();
          plCodeInput.focus();
        }
      }
    } catch (err) {
      console.error('Verify phone login error:', err);
      showClientError('Lỗi kết nối máy chủ khi xác thực OTP!');
    } finally {
      if (btnVerifyPhoneOtp) {
        btnVerifyPhoneOtp.disabled = false;
        btnVerifyPhoneOtp.textContent = 'Xác nhận & đăng nhập';
      }
    }
  }

  if (btnVerifyPhoneOtp) {
    btnVerifyPhoneOtp.addEventListener('click', handleVerifyPhoneOtp);
  }

  if (plCodeInput) {
    plCodeInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        handleVerifyPhoneOtp();
      }
    });
  }

  if (plPhoneInput) {
    plPhoneInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        handleSendPhoneOtp();
      }
    });
  }

  // AJAX Login submission for Email/Username form with graceful fallback
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      clearClientError();
      const usernameInput = document.getElementById('username');
      const passwordInput = document.getElementById('password');
      const btnEmailLogin = document.getElementById('btnEmailLogin');

      const username = usernameInput ? usernameInput.value.trim() : '';
      const password = passwordInput ? passwordInput.value : '';

      if (!username || !password) return; // browser validation handles this

      e.preventDefault();
      if (btnEmailLogin) {
        btnEmailLogin.disabled = true;
        btnEmailLogin.textContent = 'Đang đăng nhập...';
      }

      try {
        const res = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: username, password: password })
        });
        const data = await res.json();

        if (res.ok && data.success) {
          showToast({
            title: 'Đăng nhập thành công',
            message: 'Đang chuyển hướng về trang chủ...',
            type: 'success'
          });
          setTimeout(() => {
            window.location.href = '/home';
          }, 400);
        } else {
          showClientError(data.message || 'Tên đăng nhập hoặc mật khẩu không chính xác!');
          if (btnEmailLogin) {
            btnEmailLogin.disabled = false;
            btnEmailLogin.textContent = 'Đăng nhập';
          }
        }
      } catch (err) {
        console.warn('API login error, falling back to standard POST:', err);
        loginForm.submit();
      }
    });
  }
});
