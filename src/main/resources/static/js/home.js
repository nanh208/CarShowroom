/**
 * ARES TOYS & CAR SHOWROOM - JAVASCRIPT ĐIỀU KHIỂN GIAO DIỆN 2.0
 */

const SAMPLE_CARS = [
  {
    id: 1,
    code: "PS 340",
    name: "Huracan STO RC Drifting",
    brand: "Lamborghini",
    scale: "1:10 Drift Spec",
    category: "RC_CARS",
    price: 1890000,
    oldPrice: 2360000,
    discount: "-20%",
    image: "/images/cars/bestseller-1.jpg",
    material: "Khung gầm Carbon & Nhôm 6061",
    feature: "Động cơ Brushless, 60km/h, ESC 60A chống nước",
    description: "Mô hình RC Lamborghini Huracan STO bản giới hạn, tốc độ cao với hệ thống dẫn động 4 bánh toàn thời gian (4WD), tay cầm 2.4GHz không độ trễ.",
    stock: "Còn hàng"
  },
  {
    id: 2,
    code: "Huayra R",
    name: "Pagani Huayra R Full Carbon",
    brand: "Pagani",
    scale: "1:18 Collector Scale",
    category: "MODEL_CARS",
    price: 2450000,
    oldPrice: 3100000,
    discount: "-21%",
    image: "/images/cars/bestseller-2.jpg",
    material: "Hợp kim kẽm Diecast cao cấp + Vân Carbon phủ bóng",
    feature: "Mở cánh cửa cánh chim gull-wing, nắp capo trước sau, chi tiết khối động cơ V12",
    description: "Mô hình tĩnh siêu xe Pagani Huayra R hoàn thiện tinh xảo bậc nhất thế giới, nội thất bọc da thật thu nhỏ, mâm xe cắt CNC sắc nét.",
    stock: "Còn hàng"
  },
  {
    id: 3,
    code: "FR F40",
    name: "Ferrari F40 Berlinetta Rosso",
    brand: "Ferrari",
    scale: "1:18 Collector Scale",
    category: "MODEL_CARS",
    price: 2890000,
    oldPrice: 3600000,
    discount: "-20%",
    image: "/images/cars/bestseller-3.jpg",
    material: "Hợp kim đúc nguyên khối sơn tĩnh điện 4 lớp Rosso Corsa",
    feature: "Mở nắp động cơ sau V8 Twin-Turbo, mở đèn mắt lật pop-up",
    description: "Huyền thoại siêu xe Ferrari F40 nguyên bản, tái hiện sắc nét từ cánh gió đuôi trứ danh tới khối động cơ V8 tăng áp kép.",
    stock: "Còn hàng"
  },
  {
    id: 4,
    code: "M4 Drift",
    name: "BMW M4 Competition RC Drift",
    brand: "BMW",
    scale: "1:10 Drift Spec",
    category: "RC_CARS",
    price: 1290000,
    oldPrice: 1600000,
    discount: "-19%",
    image: "/images/cars/rc-1.jpg",
    material: "Khung gia cố sợi thủy tinh, lốp Drift chuyên dụng",
    feature: "Vi sai cầu sau độ khóa, góc lái mở rộng 65 độ, đèn pha LED",
    description: "Chiếc xe RC hoàn hảo cho các tay lái đam mê bộ môn drift mô hình. Dễ kiểm soát góc trượt và phản hồi ga cực kỳ chân thực.",
    stock: "Còn hàng"
  },
  {
    id: 5,
    code: "DP 450",
    name: "Nissan Skyline GT-R R34 Nismo",
    brand: "Nissan",
    scale: "1:10 High-Speed Spec",
    category: "RC_CARS",
    price: 2150000,
    oldPrice: 2700000,
    discount: "-20%",
    image: "/images/cars/rc-2.jpg",
    material: "Hợp kim nhôm T6 & Nhựa ABS siêu bền",
    feature: "Hệ dẫn động đai 2 cầu, tốc độ tối đa 75km/h, pin Lipo 3S 5200mAh",
    description: "Huyền thoại Godzilla Skyline GT-R R34 trong lớp áo Z-Tune Nismo. Sức mạnh bùng nổ trên mọi cung đường đua tốc độ.",
    stock: "Còn hàng"
  },
  {
    id: 6,
    code: "HS 832 GTB",
    name: "Porsche 911 GT3 RS Track Spec",
    brand: "Porsche",
    scale: "1:10 Grip Spec",
    category: "RC_CARS",
    price: 1690000,
    oldPrice: 2100000,
    discount: "-20%",
    image: "/images/cars/rc-3.jpg",
    material: "Khung gầm carbon mật độ cao, cánh gió khí động học DRS chủ động",
    feature: "Hệ thống con quay hồi chuyển Gyro ổn định lái, motor không chổi than 3650",
    description: "Mẫu xe RC chinh phục các khúc cua gắt với độ bám đường đỉnh cao, được mô phỏng theo chiến mã Nurburgring của thương hiệu Stuttgart.",
    stock: "Còn hàng"
  },
  {
    id: 7,
    code: "PS 938 BMR",
    name: "Bugatti Chiron Pur Sport 1:18",
    brand: "Bugatti",
    scale: "1:18 Collector Scale",
    category: "MODEL_CARS",
    price: 3200000,
    oldPrice: 4000000,
    discount: "-20%",
    image: "/images/cars/model-1.jpg",
    material: "Hợp kim kẽm Diecast cao cấp màu sơn đen nhám Matte Black",
    feature: "Mở khoang hành lý phía trước, mở 2 cửa hông, cánh gió sau cố định cỡ lớn",
    description: "Mô hình Bugatti Chiron Pur Sport mang tông màu đen nhám huyền bí, khoang động cơ quad-turbo W16 8.0L thể hiện từng đường ống dẫn và logo Bugatti.",
    stock: "Còn hàng"
  },
  {
    id: 8,
    code: "MONZA SP2",
    name: "Ferrari Monza SP2 Speedster",
    brand: "Ferrari",
    scale: "1:18 Collector Scale",
    category: "MODEL_CARS",
    price: 4100000,
    oldPrice: 5100000,
    discount: "-20%",
    image: "/images/cars/model-2.jpg",
    material: "Hợp kim đúc nguyên khối sơn bóng công nghệ cao",
    feature: "Thiết kế Speedster mui trần không kính chắn gió, 2 ghế ngồi bọc da nâu cognac thủ công",
    description: "Mẫu xe siêu hiếm trong bộ sưu tập Icona của hãng ngựa chồm. Từng nút bấm điều khiển trên táp-lô đều được thu nhỏ với tỉ lệ hoàn hảo.",
    stock: "Còn hàng"
  },
  {
    id: 9,
    code: "PS 911 GT3 RSR",
    name: "Porsche 911 GT3 R Neon Pink",
    brand: "Ferrari",
    scale: "1:18 Collector Scale",
    category: "MODEL_CARS",
    price: 3650000,
    oldPrice: 4550000,
    discount: "-20%",
    image: "/images/cars/model-3.jpg",
    material: "Hợp kim cao cấp phủ sơn dạ quang Neon nổi bật",
    feature: "Trang bị lồng chống lật roll-cage chuẩn FIA, đai an toàn 6 điểm bằng vải thật",
    description: "Chiếc xe đua đường trường GT3 R rực rỡ dưới ánh đèn showroom, là điểm nhấn ấn tượng trong bất kỳ bộ sưu tập mô hình xe hơi nào.",
    stock: "Còn hàng"
  }
];

let cartItems = [];
let savedCartItems = [];
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
const checkoutModal = document.getElementById('checkoutModal');
const cartCountDisplay = document.getElementById('cartCountDisplay');
let sandboxOrderCode = null;
let sandboxOrderToken = null;
let sandboxTransactionRef = null;
let returnOrder = null;

function openReturnsModal() {
  const modal = document.getElementById('returnsModal');
  if (!modal) return;
  resetReturnLookup();
  document.getElementById('returnTrackMessage').textContent = '';
  modal.classList.add('active');
  document.body.style.overflow = 'hidden';
}

function closeReturnsModal() {
  document.getElementById('returnsModal')?.classList.remove('active');
  document.body.style.overflow = 'auto';
}

function resetReturnLookup() {
  returnOrder = null;
  const lookup = document.getElementById('returnLookupForm');
  const request = document.getElementById('returnRequestForm');
  const success = document.getElementById('returnSuccess');
  lookup.reset();
  request.reset();
  lookup.hidden = false;
  request.hidden = true;
  success.hidden = true;
  document.getElementById('returnMessage').textContent = '';
  document.getElementById('returnMessage').classList.remove('is-error');
  document.getElementById('returnOrderItems').replaceChildren();
}

async function handleReturnLookup(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const message = document.getElementById('returnMessage');
  const button = form.querySelector('button[type="submit"]');
  const orderCode = form.elements.orderCode.value.trim();
  const phone = form.elements.phone.value.trim();
  button.disabled = true;
  message.textContent = 'Đang xác minh đơn hàng…';
  message.classList.remove('is-error');
  try {
    const query = new URLSearchParams({ phone });
    const response = await fetch(`/api/returns/orders/${encodeURIComponent(orderCode)}?${query}`, { headers: { Accept: 'application/json' } });
    const result = await response.json();
    if (!response.ok || !result.success || !result.data) throw new Error(result.message || 'Không tra cứu được đơn hàng.');
    returnOrder = { ...result.data, phone };
    renderReturnItems(returnOrder);
    form.hidden = true;
    const requestForm = document.getElementById('returnRequestForm');
    requestForm.hidden = returnOrder.hasOpenRequest;
    if (returnOrder.hasOpenRequest) {
      message.textContent = 'Đơn hàng đã có yêu cầu đổi trả đang chờ xử lý.';
      form.hidden = false;
    }
  } catch (error) {
    message.textContent = error.message || 'Không thể kết nối để xác minh đơn hàng.';
    message.classList.add('is-error');
  } finally {
    button.disabled = false;
  }
}

function renderReturnItems(order) {
  const container = document.getElementById('returnOrderItems');
  container.replaceChildren();
  const deadline = document.createElement('p');
  deadline.className = 'checkout-payment-note';
  deadline.textContent = `Đơn ${order.orderCode} · gửi yêu cầu trước ${new Date(order.returnDeadline).toLocaleString('vi-VN')}`;
  container.appendChild(deadline);
  order.items.forEach((item) => {
    const row = document.createElement('label');
    row.className = 'checkout-summary-line';
    const name = document.createElement('span');
    name.textContent = `${item.productName} (đã mua ${item.purchasedQuantity}, còn có thể yêu cầu ${item.availableQuantity})`;
    const quantity = document.createElement('select');
    quantity.name = 'returnQuantity';
    quantity.dataset.productCode = item.productCode;
    for (let count = 0; count <= item.availableQuantity; count++) {
      const option = document.createElement('option');
      option.value = String(count);
      option.textContent = count === 0 ? 'Không đổi trả' : `Số lượng: ${count}`;
      quantity.appendChild(option);
    }
    quantity.setAttribute('aria-label', `Số lượng đổi trả ${item.productName}`);
    row.append(name, quantity);
    container.appendChild(row);
  });
}

async function submitReturnRequest(event) {
  event.preventDefault();
  if (!returnOrder) return;
  const form = event.currentTarget;
  const button = form.querySelector('button[type="submit"]');
  const items = [...form.querySelectorAll('select[name="returnQuantity"]')]
    .map(select => ({ productCode: select.dataset.productCode, quantity: Number(select.value) }))
    .filter(item => item.quantity > 0);
  if (!items.length) {
    document.getElementById('returnMessage').textContent = 'Chọn ít nhất một sản phẩm cần đổi trả.';
    document.getElementById('returnMessage').classList.add('is-error');
    return;
  }
  button.disabled = true;
  try {
    const response = await fetch(`/api/returns/orders/${encodeURIComponent(returnOrder.orderCode)}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ phone: returnOrder.phone, kind: form.elements.kind.value, reason: form.elements.reason.value.trim(), items })
    });
    const result = await response.json();
    if (!response.ok || !result.success || !result.data) throw new Error(result.message || 'Không gửi được yêu cầu đổi trả.');
    document.getElementById('returnRequestForm').hidden = true;
    document.getElementById('returnLookupForm').hidden = true;
    document.getElementById('returnSuccessText').textContent = `Mã yêu cầu: ${result.data.requestCode}. Trạng thái: đang chờ cửa hàng xem xét. Giá trị hoàn tiền ước tính (nếu được duyệt): ${formatVND(result.data.estimatedRefundAmount)}. Đây chưa phải giao dịch hoàn tiền.`;
    document.getElementById('returnSuccess').hidden = false;
  } catch (error) {
    const message = document.getElementById('returnMessage');
    message.textContent = error.message || 'Không thể kết nối để gửi yêu cầu.';
    message.classList.add('is-error');
  } finally {
    button.disabled = false;
  }
}

async function trackReturnRequest(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const message = document.getElementById('returnTrackMessage');
  const query = new URLSearchParams({ phone: form.elements.phone.value.trim() });
  try {
    const response = await fetch(`/api/returns/${encodeURIComponent(form.elements.requestCode.value.trim())}?${query}`, { headers: { Accept: 'application/json' } });
    const result = await response.json();
    if (!response.ok || !result.success || !result.data) throw new Error(result.message || 'Không tra cứu được yêu cầu.');
    const statuses = { PENDING: 'Chờ cửa hàng xem xét', APPROVED: 'Đã duyệt', REJECTED: 'Không được duyệt', REFUNDED: 'Đã hoàn tiền' };
    message.textContent = `Đơn ${result.data.orderCode}: ${statuses[result.data.status] || result.data.status}.`;
    message.classList.remove('is-error');
  } catch (error) {
    message.textContent = error.message || 'Không thể kết nối để tra cứu.';
    message.classList.add('is-error');
  }
}

function loadCartFromStorage() {
  try {
    const readLines = (key) => {
      const value = JSON.parse(localStorage.getItem(key) || '[]');
      if (!Array.isArray(value)) return [];
      return value.flatMap((line) => {
        const car = line.code
          ? SAMPLE_CARS.find((item) => item.code.toLowerCase() === String(line.code).toLowerCase())
          : SAMPLE_CARS.find((item) => item.id === Number(line.id));
        if (!car) return [];
        const stock = getStockLimit(car);
        if (stock < 1) return [];
        const qty = Number.parseInt(line.qty, 10);
        if (!Number.isFinite(qty) || qty < 1) return [];
        return [{ id: car.id, code: car.code, name: car.name, price: car.price, image: car.image, qty: Math.min(qty, stock) }];
      });
    };
    cartItems = readLines('ares_cart');
    savedCartItems = readLines('ares_cart_saved');
  } catch (e) {
    cartItems = [];
    savedCartItems = [];
  }
  saveCartToStorage();
  updateCartUI();
}

function saveCartToStorage() {
  localStorage.setItem('ares_cart', JSON.stringify(cartItems));
  localStorage.setItem('ares_cart_saved', JSON.stringify(savedCartItems));
  updateCartUI();
}

function getStockLimit(car) {
  const match = String(car?.stock || '').match(/\d+/);
  return match ? Math.max(0, Number(match[0])) : 99;
}

function addToCart(car, qty = 1) {
  const stock = getStockLimit(car);
  if (!car || stock < 1) {
    showToast('Sản phẩm hiện đã hết hàng.');
    return;
  }
  const amount = Math.max(1, Math.floor(Number(qty) || 1));
  const existing = cartItems.find(item => item.id === car.id);
  if (existing) {
    const nextQty = Math.min(existing.qty + amount, stock);
    if (nextQty === existing.qty) {
      showToast(`Chỉ còn tối đa ${stock} sản phẩm trong kho.`);
      return;
    }
    existing.qty = nextQty;
  } else {
    cartItems.push({
      id: car.id,
      code: car.code,
      name: car.name,
      price: car.price,
      image: car.image,
      qty: Math.min(amount, stock)
    });
  }
  saveCartToStorage();
  showToast(`Đã cập nhật giỏ hàng: ${car.name} (${cartItems.find(item => item.id === car.id).qty}).`);
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

function setCartQuantity(carId, quantity) {
  const line = cartItems.find(item => item.id === carId);
  const car = SAMPLE_CARS.find(item => item.id === carId);
  if (!line || !car) return;
  if (quantity < 1) {
    removeFromCart(carId);
    return;
  }
  line.qty = Math.min(Math.floor(quantity), getStockLimit(car));
  saveCartToStorage();
  renderCartItemsList();
}

function saveForLater(carId) {
  const line = cartItems.find(item => item.id === carId);
  if (!line) return;
  cartItems = cartItems.filter(item => item.id !== carId);
  savedCartItems = [line, ...savedCartItems.filter(item => item.id !== carId)];
  saveCartToStorage();
  renderCartItemsList();
}

function moveSavedToCart(carId) {
  const line = savedCartItems.find(item => item.id === carId);
  const car = SAMPLE_CARS.find(item => item.id === carId);
  if (!line || !car) return;
  const inCart = cartItems.find(item => item.id === carId);
  if ((inCart?.qty || 0) >= getStockLimit(car)) {
    showToast('Giỏ hàng đã đạt số lượng tối đa còn trong kho.');
    return;
  }
  savedCartItems = savedCartItems.filter(item => item.id !== carId);
  addToCart(car, 1);
  saveCartToStorage();
  renderCartItemsList();
}

function removeSavedItem(carId) {
  savedCartItems = savedCartItems.filter(item => item.id !== carId);
  saveCartToStorage();
  renderCartItemsList();
}

function updateCartUI() {
  const totalCount = cartItems.reduce((acc, it) => acc + it.qty, 0);
  if (cartCountDisplay) {
    cartCountDisplay.textContent = `${totalCount} món`;
  }
}

function renderCartItemsList() {
  const listEl = document.getElementById('cartItemsList');
  const savedEl = document.getElementById('cartSavedList');
  const totalValEl = document.getElementById('cartTotalPrice');
  if (!listEl) return;

  let totalMoney = 0;
  listEl.replaceChildren();
  if (cartItems.length === 0) {
    const empty = document.createElement('div');
    empty.className = 'cart-empty-state';
    empty.textContent = 'Giỏ hàng của bạn đang trống.';
    listEl.appendChild(empty);
  }
  cartItems.forEach(item => {
    const sub = item.price * item.qty;
    totalMoney += sub;
    const row = document.createElement('div');
    row.className = 'cart-item-row';
    const image = document.createElement('img');
    image.className = 'cart-item-thumb';
    image.src = item.image;
    image.alt = item.name;
    const details = document.createElement('div');
    details.className = 'cart-item-details';
    const title = document.createElement('div');
    title.className = 'cart-item-title';
    title.textContent = item.name;
    const price = document.createElement('div');
    price.className = 'cart-item-price';
    price.textContent = `${formatVND(item.price)} × ${item.qty} = ${formatVND(sub)}`;
    const controls = document.createElement('div');
    controls.className = 'cart-item-controls';
    const minus = document.createElement('button');
    minus.type = 'button'; minus.textContent = '−'; minus.setAttribute('aria-label', `Giảm số lượng ${item.name}`);
    minus.addEventListener('click', () => setCartQuantity(item.id, item.qty - 1));
    const quantity = document.createElement('span');
    quantity.textContent = String(item.qty);
    const plus = document.createElement('button');
    plus.type = 'button'; plus.textContent = '+'; plus.setAttribute('aria-label', `Tăng số lượng ${item.name}`);
    plus.disabled = item.qty >= getStockLimit(SAMPLE_CARS.find(car => car.id === item.id));
    plus.addEventListener('click', () => setCartQuantity(item.id, item.qty + 1));
    controls.append(minus, quantity, plus);
    const save = document.createElement('button');
    save.type = 'button'; save.className = 'cart-save-later'; save.textContent = 'Để mua sau';
    save.addEventListener('click', () => saveForLater(item.id));
    details.append(title, price, controls, save);
    const remove = document.createElement('button');
    remove.type = 'button'; remove.className = 'cart-item-remove'; remove.title = 'Xoá'; remove.setAttribute('aria-label', `Xoá ${item.name}`); remove.textContent = '×';
    remove.addEventListener('click', () => removeFromCart(item.id));
    row.append(image, details, remove);
    listEl.appendChild(row);
  });

  if (savedEl) {
    savedEl.replaceChildren();
    if (savedCartItems.length) {
      const heading = document.createElement('h4');
      heading.className = 'cart-saved-heading';
      heading.textContent = `Đã lưu để mua sau (${savedCartItems.length})`;
      savedEl.appendChild(heading);
      savedCartItems.forEach(item => {
        const row = document.createElement('div');
        row.className = 'cart-saved-row';
        const label = document.createElement('span');
        label.textContent = `${item.name} · ${formatVND(item.price)}`;
        const restore = document.createElement('button');
        restore.type = 'button'; restore.textContent = 'Thêm lại';
        restore.addEventListener('click', () => moveSavedToCart(item.id));
        const remove = document.createElement('button');
        remove.type = 'button'; remove.textContent = 'Xoá';
        remove.addEventListener('click', () => removeSavedItem(item.id));
        row.append(label, restore, remove);
        savedEl.appendChild(row);
      });
    }
  }

  if (totalValEl) {
    totalValEl.textContent = formatVND(totalMoney);
  }
  const clearButton = document.querySelector('.cart-btn-clear');
  const checkoutButton = document.querySelector('.cart-btn-checkout');
  if (clearButton) clearButton.disabled = cartItems.length === 0;
  if (checkoutButton) checkoutButton.disabled = cartItems.length === 0;
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
  openCheckoutModal();
}

function openCheckoutModal() {
  if (!cartItems.length) {
    showToast('Giỏ hàng của bạn đang trống.');
    return;
  }
  const summary = document.getElementById('checkoutSummary');
  const total = document.getElementById('checkoutTotal');
  const form = document.getElementById('checkoutForm');
  const success = document.getElementById('checkoutSuccess');
  const paymentActions = document.getElementById('sandboxPaymentActions');
  const message = document.getElementById('checkoutMessage');
  if (!checkoutModal || !summary || !total || !form || !success || !message) return;

  summary.replaceChildren();
  cartItems.forEach((item) => {
    const line = document.createElement('div');
    line.className = 'checkout-summary-line';
    const name = document.createElement('span');
    name.textContent = `${item.name} × ${item.qty}`;
    const price = document.createElement('strong');
    price.textContent = formatVND(item.price * item.qty);
    line.append(name, price);
    summary.appendChild(line);
  });
  total.textContent = formatVND(cartItems.reduce((sum, item) => sum + item.price * item.qty, 0));
  form.reset();
  form.hidden = false;
  success.hidden = true;
  paymentActions.hidden = true;
  sandboxOrderCode = sandboxOrderToken = sandboxTransactionRef = null;
  message.textContent = '';
  message.classList.remove('is-error');
  closeCartModal();
  checkoutModal.classList.add('active');
  document.body.style.overflow = 'hidden';
  form.elements.customerName.focus();
}

function closeCheckoutModal() {
  if (checkoutModal) checkoutModal.classList.remove('active');
  document.body.style.overflow = 'auto';
}

async function handleCheckout(event) {
  event.preventDefault();
  const form = event.currentTarget;
  const submitButton = form.querySelector('button[type="submit"]');
  const message = document.getElementById('checkoutMessage');
  const success = document.getElementById('checkoutSuccess');
  const successText = document.getElementById('checkoutSuccessText');
  const paymentActions = document.getElementById('sandboxPaymentActions');
  if (!cartItems.length) {
    message.textContent = 'Giỏ hàng đang trống. Vui lòng quay lại chọn sản phẩm.';
    message.classList.add('is-error');
    return;
  }

  submitButton.disabled = true;
  submitButton.textContent = 'Đang gửi đơn…';
  message.textContent = '';
  message.classList.remove('is-error');
  try {
    const payload = {
      customerName: form.elements.customerName.value.trim(),
      phone: form.elements.phone.value.trim(),
      email: form.elements.email.value.trim(),
      shippingAddress: form.elements.shippingAddress.value.trim(),
      note: form.elements.note.value.trim(),
      paymentMethod: form.elements.paymentMethod.value,
      items: cartItems.map(item => ({ productCode: item.code, quantity: item.qty }))
    };
    const response = await fetch('/api/orders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify(payload)
    });
    const result = await response.json();
    if (!response.ok || !result.success || !result.data) {
      throw new Error(result.message || 'Chưa thể tạo đơn hàng. Vui lòng thử lại.');
    }

    const order = result.data;
    cartItems = [];
    saveCartToStorage();
    form.hidden = true;
    if (order.paymentMethod === 'COD') {
      successText.textContent = `Mã đơn hàng: ${order.code}. Tổng tiền ${formatVND(order.total)}. Đơn đang chờ xác nhận; bạn sẽ thanh toán khi nhận hàng.`;
    } else {
      sandboxOrderCode = order.code;
      sandboxOrderToken = order.paymentToken;
      successText.textContent = `Mã đơn hàng: ${order.code}. Tổng tiền ${formatVND(order.total)}. Đây là phiên thanh toán mô phỏng, không phát sinh giao dịch thật.`;
      try {
        const paymentResponse = await fetch(`/api/payments/sandbox/orders/${encodeURIComponent(order.code)}`, {
          method: 'POST', headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
          body: JSON.stringify({ orderToken: sandboxOrderToken, provider: order.paymentMethod })
        });
        const paymentResult = await paymentResponse.json();
        if (!paymentResponse.ok || !paymentResult.success || !paymentResult.data) {
          throw new Error(paymentResult.message || 'Chưa tạo được phiên mô phỏng.');
        }
        sandboxTransactionRef = paymentResult.data.transactionRef;
        document.getElementById('sandboxPaymentStatus').textContent = `Mã giao dịch mô phỏng: ${sandboxTransactionRef}. Phiên hết hạn lúc ${new Date(paymentResult.data.expiresAt).toLocaleString('vi-VN')}. Chọn kết quả mô phỏng bên dưới.`;
      } catch (paymentError) {
        document.getElementById('sandboxPaymentStatus').textContent = `${paymentError.message} Đơn đã được tạo (${order.code}), bạn có thể đóng cửa sổ và liên hệ cửa hàng để thanh toán lại.`;
      }
      paymentActions.hidden = false;
    }
    success.hidden = false;
  } catch (error) {
    message.textContent = error.message || 'Không thể kết nối để gửi đơn. Vui lòng thử lại.';
    message.classList.add('is-error');
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = 'Xác nhận đặt hàng';
  }
}

async function completeSandboxPayment(responseCode) {
  const status = document.getElementById('sandboxPaymentStatus');
  const buttons = document.querySelectorAll('#sandboxPaymentActions button');
  if (!sandboxOrderToken || !sandboxTransactionRef) {
    status.textContent = 'Không có phiên thanh toán đang chờ. Vui lòng tạo đơn mới để thử lại.';
    return;
  }
  buttons.forEach(button => { button.disabled = true; });
  try {
    const response = await fetch(`/api/payments/sandbox/transactions/${encodeURIComponent(sandboxTransactionRef)}/complete`, {
      method: 'POST', headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify({ orderToken: sandboxOrderToken, responseCode })
    });
    const result = await response.json();
    if (!response.ok || !result.success || !result.data) throw new Error(result.message || 'Không thể ghi nhận kết quả mô phỏng.');
    const payment = result.data;
    status.textContent = payment.status === 'SUCCESS'
      ? `Mô phỏng thành công. Đơn ${payment.orderCode} đã được đánh dấu đã thanh toán (sandbox).`
      : payment.status === 'CANCELLED' ? 'Bạn đã mô phỏng hủy thanh toán. Đơn hàng vẫn chưa được thanh toán.'
        : 'Mô phỏng thanh toán thất bại. Đơn hàng vẫn chưa được thanh toán.';
    if (payment.status !== 'PENDING') buttons.forEach(button => { button.hidden = true; });
  } catch (error) {
    status.textContent = error.message || 'Không thể kết nối với dịch vụ thanh toán mô phỏng.';
  } finally {
    buttons.forEach(button => { if (!button.hidden) button.disabled = false; });
  }
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
  if (checkoutModal) {
    checkoutModal.addEventListener('click', (e) => {
      if (e.target === checkoutModal) closeCheckoutModal();
    });
  }
  const returnsModal = document.getElementById('returnsModal');
  if (returnsModal) {
    returnsModal.addEventListener('click', (e) => {
      if (e.target === returnsModal) closeReturnsModal();
    });
  }
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && checkoutModal?.classList.contains('active')) closeCheckoutModal();
    if (e.key === 'Escape' && returnsModal?.classList.contains('active')) closeReturnsModal();
  });
});
