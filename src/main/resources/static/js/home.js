/**
 * Ares Store - Concept 2 Frontend REST API Integration
 * Kết nối toàn bộ giao diện Trang chủ tới Backend REST APIs
 */

let cartData = { totalQuantity: 0, formattedTotalPrice: '0₫', items: [] };
let currentModalCarId = 1;
let currentModalQty = 1;

document.addEventListener('DOMContentLoaded', () => {
  // 1. Tải dữ liệu toàn bộ trang chủ qua REST API
  loadCurrentUserApi();
  loadCartApi();
  loadPromotionsApi();
  loadCategoriesApi();
  loadCarsSectionApi('BEST_SELLER', 'bestSellersGrid');
  loadCarsSectionApi('RC_HIGHLIGHTS', 'rcHighlightsGrid');
  loadCarsSectionApi('MODEL_HIGHLIGHTS', 'modelHighlightsGrid');
  loadReviewsApi();

  // 2. Khởi tạo sự kiện giao diện
  initNavEvents();
  initSearchEvents();
  initCartModalEvents();
});

/* ==========================================================================
   1. AUTH API: KIỂM TRA ĐĂNG NHẬP (GET /api/auth/me)
   ========================================================================== */
async function loadCurrentUserApi() {
  const container = document.getElementById('userAuthSection');
  if (!container) return;

  try {
    const res = await fetch('/api/auth/me', { credentials: 'include' });
    if (res.ok) {
      const json = await res.json();
      if (json.data && json.data.username) {
        container.innerHTML = `
          <div class="user-logged-badge" title="Đã đăng nhập: ${json.data.username}">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <span>${json.data.username}</span>
            <a href="javascript:void(0)" class="user-logout-link" onclick="logoutUserApi()">(Thoát)</a>
          </div>
        `;
        return;
      }
    }
  } catch (err) {
    console.warn('Auth API /me check failed:', err);
  }

  // Mặc định: Nút đăng nhập
  container.innerHTML = `
    <a href="/login" class="account-btn" title="Đăng nhập tài khoản">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
        <circle cx="12" cy="7" r="4"></circle>
      </svg>
    </a>
  `;
}

// Đăng xuất qua API POST /api/auth/logout
window.logoutUserApi = async function() {
  try {
    await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
    window.location.reload();
  } catch (err) {
    window.location.href = '/logout';
  }
};

/* ==========================================================================
   2. PROMOTIONS API (GET /api/promotions)
   ========================================================================== */
async function loadPromotionsApi() {
  try {
    const res = await fetch('/api/promotions');
    if (!res.ok) return;
    const json = await res.json();
    const data = json.data;
    if (!data) return;

    const promoImg = document.getElementById('promoBannerImg');
    if (promoImg && data.saleBanner) {
      promoImg.src = data.saleBanner;
    }

    const heroImg = document.getElementById('heroBannerImg');
    if (heroImg && data.heroBanner) {
      heroImg.src = data.heroBanner;
    }
  } catch (e) {
    console.warn('Promotions API fallback to defaults:', e);
  }
}

/* ==========================================================================
   3. CATEGORIES API (GET /api/categories)
   ========================================================================== */
async function loadCategoriesApi() {
  const container = document.getElementById('categoriesContainer');
  const megaContainer = document.getElementById('megaPanelCards');
  if (!container) return;

  try {
    const res = await fetch('/api/categories');
    if (!res.ok) return;
    const json = await res.json();
    const categories = json.data;
    if (!Array.isArray(categories) || !categories.length) return;

    // Icon map tương ứng đúng chuẩn concept
    const iconMap = {
      'RC_CARS': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="6" width="20" height="12" rx="4"></rect><path d="M6 12h4m-2-2v4"></path><circle cx="15" cy="11" r="1"></circle><circle cx="17" cy="13" r="1"></circle></svg>`,
      'MODEL_CARS': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l2.4 7.4H22l-6.2 4.5 2.4 7.4-6.2-4.5-6.2 4.5 2.4-7.4L2 9.4h7.6z"></path></svg>`,
      'BATTERIES': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="16" height="10" rx="2"></rect><line x1="20" y1="11" x2="20" y2="13"></line><line x1="6" y1="11" x2="6" y2="13"></line><line x1="10" y1="11" x2="10" y2="13"></line></svg>`,
      'CONTROLLERS': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"></circle><path d="M12 8v8m-4-4h8"></path></svg>`,
      'OTHER_PARTS': `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"></path></svg>`
    };

    container.innerHTML = categories.map((cat, idx) => `
      <button class="cat-rect-btn ${idx === 0 ? 'active' : ''}" data-cat="${cat.id}" onclick="filterCategoryApi('${cat.id}', this)">
        <span>${cat.name}</span>
        ${iconMap[cat.id] || iconMap['RC_CARS']}
      </button>
    `).join('');

    // Nạp mega panel nếu có
    if (megaContainer) {
      megaContainer.innerHTML = `
        <a href="#modelHighlightsSection" class="cat-preview-card" onclick="document.getElementById('navHomeTab').click()">
          <div class="cat-preview-thumb"><img src="/images/cars/model-1.jpg" alt="Xe Model"></div>
          <div class="cat-preview-name">Xe Model</div>
        </a>
        <a href="#rcHighlightsSection" class="cat-preview-card" onclick="document.getElementById('navHomeTab').click()">
          <div class="cat-preview-thumb"><img src="/images/cars/rc-1.jpg" alt="Xe RC"></div>
          <div class="cat-preview-name">Xe RC</div>
        </a>
        <a href="#bestSellersSection" class="cat-preview-card" onclick="document.getElementById('navHomeTab').click()">
          <div class="cat-preview-thumb"><img src="/images/cars/bestseller-2.jpg" alt="Xe Trưng Bày"></div>
          <div class="cat-preview-name">Xe Trưng Bày</div>
        </a>
      `;
    }
  } catch (e) {
    console.warn('Categories API failed:', e);
  }
}

// Lọc hoặc scroll khi click Category
window.filterCategoryApi = async function(catId, btnEl) {
  document.querySelectorAll('.cat-rect-btn').forEach(b => b.classList.remove('active'));
  if (btnEl) btnEl.classList.add('active');

  if (catId === 'RC_CARS') {
    const el = document.getElementById('rcHighlightsSection');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  } else if (catId === 'MODEL_CARS') {
    const el = document.getElementById('modelHighlightsSection');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  } else {
    // Gọi API lọc xe
    try {
      const res = await fetch(`/api/cars?category=${encodeURIComponent(catId)}`);
      const json = await res.json();
      if (json.data && json.data.length) {
        alert(`Danh mục ${catId}: Tìm thấy ${json.data.length} mẫu xe.`);
      }
    } catch (e) {}
  }
};

/* ==========================================================================
   4. CARS API (GET /api/cars?section=...)
   ========================================================================== */
async function loadCarsSectionApi(section, containerId) {
  const container = document.getElementById(containerId);
  if (!container) return;

  try {
    const res = await fetch(`/api/cars?section=${encodeURIComponent(section)}`);
    if (!res.ok) return;
    const json = await res.json();
    const cars = json.data;
    if (!Array.isArray(cars) || !cars.length) return;

    container.innerHTML = cars.map(car => `
      <div class="concept-car-card" onclick="openCarModalFromApi(${car.id})" title="${car.name}">
        <img src="${car.image}" alt="${car.name}" class="concept-car-img" onerror="this.src='/images/cars/bestseller-1.jpg'">
      </div>
    `).join('');
  } catch (err) {
    console.warn(`Cars API failed for section ${section}:`, err);
  }
}

/* ==========================================================================
   5. REVIEWS API (GET /api/reviews)
   ========================================================================== */
async function loadReviewsApi() {
  const container = document.getElementById('reviewsGrid');
  if (!container) return;

  try {
    const res = await fetch('/api/reviews');
    if (!res.ok) return;
    const json = await res.json();
    const reviews = json.data;
    if (!Array.isArray(reviews) || !reviews.length) return;

    container.innerHTML = reviews.map(rev => `
      <div class="concept-review-card">
        <div class="concept-review-photo">
          <img src="${rev.carImage}" alt="Review car photo" onerror="this.src='/images/cars/review-1.jpg'">
        </div>
        <div class="concept-review-content">
          <div class="concept-review-header">
            <span class="concept-reviewer-name">${rev.username}</span>
            <span class="concept-reviewer-stars">${'★'.repeat(rev.rating || 5)}</span>
          </div>
          <p class="concept-review-text">
            "${rev.comment}"
          </p>
          <div class="concept-review-date">
            ${rev.purchaseDate || 'Verified Purchase'}
          </div>
        </div>
      </div>
    `).join('');
  } catch (e) {
    console.warn('Reviews API failed:', e);
  }
}

/* ==========================================================================
   6. CART API: GET / POST / DELETE (/api/cart)
   ========================================================================== */
async function loadCartApi() {
  try {
    const res = await fetch('/api/cart', { credentials: 'include' });
    if (res.ok) {
      const json = await res.json();
      if (json.data) {
        cartData = json.data;
        updateCartBadge();
      }
    }
  } catch (e) {
    console.warn('Cart API failed:', e);
  }
}

function updateCartBadge() {
  const badge = document.getElementById('cartCountDisplay');
  if (badge) {
    badge.textContent = `${cartData.totalQuantity || 0} in cart`;
  }
}

// Thêm xe vào giỏ hàng qua POST /api/cart/items
window.addToCartFromModal = async function() {
  try {
    const res = await fetch('/api/cart/items', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ carId: currentModalCarId, quantity: currentModalQty })
    });
    const json = await res.json();
    if (res.ok && json.data) {
      cartData = json.data;
      updateCartBadge();
      closeCarModal();
      alert(`🛒 Đã thêm thành công vào giỏ hàng! Hiện có ${cartData.totalQuantity} sản phẩm.`);
    } else {
      cartData.totalQuantity += currentModalQty;
      updateCartBadge();
      closeCarModal();
    }
  } catch (err) {
    cartData.totalQuantity += currentModalQty;
    updateCartBadge();
    closeCarModal();
  }
};

// Làm trống giỏ hàng qua DELETE /api/cart
window.clearCartApi = async function() {
  try {
    const res = await fetch('/api/cart', { method: 'DELETE', credentials: 'include' });
    if (res.ok) {
      const json = await res.json();
      cartData = json.data || { totalQuantity: 0, formattedTotalPrice: '0₫', items: [] };
      updateCartBadge();
      renderCartModalList();
    }
  } catch (e) {
    alert('Lỗi khi làm trống giỏ hàng.');
  }
};

// Xóa 1 món hàng qua DELETE /api/cart/items/{id}
window.removeCartItemApi = async function(carId) {
  try {
    const res = await fetch(`/api/cart/items/${carId}`, { method: 'DELETE', credentials: 'include' });
    if (res.ok) {
      const json = await res.json();
      cartData = json.data;
      updateCartBadge();
      renderCartModalList();
    }
  } catch (e) {
    console.warn('Delete item failed:', e);
  }
};

/* ==========================================================================
   7. MODAL CHI TIẾT SẢN PHẨM: GET /api/cars/{id}
   ========================================================================== */
window.openCarModalFromApi = async function(carId) {
  currentModalCarId = carId;
  currentModalQty = 1;
  const qtyEl = document.getElementById('modalQtyDisplay');
  if (qtyEl) qtyEl.textContent = '1';

  try {
    const res = await fetch(`/api/cars/${carId}`);
    if (res.ok) {
      const json = await res.json();
      const car = json.data;
      if (car) {
        document.getElementById('modalCarTitle').textContent = car.name;
        document.getElementById('modalCarPrice').textContent = car.formattedPrice || 'Liên hệ';
        document.getElementById('modalCarPriceOld').textContent = car.formattedOldPrice || '';
        document.getElementById('modalDiscount').textContent = `-${car.discountPercent || 20}% OFF`;
        document.getElementById('modalCarImg').src = car.image;
        document.getElementById('modalSpecScale').textContent = car.scale || '1:18';
        document.getElementById('modalSpecMaterial').textContent = car.material || 'Hợp kim cao cấp';
        document.getElementById('modalSpecFeature').textContent = car.features || 'Chi tiết cao cấp';
        document.getElementById('modalCarDesc').textContent = car.description || 'Mô hình chính hãng Ares Store.';
        document.getElementById('modalCategory').textContent = car.category === 'RC_CARS' ? 'RC Cars • Xe Điều Khiển' : 'Model Cars • Mô Hình Tĩnh';
        document.getElementById('modalBadge').textContent = car.category === 'RC_CARS' ? 'Ares RC Spec' : 'Ares Diecast 1:18';
      }
    }
  } catch (e) {
    console.warn('Car detail API error:', e);
  }

  const modal = document.getElementById('productModal');
  if (modal) {
    modal.style.display = 'flex';
    setTimeout(() => modal.classList.add('active'), 10);
    document.body.style.overflow = 'hidden';
  }
};

window.closeCarModal = function() {
  const modal = document.getElementById('productModal');
  if (modal) {
    modal.classList.remove('active');
    setTimeout(() => modal.style.display = 'none', 250);
    document.body.style.overflow = '';
  }
};

window.changeModalQty = function(delta) {
  currentModalQty = Math.max(1, currentModalQty + delta);
  const qtyEl = document.getElementById('modalQtyDisplay');
  if (qtyEl) qtyEl.textContent = currentModalQty;
};

/* ==========================================================================
   8. GIỎ HÀNG MODAL
   ========================================================================== */
function initCartModalEvents() {
  const cartBtn = document.getElementById('cartOpenBtn');
  if (cartBtn) {
    cartBtn.addEventListener('click', () => {
      openCartModal();
    });
  }
}

window.openCartModal = async function() {
  await loadCartApi();
  renderCartModalList();
  const modal = document.getElementById('cartModal');
  if (modal) {
    modal.style.display = 'flex';
    setTimeout(() => modal.classList.add('active'), 10);
    document.body.style.overflow = 'hidden';
  }
};

window.closeCartModal = function() {
  const modal = document.getElementById('cartModal');
  if (modal) {
    modal.classList.remove('active');
    setTimeout(() => modal.style.display = 'none', 250);
    document.body.style.overflow = '';
  }
};

function renderCartModalList() {
  const listEl = document.getElementById('cartItemsList');
  const totalEl = document.getElementById('cartTotalPrice');
  if (!listEl) return;

  if (totalEl) totalEl.textContent = cartData.formattedTotalPrice || '0₫';

  if (!cartData.items || !cartData.items.length) {
    listEl.innerHTML = `<div style="text-align: center; color: #64748b; padding: 20px;">Giỏ hàng của bạn đang trống.</div>`;
    return;
  }

  listEl.innerHTML = cartData.items.map(it => `
    <div class="cart-item-row">
      <img src="${it.image}" alt="${it.name}" class="cart-item-img">
      <div class="cart-item-details">
        <div class="cart-item-name">${it.name}</div>
        <div class="cart-item-sub">Số lượng: ${it.quantity} x ${it.formattedPrice}</div>
      </div>
      <button class="cart-item-del-btn" onclick="removeCartItemApi(${it.carId})" title="Xóa">&times;</button>
    </div>
  `).join('');
}

/* ==========================================================================
   9. TÌM KIẾM API & TABS NAVIGATION
   ========================================================================== */
function initNavEvents() {
  const homeTab = document.getElementById('navHomeTab');
  const catTab = document.getElementById('navCategoryTab');
  const bannerCard = document.getElementById('huracanBannerCard');
  const megaPanel = document.getElementById('categoryMegaPanel');
  const dotsRow = document.getElementById('heroDotsRow');

  if (homeTab && catTab) {
    homeTab.addEventListener('click', () => {
      homeTab.classList.add('active');
      catTab.classList.remove('active');
      if (bannerCard) bannerCard.style.display = 'block';
      if (megaPanel) megaPanel.style.display = 'none';
      if (dotsRow) dotsRow.style.display = 'flex';
    });

    catTab.addEventListener('click', () => {
      catTab.classList.add('active');
      homeTab.classList.remove('active');
      if (bannerCard) bannerCard.style.display = 'none';
      if (megaPanel) megaPanel.style.display = 'flex';
      if (dotsRow) dotsRow.style.display = 'none';
    });
  }
}

function initSearchEvents() {
  const toggleBtn = document.getElementById('searchToggleBtn');
  const searchBar = document.getElementById('searchApiBar');
  const searchInput = document.getElementById('searchApiInput');
  const searchBtn = document.getElementById('searchApiBtn');
  const closeBtn = document.getElementById('searchCloseBtn');

  if (toggleBtn && searchBar) {
    toggleBtn.addEventListener('click', () => {
      searchBar.style.display = searchBar.style.display === 'none' ? 'flex' : 'none';
      if (searchBar.style.display === 'flex' && searchInput) {
        searchInput.focus();
      }
    });

    if (closeBtn) {
      closeBtn.addEventListener('click', () => {
        searchBar.style.display = 'none';
      });
    }

    if (searchBtn && searchInput) {
      const executeSearch = async () => {
        const q = searchInput.value.trim();
        if (!q) return;
        try {
          const res = await fetch(`/api/cars?search=${encodeURIComponent(q)}`);
          const json = await res.json();
          const cars = json.data;
          if (cars && cars.length > 0) {
            openCarModalFromApi(cars[0].id);
          } else {
            alert(`Không tìm thấy siêu xe nào với từ khóa: "${q}"`);
          }
        } catch (e) {
          console.warn('Search API failed:', e);
        }
      };

      searchBtn.addEventListener('click', executeSearch);
      searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') executeSearch();
      });
    }
  }
}
