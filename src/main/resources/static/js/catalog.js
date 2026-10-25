(() => {
  const form = document.getElementById('filters');
  if (!form) return;
  const query = document.getElementById('q');
  const suggestions = document.getElementById('suggestions');
  let timer, request;
  query.addEventListener('input', () => {
    clearTimeout(timer);
    request?.abort();
    suggestions.replaceChildren();
    if (query.value.trim().length < 2) return;
    timer = setTimeout(async () => {
      const current = new AbortController(); request = current;
      try {
        const response = await fetch('/api/products/suggest?q=' + encodeURIComponent(query.value.trim()), { signal: current.signal });
        if (!response.ok) return;
        const rows = await response.json();
        if (current.signal.aborted) return;
        suggestions.replaceChildren(...rows.map(row => {
          const option = document.createElement('option'); option.value = row.name; return option;
        }));
      } catch { /* Search remains usable when suggestions are unavailable. */ }
    }, 250);
  });
  form.addEventListener('submit', event => {
    const min = form.elements.minPrice.value, max = form.elements.maxPrice.value;
    const invalid = min !== '' && max !== '' && Number(min) > Number(max);
    const error = document.getElementById('filter-error');
    error.hidden = !invalid;
    if (invalid) { event.preventDefault(); error.textContent = 'Giá từ phải nhỏ hơn hoặc bằng giá đến.'; }
  });
})();
