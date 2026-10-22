/**
 * ARES TOYS & CAR SHOWROOM - JAVASCRIPT ĐIỀU KHIỂN GIAO DIỆN 2.0
 */

const SAMPLE_CARS = [
  {
    id: 1,
    name: "Hot Wheels Skyline GT-R R34 Premium",
    brand: "Hot Wheels",
    scale: "1:64",
    category: "DIECAST_MODEL",
    price: 3200000,
    oldPrice: 4000000,
    discount: "-20%",
    image: "/images/toys/hw-r34-0.jpg",
    material: "Hợp kim đúc Diecast, lốp cao su Real Riders",
    feature: "Phiên bản Premium kim loại nguyên khối, chi tiết tem đua sắc nét",
    description: "Huyền thoại JDM thu nhỏ tỷ lệ 1:64 cực nét dành cho nhà sưu tầm Hot Wheels chuyên nghiệp.",
    stock: "Còn 12 chiếc"
  },
  {
    id: 2,
    name: "Traxxas Slash 4X4 VXL Short Course",
    brand: "Traxxas",
    scale: "1:10",
    category: "RC_CARS",
    price: 8900000,
    oldPrice: 10500000,
    discount: "-15%",
    image: "/images/toys/traxxas-slash-0.jpg",
    material: "Khung composite chống sốc cực bền",
    feature: "Động cơ Brushless Velineon 3S chống nước, tốc độ >95km/h",
    description: "Vua xe bán tải địa hình RC chuyên bay nhảy và chạy đường gồ ghề bất chấp mọi thời tiết.",
    stock: "Còn 4 chiếc"
  },
  {
    id: 3,
    name: "Kyosho Lamborghini Countach LP500S 1:18",
    brand: "Kyosho",
    scale: "1:18",
    category: "DIECAST_MODEL",
    price: 4250000,
    oldPrice: 4850000,
    discount: "-12%",
    image: "/images/toys/kyosho-countach-0.jpg",
    material: "Hợp kim đúc tinh xảo",
    feature: "Mở full cửa cánh chim, capo trước sau, đèn mắt lật",
    description: "Biểu tượng siêu xe thập niên 80 được tái hiện từng chi tiết động cơ V12 và nội thất da cổ điển.",
    stock: "Còn 3 chiếc"
  },
  {
    id: 4,
    name: "WLToys 144001 4WD High Speed Buggy",
    brand: "WLToys",
    scale: "1:14",
    category: "RC_CARS",
    price: 1250000,
    oldPrice: 1550000,
    discount: "-18%",
    image: "/images/toys/wltoys-144001-0.jpg",
    material: "Khung hợp kim nhôm",
    feature: "Dẫn động 4 bánh 4WD, giảm xóc dầu, tốc độ 60km/h",
    description: "Mẫu RC Buggy nhập môn tốt nhất phân khúc với khung gầm kim loại chắc chắn.",
    stock: "Còn 15 chiếc"
  },
  {
    id: 5,
    name: "BMW M4 Competition RC Track Drifter",
    brand: "BMW",
    scale: "1:10",
    category: "RC_CARS",
    price: 2850000,
    oldPrice: 3400000,
    discount: "-16%",
    image: "/images/cars/rc-1.jpg",
    material: "Khung composite sợi thủy tinh",
    feature: "Gyro trợ lái ESP drift mượt mà, đèn LED pha",
    description: "Dòng RC drift chuyên nghiệp cho sân tập và thi đấu.",
    stock: "Còn 6 chiếc"
  },
  {
    id: 6,
    name: "Nissan Skyline GT-R R34 Nismo Z-Tune",
    brand: "Nissan",
    scale: "1:10",
    category: "RC_CARS",
    price: 3600000,
    oldPrice: 4200000,
    discount: "-15%",
    image: "/images/cars/rc-2.jpg",
    material: "Khung hợp kim nhôm CNC",
    feature: "Động cơ không chổi than, pin Lipo 3S tốc độ 80km/h",
    description: "Quái vật Godzilla R34 phong cách Nismo Z-Tune bạc đặc trưng.",
    stock: "Còn 5 chiếc"
  },
  {
    id: 7,
    name: "Porsche 911 GT3 RS Track Spec",
    brand: "Porsche",
    scale: "1:10",
    category: "RC_CARS",
    price: 3450000,
    oldPrice: 3950000,
    discount: "-13%",
    image: "/images/cars/rc-3.jpg",
    material: "Khung carbon gia cố, cánh gió khí động học",
    feature: "Treo độc lập 4 bánh, lốp bám đường cao cấp",
    description: "Chiến mã đường đua 911 GT3 RS phiên bản điều khiển từ xa.",
    stock: "Còn 2 chiếc"
  },
  {
    id: 8,
    name: "Bugatti Chiron Pur Sport 1:18 Limited",
    brand: "Bugatti",
    scale: "1:18",
    category: "DIECAST_MODEL",
    price: 5200000,
    oldPrice: 6000000,
    discount: "-14%",
    image: "/images/cars/model-1.jpg",
    material: "Resin cao cấp hoàn thiện thủ công",
    feature: "Kèm đế da sang trọng và hộp mica trưng bày chống bụi",
    description: "Bản sao hoàn mỹ của siêu phẩm Chiron Pur Sport.",
    stock: "Còn 2 chiếc"
  },
  {
    id: 9,
    name: "Ferrari Monza SP2 Speedster Icona",
    brand: "Ferrari",
    scale: "1:18",
    category: "DIECAST_MODEL",
    price: 4300000,
    oldPrice: 4900000,
    discount: "-12%",
    image: "/images/cars/model-2.jpg",
    material: "Hợp kim đúc tinh xảo",
    feature: "Thiết kế Speedster không kính chắn gió, nội thất đỏ",
    description: "Mô hình tôn vinh dòng barchetta huyền thoại.",
    stock: "Còn 4 chiếc"
  }
];

let cartItems = [];
let currentModalCar = null;
let currentModalQty = 1;

function formatVND(amount) {
  if (isNaN(amount)) return "0 ₫";
  return new Intl.NumberFormat('vi-VN').format(amount) + " ₫";
}

// 1. FLASH SALE TIMER
function startFlashCountdown() {
  const cdElement = document.getElementById('flashTimer');
  if (!cdElement) return;

  let remainingSeconds = 8 * 3600 + 45 * 60 + 20;
  function update() {
    if (remainingSeconds <= 0) remainingSeconds = 12 * 3600;
    const h = String(Math.floor(remainingSeconds / 3600)).padStart(2, '0');
    const m = String(Math.floor((remainingSeconds % 3600) / 60)).padStart(2, '0');
    const s = String(remainingSeconds % 60).padStart(2, '0');
    cdElement.textContent = `${h}:${m}:${s}`;
    remainingSeconds--;
  }
  update();
  setInterval(update, 1000);
}

// 2. MODAL CHI TIẾT SẢN PHẨM
const productModal = document.getElementById('productModal');

function openProductModal(carId) {
  const car = SAMPLE_CARS.find(c => c.id === carId) || SAMPLE_CARS[0];
  currentModalCar = car;
  currentModalQty = 1;

  document.getElementById('modalCarImg').src = car.image;
  document.getElementById('modalCategory').textContent = car.category.replace('_', ' ');
  document.getElementById('modalCarTitle').textContent = car.name;
  document.getElementById('modalCarPrice').textContent = formatVND(car.price);
  document.getElementById('modalCarPriceOld').textContent = formatVND(car.oldPrice);
  document.getElementById('modalDiscount').textContent = car.discount;
  document.getElementById('modalSpecScale').textContent = car.scale;
  document.getElementById('modalSpecMaterial').textContent = car.material;
  document.getElementById('modalSpecFeature').textContent = car.feature;
  document.getElementById('modalSpecStock').textContent = car.stock;
  document.getElementById('modalCarDesc').textContent = car.description;
  document.getElementById('modalQtyDisplay').textContent = currentModalQty;

  productModal.classList.add('active');
  document.body.style.overflow = 'hidden';
}

function closeProductModal() {
  productModal.classList.remove('active');
  document.body.style.overflow = 'auto';
}

function changeModalQty(delta) {
  currentModalQty = Math.max(1, currentModalQty + delta);
  document.getElementById('modalQtyDisplay').textContent = currentModalQty;
}

function addToCartFromModal() {
  if (!currentModalCar) return;
  addToCart(currentModalCar, currentModalQty);
  closeProductModal();
}

// 3. GIỎ HÀNG
const cartModal = document.getElementById('cartModal');
const cartCountDisplay = document.getElementById('cartCountDisplay');

function loadCartFromStorage() {
  try {
    const raw = localStorage.getItem('ares_cart');
    cartItems = raw ? JSON.parse(raw) : [];
  } catch (e) {
    cartItems = [];
  }
  updateCartUI();
}

function saveCartToStorage() {
  localStorage.setItem('ares_cart', JSON.stringify(cartItems));
  updateCartUI();
}

function addToCart(car, qty = 1) {
  const existing = cartItems.find(item => item.id === car.id);
  if (existing) {
    existing.qty += qty;
  } else {
    cartItems.push({
      id: car.id,
      name: car.name,
      price: car.price,
      image: car.image,
      qty: qty
    });
  }
  saveCartToStorage();
  showToast(`Đã thêm ${qty}x "${car.name}" vào giỏ hàng!`);
}

function removeFromCart(carId) {
  cartItems = cartItems.filter(item => item.id !== carId);
  saveCartToStorage();
  renderCartItemsList();
}

function clearCart() {
  cartItems = [];
  saveCartToStorage();
  renderCartItemsList();
  showToast("Giỏ hàng đã được làm trống!");
}

function updateCartUI() {
  const totalCount = cartItems.reduce((acc, it) => acc + it.qty, 0);
  if (cartCountDisplay) {
    cartCountDisplay.textContent = `${totalCount} món`;
  }
}

function renderCartItemsList() {
  const listEl = document.getElementById('cartItemsList');
  const totalValEl = document.getElementById('cartTotalPrice');
  if (!listEl) return;

  if (cartItems.length === 0) {
    listEl.innerHTML = '<div style="text-align:center; padding: 32px 10px; color: #888;">Giỏ hàng của bạn đang trống.</div>';
    if (totalValEl) totalValEl.textContent = '0 ₫';
    return;
  }

  let totalMoney = 0;
  listEl.innerHTML = cartItems.map(item => {
    const sub = item.price * item.qty;
    totalMoney += sub;
    return `
      <div class="cart-item-row">
        <img class="cart-item-thumb" src="${item.image}" alt="${item.name}">
        <div class="cart-item-details">
          <div class="cart-item-title">${item.name}</div>
          <div class="cart-item-price">${formatVND(item.price)} x ${item.qty} = ${formatVND(sub)}</div>
        </div>
        <button type="button" class="cart-item-remove" onclick="removeFromCart(${item.id})" title="Xoá">&times;</button>
      </div>
    `;
  }).join('');

  if (totalValEl) {
    totalValEl.textContent = formatVND(totalMoney);
  }
}

function openCartModal() {
  renderCartItemsList();
  cartModal.classList.add('active');
  document.body.style.overflow = 'hidden';
}

function closeCartModal() {
  cartModal.classList.remove('active');
  document.body.style.overflow = 'auto';
}

function handleCheckout() {
  if (cartItems.length === 0) {
    alert("Giỏ hàng của bạn đang trống!");
    return;
  }
  alert("Cảm ơn bạn! Đơn hàng mô phỏng đã được tạo thành công.");
  clearCart();
  closeCartModal();
}

// 4. TOAST
function showToast(message) {
  let toast = document.getElementById('toastNotice');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'toastNotice';
    toast.className = 'toast-notice';
    document.body.appendChild(toast);
  }
  toast.innerHTML = `<span>✓</span> <span>${message}</span>`;
  toast.classList.add('show');
  setTimeout(() => {
    toast.classList.remove('show');
  }, 2800);
}

// 5. NEWSLETTER
function handleNewsletter(e) {
  e.preventDefault();
  const input = document.getElementById('newsletterEmail');
  if (input && input.value) {
    showToast(`Đã ghi nhận đăng ký: ${input.value}`);
    input.value = '';
  }
}

// 6. KHỞI TẠO DOM
document.addEventListener('DOMContentLoaded', () => {
  loadCartFromStorage();
  startFlashCountdown();

  if (productModal) {
    productModal.addEventListener('click', (e) => {
      if (e.target === productModal) closeProductModal();
    });
  }
  if (cartModal) {
    cartModal.addEventListener('click', (e) => {
      if (e.target === cartModal) closeCartModal();
    });
  }
});
