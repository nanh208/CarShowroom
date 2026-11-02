<template>
  <div class="home">
    <!-- ======================================================== -->
    <!-- PHẦN 1: HERO CAROUSEL & PROMO BANNER                     -->
    <!-- ======================================================== -->

    <!-- 1.1 Hero Carousel -->
    <section class="hero" @mouseenter="pause = true" @mouseleave="pause = false">
      <div class="slides">
        <router-link
          v-for="(s, i) in hero"
          :key="s.id"
          :to="s.link || '/products'"
          :class="['slide', { on: i === cur }]"
        >
          <div class="bg" :style="{ backgroundImage: `url(${s.image})` }" />
          <div class="shade" />
          <div class="txt">
            <small>{{ s.kicker }}</small>
            <h1>{{ s.title }}</h1>
            <span>{{ s.subtitle }}</span>
          </div>
        </router-link>
        <div v-if="!hero.length" class="slide on sk" />
      </div>
    </section>

    <!-- Nút chấm tròn chuyển slide (Dots) -->
    <div class="dots">
      <button
        v-for="(s, i) in hero"
        :key="i"
        :class="{ on: i === cur }"
        :aria-label="`Ảnh ${i + 1}`"
        @click="cur = i"
      />
    </div>

    <div class="container">
      <!-- 1.2 Promo Banner -->
      <router-link v-if="promo" :to="promo.link || '/promotions'" class="promo-banner">
        <img loading="lazy" decoding="async" :src="promo.image" alt="" />
        <div />
        <div class="pb-txt">
          <b>{{ promo.title }}</b>
          <i>{{ promo.kicker }}</i>
        </div>
        <div class="pb-off">
          <small>{{ $t('Ưu đãi') }}</small>
          <b>{{ promo.subtitle }}</b>
        </div>
        <div class="pb-moon">
          <svg class="wheel-ico" viewBox="0 0 40 40" width="74" height="74">
            <circle cx="20" cy="20" r="18" fill="#26262a"/>
            <circle cx="20" cy="20" r="11" fill="#d9d9de"/>
            <g stroke="#8a8a90" stroke-width="2.4">
              <path d="M20 9v22M9 20h22M12 12l16 16M28 12 12 28"/>
            </g>
            <circle cx="20" cy="20" r="3.5" fill="#555"/>
          </svg>
        </div>
      </router-link>

      <!-- ======================================================== -->
      <!-- PHẦN 2: DANH MỤC SẢN PHẨM & CAM KẾT DỊCH VỤ (USP)       -->
      <!-- ======================================================== -->

      <!-- 2.1 Danh mục sản phẩm (Categories) -->
      <section v-reveal class="block">
        <h2>{{ $t('Danh mục') }}</h2>
        <div class="cats">
          <router-link
            v-for="c in categoriesList"
            :key="c.code"
            :to="{ path: '/products', query: { type: c.code } }"
            class="cat"
          >
            <span>{{ c.name }}</span>
            <svg
              class="wheel-ico"
              viewBox="0 0 24 24"
              width="20"
              height="20"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
              v-html="icon(c.code)"
            />
          </router-link>
        </div>
      </section>

      <!-- 2.2 Cam kết dịch vụ (USP) -->
      <div class="usp" v-reveal>
        <div>
          <b><CountUp :value="(catalog.options?.freeShipFrom || 500000) / 1000" />K</b>
          <span>{{ $t('Freeship từ') }}</span>
        </div>
        <div>
          <b><CountUp :value="totalProducts || 120" />+</b>
          <span>{{ $t('Mẫu xe & phụ kiện') }}</span>
        </div>
        <div>
          <b><CountUp :value="7" /> {{ $t('ngày') }}</b>
          <span>{{ $t('Đổi trả miễn phí') }}</span>
        </div>
        <div>
          <b>0%</b>
          <span>{{ $t('Trả góp RC từ 3 triệu') }}</span>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '../api'
import CountUp from '../components/CountUp.vue'
import { catalog, loadCatalog } from '../format'

// ----------------------------------------------------
// PHẦN 1: Dữ liệu Banner Hero & Promo
// ----------------------------------------------------
const banners = ref([])
const cur = ref(0)
const pause = ref(false)
let timer = null

const hero = computed(() => banners.value.filter(b => b.position === 'HERO'))
const promo = computed(() => banners.value.find(b => b.position === 'PROMO'))

// ----------------------------------------------------
// PHẦN 2: Danh mục sản phẩm & Cam kết dịch vụ (USP)
// ----------------------------------------------------
const totalProducts = ref(0)

// Fallback danh mục mặc định phòng khi backend chưa kịp phản hồi
const defaultCategories = [
  { code: 'MODEL', name: 'Xe Model' },
  { code: 'RC', name: 'Xe RC' },
  { code: 'DISPLAY', name: 'Xe Trưng Bày' },
  { code: 'KIT', name: 'Lắp ráp & Kit' },
  { code: 'BATTERY', name: 'Pin & Sạc' },
  { code: 'REMOTE', name: 'Khiển RC' },
  { code: 'WHEEL', name: 'Bánh xe RC' },
  { code: 'PART', name: 'Linh kiện & Phụ kiện' }
]

const categoriesList = computed(() => {
  return catalog.categories && catalog.categories.length > 0
    ? catalog.categories
    : defaultCategories
})

// Icon SVG tương ứng cho từng loại danh mục
const I = {
  MODEL: '<path d="M3 15h18l-1.5-4.5A2 2 0 0 0 17.6 9H9l-3 3H4a1 1 0 0 0-1 1z"/><circle cx="7.5" cy="16" r="2"/><circle cx="16.5" cy="16" r="2"/>',
  RC: '<path d="M4 15h16l-2-5H7z"/><circle cx="7" cy="16" r="2.4"/><circle cx="17" cy="16" r="2.4"/><path d="M15 10V4m0 0 3 1"/>',
  DISPLAY: '<rect x="3" y="4" width="18" height="15" rx="2"/><path d="M6 14h12l-1-3H8z"/><path d="M3 19h18"/>',
  KIT: '<rect x="4" y="9" width="16" height="10" rx="1"/><rect x="6" y="6" width="4" height="3"/><rect x="14" y="6" width="4" height="3"/>',
  BATTERY: '<rect x="3" y="7" width="16" height="10" rx="2"/><path d="M21 10v4M11 9l-2 3h4l-2 3"/>',
  REMOTE: '<path d="M8 21v-6l-3-3V6a2 2 0 0 1 2-2h6l5 5v3l-4 3v6z"/><circle cx="12" cy="9" r="1.6"/>',
  WHEEL: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="4"/><path d="M12 3v5m0 8v5M3 12h5m8 0h5"/>',
  PART: '<path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.5-.5-.5-2.5z"/>'
}
const icon = c => I[c] || I.PART

onMounted(() => {
  // Phần 1: Tải danh sách banner
  api.get('/banners').then(r => {
    banners.value = r.data
  }).catch(() => {})

  // Phần 1: Tự động chuyển slide sau mỗi 5.5s
  timer = setInterval(() => {
    if (!pause.value && hero.value.length) {
      cur.value = (cur.value + 1) % hero.value.length
    }
  }, 5500)

  // Phần 2: Tải danh mục động và options từ backend
  loadCatalog()

  // Phần 2: Lấy tổng số lượng sản phẩm để hiển thị trên thẻ USP
  api.get('/products', { params: { size: 1 } })
    .then(r => {
      if (r?.data?.totalElements) totalProducts.value = r.data.totalElements
    })
    .catch(() => {})
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
/* ---------------------------------------------------- */
/* PHẦN 1: Styles Hero & Promo Banner                   */
/* ---------------------------------------------------- */
.hero { max-width: 1180px; margin: 0 auto; padding: 0 24px; }
.slides { position: relative; height: 420px; border-radius: 0 0 12px 12px; overflow: hidden; background: #111; }
.slide { position: absolute; inset: 0; opacity: 0; transition: opacity .25s ease-out; color: #fff; overflow: hidden; }
.slide.on { opacity: 1; z-index: 1; }
.bg { position: absolute; inset: 0; background-size: cover; background-position: center; }
.shade { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,.6) 0%, rgba(0,0,0,.1) 38%, rgba(0,0,0,.2) 60%, rgba(0,0,0,.7) 100%); }
.txt { position: absolute; left: 0; right: 0; top: 90px; text-align: center; }
.txt small { letter-spacing: .3em; text-transform: uppercase; font-size: .75rem; opacity: .85; }
.txt h1 { font-family: var(--head); font-size: 4.2rem; letter-spacing: .14em; font-weight: 600; line-height: 1.05; margin: 8px 0 0; text-transform: uppercase; text-shadow: 0 6px 30px rgba(0,0,0,.5); }
.txt span { display: block; margin-top: 170px; font-size: .95rem; }

.dots { display: flex; justify-content: center; gap: 6px; margin: 12px 0 18px; }
.dots button { width: 8px; height: 8px; border-radius: 50%; border: none; background: #cfcfcf; cursor: pointer; padding: 0; transition: background-color .2s ease-out, width .2s ease-out; }
.dots button.on { background: #555; width: 22px; border-radius: 4px; }

.promo-banner { position: relative; display: grid; grid-template-columns: 1fr 1.2fr .8fr; align-items: center; height: 100px; border-radius: 10px; overflow: hidden; background: #13235a; color: #fff; margin-top: 10px; text-decoration: none; }
.promo-banner img { position: absolute; left: 0; top: 0; height: 100%; width: 42%; object-fit: cover; opacity: .6; mask-image: linear-gradient(90deg, #000 40%, transparent); }
.pb-txt { text-align: center; z-index: 1; }
.pb-txt b { display: block; font-family: var(--head); font-size: 2rem; color: #f6c445; line-height: 1; }
.pb-txt i { font-family: Georgia, serif; font-style: normal; }
.pb-off { z-index: 1; }
.pb-off small { display: block; font-size: .75rem; color: #f6c445; }
.pb-off b { font-family: var(--head); font-size: 1.15rem; color: #fff; line-height: 1.2; }
.pb-moon { position: absolute; right: 34px; top: 13px; }

/* ---------------------------------------------------- */
/* PHẦN 2: Styles Danh mục (Categories) & Cam kết (USP)  */
/* ---------------------------------------------------- */
.block { margin-top: 24px; }
.block h2 { font-size: 1.15rem; color: #5a5a5f; margin-bottom: 10px; }
.cats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.cat { display: flex; justify-content: space-between; align-items: center; height: 42px; padding: 0 14px; border-radius: 8px; background: var(--tile-2); color: #3a3a3f; font-size: .88rem; font-weight: 500; transition: .2s; text-decoration: none; }
.cat:hover { background: var(--bronze); color: #fff; }

.usp { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-top: 18px; }
.usp div { background: var(--bronze-soft); border-radius: 10px; padding: 12px 16px; display: flex; flex-direction: column; }
.usp b { font-family: var(--head); font-size: 1.35rem; color: var(--bronze-3); }
.usp span { font-size: .8rem; color: var(--text-2); }

@media (max-width: 860px) {
  .txt h1 { font-size: 2.6rem; }
  .cats, .usp { grid-template-columns: repeat(2, 1fr); }
}
</style>
