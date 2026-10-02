/**
 * Ares Store - Home Page Script (Concept 2, Wireframe & Category Mega Panel)
 */

document.addEventListener('DOMContentLoaded', () => {
  initHeroSlider();
  initTopNavTabs();
  initCategories();
  initCartCounter();
});

/* ==========================================================================
   1. CHUYỂN ĐỔI TAB THANH MENU: HOME VS DANH MỤC (HÌNH 4)
   ========================================================================== */
function initTopNavTabs() {
  const homeTab = document.getElementById('navHomeTab');
  const catTab = document.getElementById('navCategoryTab');
  const heroCoverBox = document.getElementById('heroCoverBox');
  const catMegaPanel = document.getElementById('categoryMegaPanel');
  const heroDots = document.getElementById('heroDotsContainer');

  if (homeTab && catTab) {
    homeTab.addEventListener('click', () => {
      homeTab.classList.add('active');
      catTab.classList.remove('active');

      if (heroCoverBox) heroCoverBox.style.display = 'flex';
      if (catMegaPanel) catMegaPanel.style.display = 'none';
      if (heroDots) heroDots.style.display = 'flex';
    });

    catTab.addEventListener('click', () => {
      catTab.classList.add('active');
      homeTab.classList.remove('active');

      if (heroCoverBox) heroCoverBox.style.display = 'none';
      if (catMegaPanel) catMegaPanel.style.display = 'flex';
      if (heroDots) heroDots.style.display = 'none';
    });
  }
}

/* ==========================================================================
   2. HERO COVER SLIDER (5 CHẤM ĐIỀU HƯỚNG ĐƠN GIẢN, BỎ CHẤM PHÁT SÁNG)
   ========================================================================== */
function initHeroSlider() {
  const slides = document.querySelectorAll('.hero-car-slide');
  const dots = document.querySelectorAll('.simple-dot');
  if (!slides.length) return;

  let currentSlide = 0;
  let slideTimer = null;

  function setSlide(index) {
    slides.forEach((slide, i) => {
      slide.classList.toggle('active', i === index);
    });
    dots.forEach((dot, i) => {
      dot.classList.toggle('active', i === index);
    });
    currentSlide = index;
  }

  function nextSlide() {
    let next = (currentSlide + 1) % slides.length;
    setSlide(next);
  }

  dots.forEach((dot, i) => {
    dot.addEventListener('click', () => {
      setSlide(i);
      resetAutoSlide();
    });
  });

  function startAutoSlide() {
    slideTimer = setInterval(nextSlide, 4500);
  }

  function resetAutoSlide() {
    clearInterval(slideTimer);
    startAutoSlide();
  }

  const cover = document.getElementById('heroCoverBox');
  if (cover) {
    cover.addEventListener('mouseenter', () => clearInterval(slideTimer));
    cover.addEventListener('mouseleave', startAutoSlide);
  }

  startAutoSlide();
}

/* ==========================================================================
   3. CATEGORIES FILTER (CHỮ TRÁI, ICON PHẢI)
   ========================================================================== */
function initCategories() {
  const categoryBtns = document.querySelectorAll('.category-rect-btn');

  categoryBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
      categoryBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
    });
  });
}

/* ==========================================================================
   4. GIỎ HÀNG VÀ MODAL XEM NHANH (SHOWROOM LIGHTBOX)
   ========================================================================== */
let cartItemCount = 0;
let currentModalQty = 1;

function initCartCounter() {
  const cartBtn = document.getElementById('cartOpenBtn');
  if (cartBtn) {
    cartBtn.addEventListener('click', () => {
      alert(`Giỏ hàng của bạn đang có ${cartItemCount} sản phẩm.`);
    });
  }
}

function updateCartDisplay() {
  const el = document.getElementById('cartCountDisplay');
  if (el) {
    el.textContent = `${cartItemCount} in cart`;
  }
}

// Thay đổi số lượng mua trong modal
window.changeModalQty = function(delta) {
  currentModalQty = Math.max(1, currentModalQty + delta);
  const qtyEl = document.getElementById('modalQtyDisplay');
  if (qtyEl) qtyEl.textContent = currentModalQty;
};

// Mở modal thông tin xe
window.openCarModal = function(title, price, img, scale, material, feature, category) {
  const modal = document.getElementById('productModal');
  if (!modal) return;

  // Reset số lượng
  currentModalQty = 1;
  const qtyEl = document.getElementById('modalQtyDisplay');
  if (qtyEl) qtyEl.textContent = '1';

  // Điền tiêu đề & giá
  const titleEl = document.getElementById('modalCarTitle');
  if (titleEl) titleEl.textContent = title || 'Mô hình siêu xe Ares';

  const priceEl = document.getElementById('modalCarPrice');
  if (priceEl) priceEl.textContent = price || 'Liên hệ';

  // Tính giá cũ (+25%)
  const rawPrice = parseInt((price || '').replace(/[^0-9]/g, '')) || 0;
  if (rawPrice > 0) {
    const oldPrice = Math.round((rawPrice * 1.25) / 10000) * 10000;
    const oldPriceStr = oldPrice.toLocaleString('vi-VN') + '₫';
    const oldEl = document.getElementById('modalCarPriceOld');
    if (oldEl) oldEl.textContent = oldPriceStr;
  }

  // Điền ảnh
  const imgEl = document.getElementById('modalCarImg');
  if (imgEl) imgEl.src = img || '';

  // Nhận diện loại xe thông minh
  const isRC = (title || '').toUpperCase().includes('RC') || (title || '').toUpperCase().includes('DRIFT');

  const badgeEl = document.getElementById('modalBadge');
  if (badgeEl) badgeEl.textContent = isRC ? 'Ares RC Pro Edition' : 'Ares Official Diecast';

  const catEl = document.getElementById('modalCategory');
  if (catEl) catEl.textContent = category || (isRC ? 'RC Cars • Xe Điều Khiển Tốc Độ Cao' : 'Model Cars • Siêu Xe Tĩnh Chi Tiết Cao');

  const scaleEl = document.getElementById('modalSpecScale');
  if (scaleEl) {
    scaleEl.textContent = scale || (isRC ? '1:10 Drift Spec' : '1:18 Collector Scale');
  }

  const matEl = document.getElementById('modalSpecMaterial');
  if (matEl) {
    matEl.textContent = material || (isRC ? 'Khung gầm Carbon & Hợp kim nhôm 6061' : 'Hợp kim kẽm Diecast cao cấp + Sơn tĩnh điện');
  }

  const featEl = document.getElementById('modalSpecFeature');
  if (featEl) {
    featEl.textContent = feature || (isRC ? 'Motor không chổi than, 60km/h, ESC 60A' : 'Mở full 4 cửa, nắp capo, chi tiết động cơ');
  }

  modal.classList.add('active');
  document.body.style.overflow = 'hidden'; // Khóa cuộn trang khi mở modal
};

// Đóng modal
window.closeCarModal = function() {
  const modal = document.getElementById('productModal');
  if (modal) {
    modal.classList.remove('active');
    document.body.style.overflow = ''; // Mở lại cuộn trang
  }
};

// Thêm vào giỏ từ modal
window.addToCartFromModal = function() {
  cartItemCount += currentModalQty;
  updateCartDisplay();
  const currentTitle = document.getElementById('modalCarTitle') ? document.getElementById('modalCarTitle').textContent : 'sản phẩm';
  closeCarModal();
  alert(`Đã thêm ${currentModalQty}x "${currentTitle}" vào giỏ hàng thành công!`);
};

// Đóng khi click ngoài modal
window.addEventListener('click', (e) => {
  const modal = document.getElementById('productModal');
  if (e.target === modal) {
    closeCarModal();
  }
});

// Đóng khi nhấn phím ESC
window.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    closeCarModal();
  }
});
