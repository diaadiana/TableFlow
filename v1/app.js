'use strict';

const API = '';

/* ── Color maps ─────────────────────────────────────────── */
const TYPE_COLORS = {
  ZI_DE_NASTERE: { bar: '#F59E0B', bg: '#FEF3C7', text: '#92400E', label: 'Zi de naștere' },
  FAMILIE:       { bar: '#10B981', bg: '#D1FAE5', text: '#065F46', label: 'Familie' },
  PRIETENI:      { bar: '#3B82F6', bg: '#DBEAFE', text: '#1E40AF', label: 'Prieteni' },
  INTALNIRE:     { bar: '#8B5CF6', bg: '#EDE9FE', text: '#5B21B6', label: 'Întâlnire' },
};

const STATUS_STYLES = {
  FREE:     { fill: '#D1FAE5', stroke: '#34D399', w: 1.5 },
  PARTIAL:  { fill: '#FEF3C7', stroke: '#F59E0B', w: 1.5 },
  OCCUPIED: { fill: '#FEF3C7', stroke: '#F59E0B', w: 1.5 },
};

const ACCENT = '#C2643A';

/* ── Table layout config (center x, center y, seats) ──── */
const TABLE_LAYOUT = [
  { id: 1, cx: 140, cy: 160, seats: 3, zone: 'INTERIOR' },
  { id: 2, cx: 310, cy: 160, seats: 2, zone: 'INTERIOR' },
  { id: 3, cx: 140, cy: 310, seats: 4, zone: 'INTERIOR' },
  { id: 4, cx: 310, cy: 310, seats: 4, zone: 'INTERIOR' },
  { id: 5, cx: 580, cy: 160, seats: 4, zone: 'EXTERIOR' },
  { id: 6, cx: 750, cy: 160, seats: 5, zone: 'EXTERIOR' },
  { id: 7, cx: 665, cy: 310, seats: 6, zone: 'EXTERIOR' },
];

const DAYS = ['Duminică','Luni','Marți','Miercuri','Joi','Vineri','Sâmbătă'];
const MONTHS = ['Ianuarie','Februarie','Martie','Aprilie','Mai','Iunie',
  'Iulie','August','Septembrie','Octombrie','Noiembrie','Decembrie'];

/* ── State ──────────────────────────────────────────────── */
let currentDate   = new Date();
let selectedTable = null;
let tablesData    = [];
let reservations  = []; // All reservations for current filtered date (or all)
let currentView   = 'dashboard';

/* ── DOM ────────────────────────────────────────────────── */
const $ = id => document.getElementById(id);

/* ── Init ───────────────────────────────────────────────── */
(function init() {
  renderDateTitle();
  loadData();
  
  $('datePrev').onclick  = () => { currentDate.setDate(currentDate.getDate()-1); handleDateChange(); };
  $('dateNext').onclick  = () => { currentDate.setDate(currentDate.getDate()+1); handleDateChange(); };
  $('dateToday').onclick = () => { currentDate = new Date(); handleDateChange(); };
  
  $('detailClose').onclick = closePanel;
  $('btnCancel').onclick   = closePanel;
  
  // Navigation
  $('nav-dashboard').onclick    = () => switchView('dashboard');
  $('nav-reservations').onclick = () => switchView('reservations');
  $('nav-tables').onclick       = () => switchView('tables');
  $('nav-clients').onclick      = () => switchView('clients');

  // Escape key to close panel
  window.addEventListener('keydown', e => {
    if (e.key === 'Escape') closePanel();
  });
})();

function handleDateChange() {
  renderDateTitle();
  loadData();
}

function switchView(view) {
  currentView = view;
  document.querySelectorAll('.nav-item').forEach(n => {
    n.classList.toggle('active', n.id === `nav-${view}`);
  });
  closePanel();
  render();
}

/* ── Data ───────────────────────────────────────────────── */
async function loadData() {
  try {
    const [tRes, rRes] = await Promise.all([
      fetch(`${API}/api/tables?datetime=${encodeURIComponent(fmtISO(new Date(), new Date().getHours(), 0))}`),
      fetch(`${API}/api/reservations`),
    ]);
    tablesData = await tRes.json();
    const allRes = await rRes.json();
    
    // Filter reservations for current day
    const curDateStr = fmtISO(currentDate, 0, 0).split('T')[0];
    reservations = allRes.filter(r => r.dataOra.startsWith(curDateStr));
    
    // Cache all reservations in state if needed for clients (we use allRes for clients)
    window.allReservations = allRes;
  } catch { 
    tablesData = []; 
    reservations = []; 
    window.allReservations = [];
  }
  render();
  renderResListSidebar();
}

function render() {
  const container = document.querySelector('.main-scroll');
  const timeRuler = $('timeRuler');
  const floorPlan = $('floorPlan');
  
  if (currentView === 'dashboard') {
    timeRuler.style.display = 'block';
    floorPlan.style.display = 'flex';
    renderTimeRuler();
    renderFloorPlan();
    const dyn = container.querySelector('.dynamic-content');
    if (dyn) dyn.remove();
  } else {
    timeRuler.style.display = 'none';
    floorPlan.style.display = 'none';
    
    let dyn = container.querySelector('.dynamic-content');
    if (!dyn) {
      dyn = document.createElement('div');
      dyn.className = 'dynamic-content view-container';
      container.appendChild(dyn);
    }
    
    if (currentView === 'reservations') renderReservations(dyn);
    else if (currentView === 'tables') renderTables(dyn);
    else if (currentView === 'clients') renderClients(dyn);
  }
}

function renderDateTitle() {
  const d = currentDate;
  $('dateTitle').textContent =
    `${DAYS[d.getDay()]}, ${d.getDate()} ${MONTHS[d.getMonth()]} ${d.getFullYear()}`;
}

function renderTimeRuler() {
  const el = $('timeRuler');
  el.innerHTML = '';
  for (let h = 10; h <= 23; h++) {
    const pct = ((h - 10) / 13) * 100;
    const tick = document.createElement('div');
    tick.className = 'time-tick';
    tick.style.left = pct + '%';
    tick.textContent = String(h).padStart(2,'0') + ':00';
    el.appendChild(tick);
  }
  const now = new Date();
  const nowH = now.getHours() + now.getMinutes() / 60;
  if (nowH >= 10 && nowH <= 23) {
    const pct = ((nowH - 10) / 13) * 100;
    const line = document.createElement('div');
    line.className = 'time-now-line';
    line.style.left = pct + '%';
    line.innerHTML =
      `<div class="time-now-dot"></div>` +
      `<div class="time-now-label">${pad2(now.getHours())}:${pad2(now.getMinutes())}</div>`;
    el.appendChild(line);
  }
}

/* ── Floor Plan SVG Architectural tables ─────────────────── */

function svgEl(tag, attrs) {
  const NS = 'http://www.w3.org/2000/svg';
  const el = document.createElementNS(NS, tag);
  Object.entries(attrs).forEach(([k,v]) => el.setAttribute(k, v));
  return el;
}

function getArcPath(cx, cy, r, angle) {
  const rad = angle * Math.PI / 180;
  const a1 = rad - (80 * Math.PI / 180);
  const a2 = rad + (80 * Math.PI / 180);
  const x1 = cx + r * Math.cos(a1);
  const y1 = cy + r * Math.sin(a1);
  const x2 = cx + r * Math.cos(a2);
  const y2 = cy + r * Math.sin(a2);
  return `M ${x1} ${y1} A ${r} ${r} 0 0 1 ${x2} ${y2}`;
}

function drawChair(g, cx, cy, r, angle) {
  // Chair circle
  g.appendChild(svgEl('circle', {
    cx, cy, r,
    fill: '#FFFFFF',
    stroke: '#9CA3AF',
    'stroke-width': 1.2
  }));
  // Backrest arc (160 deg centered on angle)
  g.appendChild(svgEl('path', {
    d: getArcPath(cx, cy, r + 3, angle),
    fill: 'none',
    stroke: '#9CA3AF',
    'stroke-width': 1.5
  }));
}

function renderTableLabels(g, cx, cy, id, remaining, total) {
  const mx = svgEl('text', {
    x: cx, y: cy,
    'text-anchor': 'middle',
    'dominant-baseline': 'middle',
    'font-family': 'Inter, sans-serif',
    'font-size': '11',
    'font-weight': '600',
    fill: '#374151'
  });
  mx.textContent = `M${id}`;
  g.appendChild(mx);
  
  const sub = svgEl('text', {
    x: cx, y: cy + 14,
    'text-anchor': 'middle',
    'font-size': '9',
    'font-weight': '400',
    fill: '#6B7280'
  });
  sub.textContent = `${remaining ?? total}/${total}`;
  g.appendChild(sub);
}

function renderTableSVG(cfg, api, isSelected) {
  const status = api.status || 'FREE';
  const st = STATUS_STYLES[status];
  const fill   = isSelected ? '#FFFFFF' : st.fill;
  const stroke = isSelected ? ACCENT : st.stroke;
  const sw     = isSelected ? 2 : st.w;

  const g = svgEl('g', {});
  const capacity = cfg.seats;

  if (capacity === 2) {
    // 100x100 Circle table
    g.appendChild(svgEl('circle', { cx: 50, cy: 50, r: 28, fill, stroke, 'stroke-width': sw }));
    drawChair(g, 8, 50, 10, 180);
    drawChair(g, 92, 50, 10, 0);
    renderTableLabels(g, 50, 50, cfg.id, api.locuriRamase, cfg.seats);
    return { g, w: 100, h: 100 };
  } else if (capacity === 3) {
    // 100x100 Circle table (3 seats)
    g.appendChild(svgEl('circle', { cx: 50, cy: 50, r: 28, fill, stroke, 'stroke-width': sw }));
    const angles = [-90, 30, 150];
    angles.forEach(angle => {
      const rad = angle * Math.PI / 180;
      const cx = 50 + 42 * Math.cos(rad);
      const cy = 50 + 42 * Math.sin(rad);
      drawChair(g, cx, cy, 10, angle);
    });
    renderTableLabels(g, 50, 50, cfg.id, api.locuriRamase, cfg.seats);
    return { g, w: 100, h: 100 };
  } else if (capacity === 4) {
    // 130x110 Rect table
    g.appendChild(svgEl('rect', { x: 25, y: 20, width: 80, height: 70, rx: 8, fill, stroke, 'stroke-width': sw }));
    drawChair(g, 65, 8, 10, -90);
    drawChair(g, 65, 102, 10, 90);
    drawChair(g, 12, 55, 10, 180);
    drawChair(g, 118, 55, 10, 0);
    renderTableLabels(g, 65, 55, cfg.id, api.locuriRamase, cfg.seats);
    return { g, w: 130, h: 110 };
  } else if (capacity === 5) {
    // 150x150 Circle table (5 seats)
    g.appendChild(svgEl('circle', { cx: 75, cy: 75, r: 38, fill, stroke, 'stroke-width': sw }));
    const angles = [-90, -18, 54, 126, 198];
    angles.forEach(angle => {
      const rad = angle * Math.PI / 180;
      const cx = 75 + 56 * Math.cos(rad);
      const cy = 75 + 56 * Math.sin(rad);
      drawChair(g, cx, cy, 11, angle);
    });
    renderTableLabels(g, 75, 75, cfg.id, api.locuriRamase, cfg.seats);
    return { g, w: 150, h: 150 };
  } else if (capacity === 6) {
    // 150x150 Circle table
    g.appendChild(svgEl('circle', { cx: 75, cy: 75, r: 38, fill, stroke, 'stroke-width': sw }));
    for (let i = 0; i < 6; i++) {
        const angle = -90 + i * 60;
        const rad = angle * Math.PI / 180;
        const cx = 75 + 56 * Math.cos(rad);
        const cy = 75 + 56 * Math.sin(rad);
        drawChair(g, cx, cy, 11, angle);
    }
    renderTableLabels(g, 75, 75, cfg.id, api.locuriRamase, cfg.seats);
    return { g, w: 150, h: 150 };
  }
}

function renderFloorPlan() {
  const wrap = $('floorPlan');
  const NS = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(NS, 'svg');
  svg.setAttribute('viewBox', '0 0 880 420');
  svg.setAttribute('fill', 'none');

  svg.innerHTML = `
    <rect x="20" y="20" width="410" height="380" rx="8"
          fill="none" stroke="#111827" stroke-dasharray="6,4" />
    <text x="225" y="50" text-anchor="middle"
          font-size="11" fill="#111827" font-weight="600"
          letter-spacing="0.06em">ZONA INTERIOAR&#258;</text>
    <text x="225" y="66" text-anchor="middle"
          font-size="10" fill="#111827">Sala Principal&#259;</text>
    <rect x="450" y="20" width="410" height="380" rx="8"
          fill="none" stroke="#111827" stroke-dasharray="6,4" />
    <text x="665" y="50" text-anchor="middle"
          font-size="11" fill="#111827" font-weight="600"
          letter-spacing="0.06em">TERAS&#258; CU GR&#258;DIN&#258;</text>
    <text x="665" y="66" text-anchor="middle"
          font-size="10" fill="#111827">Gr&#259;din&#259; &#537;i Teras&#259;</text>
  `;

  TABLE_LAYOUT.forEach(cfg => {
    const api = tablesData.find(t => t.id === cfg.id) || {};
    const isSelected = selectedTable === cfg.id;
    const tableData = renderTableSVG(cfg, api, isSelected);
    
    const tableG = tableData.g;
    tableG.setAttribute('class', 'table-group');
    tableG.setAttribute('transform', `translate(${cfg.cx - tableData.w/2}, ${cfg.cy - tableData.h/2})`);
    tableG.onclick = () => selectTable(cfg.id);
    
    svg.appendChild(tableG);
  });

  wrap.innerHTML = '';
  wrap.appendChild(svg);
}

function renderReservations(el) {
  el.innerHTML = `
    <div class="view-header">
      <h2 class="view-title">Listă Rezervări</h2>
      <button class="btn-primary" style="flex:none; width:auto; border-radius:6px; padding:8px 16px; font-weight:500;" id="btnNewRes">+ Rezervare Nouă</button>
    </div>
    <table class="data-table">
      <thead>
        <tr>
          <th>#</th>
          <th>Client</th>
          <th>Masa</th>
          <th>Amplasare</th>
          <th>Persoane</th>
          <th>Data & Ora</th>
          <th>Specific</th>
        </tr>
      </thead>
      <tbody id="resTableBody"></tbody>
    </table>
    <div id="resEmpty" class="empty-state" style="display:none">Nicio rezervare pentru această zi.</div>
  `;
  
  $('btnNewRes').onclick = () => openCreatePanel();
  
  const tbody = $('resTableBody');
  if (!reservations.length) {
    if ($('resEmpty')) $('resEmpty').style.display = 'block';
    return;
  }
  
  reservations.forEach((r, idx) => {
    const dt = new Date(r.dataOra);
    const dtStr = isNaN(dt) ? r.dataOra : `${pad2(dt.getDate())} ${MONTHS[dt.getMonth()].slice(0,3)} ${dt.getFullYear()}, ${pad2(dt.getHours())}:${pad2(dt.getMinutes())}`;
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${idx + 1}</td>
      <td><strong>${esc(r.numeClient)}</strong></td>
      <td>Masa ${r.masaId}</td>
      <td><span class="pill pill-slate">${r.amplasare}</span></td>
      <td>${r.nrPersoane}</td>
      <td>${dtStr}</td>
      <td><span class="pill ${pillClass(r.specific)}">${TYPE_COLORS[r.specific]?.label || r.specific}</span></td>
    `;
    tr.onclick = () => selectTable(r.masaId);
    tbody.appendChild(tr);
  });
}

function renderTables(el) {
  el.innerHTML = `
    <div class="view-header">
      <h2 class="view-title">Inventar Mese</h2>
    </div>
    <div class="grid-container">
      ${TABLE_LAYOUT.map(cfg => {
        const api = tablesData.find(t => t.id === cfg.id) || {};
        const isOccupied = api.status === 'OCCUPIED' || api.status === 'PARTIAL';
        return `
          <div class="table-inventory-card">
            <div class="table-card-row">
              <span class="table-card-id">Masa ${cfg.id}</span>
              <span class="pill ${cfg.zone === 'INTERIOR' ? 'pill-slate' : 'pill-sky'}">${cfg.zone}</span>
            </div>
            <div style="color:var(--text-secondary); font-size:14px;">Capacitate: ${api.capacity || cfg.seats} locuri</div>
            <div>
              <span class="pill ${isOccupied ? 'pill-amber' : 'pill-green'}">${isOccupied ? 'Ocupată' : 'Liberă'}</span>
            </div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

function renderClients(el) {
  const all = window.allReservations || [];
  const clientsMap = new Map();
  all.forEach(r => {
    if (!clientsMap.has(r.numeClient)) {
      clientsMap.set(r.numeClient, { name: r.numeClient, count: 0, lastDate: null });
    }
    const c = clientsMap.get(r.numeClient);
    c.count++;
    const dt = new Date(r.dataOra);
    if (!c.lastDate || dt > c.lastDate) c.lastDate = dt;
  });
  
  const clients = Array.from(clientsMap.values()).sort((a,b) => b.lastDate - a.lastDate);
  
  el.innerHTML = `
    <div class="view-header">
      <h2 class="view-title">Clienți</h2>
    </div>
    <div class="client-list">
      ${clients.length ? clients.map(c => {
        const initials = c.name.split(' ').map(w => w[0]).join('').slice(0,2).toUpperCase();
        const lastStr = c.lastDate ? `${pad2(c.lastDate.getDate())} ${MONTHS[c.lastDate.getMonth()]} ${c.lastDate.getFullYear()}` : '—';
        const color = stringToColor(c.name);
        return `
          <div class="client-row">
            <div class="res-avatar" style="background:${color.bg}; color:${color.text}">${initials}</div>
            <div class="client-row-content">
              <div style="font-weight:600;">${esc(c.name)}</div>
              <div class="client-meta">${c.count} rezervări &middot; Ultima: ${lastStr}</div>
            </div>
            <div class="client-chevron">&rsaquo;</div>
          </div>
        `;
      }).join('') : '<div class="empty-state">Niciun client înregistrat.</div>'}
    </div>
  `;
}

function selectTable(id) {
  if (selectedTable === id && $('detailPanel').classList.contains('open')) { closePanel(); return; }
  selectedTable = id;
  if (currentView === 'dashboard') renderFloorPlan();

  const api  = tablesData.find(t => t.id === id) || {};
  const cfg  = TABLE_LAYOUT.find(t => t.id === id);
  const loc  = cfg.zone === 'INTERIOR' ? 'Interior' : 'Exterior';
  const res  = reservations.find(r => r.masaId === id);

  $('detailTitle').textContent  = `Masa ${id} \u00B7 ${loc}`;
  $('detailCaption').textContent = `${api.capacity || cfg.seats} locuri`;

  const body = $('detailBody');
  body.innerHTML = '';

  if (res) {
    const specColors = TYPE_COLORS[res.specific] || {};
    const remaining  = api.locuriRamase ?? '—';
    const remClass   = remaining <= 0 ? 'pill-red' : remaining <= 2 ? 'pill-amber' : 'pill-green';

    const dt = new Date(res.dataOra);
    const dtStr = isNaN(dt) ? res.dataOra :
      `${pad2(dt.getDate())} ${MONTHS[dt.getMonth()].slice(0,3)} ${dt.getFullYear()}, ${pad2(dt.getHours())}:${pad2(dt.getMinutes())}`;

    body.innerHTML = `
      ${propRow('Client', `<strong>${esc(res.numeClient)}</strong>`)}
      ${propRow('Persoane', res.nrPersoane)}
      ${propRow('Data', dtStr)}
      ${propRow('Specific', `<span class="pill ${pillClass(res.specific)}">${specColors.label || res.specific}</span>`)}
      ${propRow('Amplasare', `<span class="pill pill-slate">${res.amplasare}</span>`)}
      ${propRow('Locuri rămase', `<span class="pill ${remClass}">${remaining}</span>`)}
      <div class="detail-note">
        <label>Note</label>
        <textarea placeholder="Nicio notă...">${res.specific === 'ZI_DE_NASTERE' ? 'Aniversare \u2014 tort surpriză, loc liniștit' : ''}</textarea>
      </div>`;
    
    const confirmBtn = $('btnConfirm');
    confirmBtn.textContent = 'Confirmă';
    confirmBtn.onclick = closePanel;
  } else {
    body.innerHTML = `
      <div class="empty-state" style="padding-top:20px;">Masă disponibilă.</div>
      <div style="display:flex; justify-content:center; margin-top:16px;">
        <button class="btn-primary" style="flex:none; padding:10px 24px;" id="btnAddResPlan">+ Adaugă Rezervare</button>
      </div>
    `;
    $('btnAddResPlan').onclick = () => openCreatePanel(id);
    const confirmBtn = $('btnConfirm');
    confirmBtn.textContent = 'Confirmă';
    confirmBtn.onclick = closePanel;
  }

  $('detailPanel').classList.add('open');
}

function openCreatePanel(tableId = null) {
  $('detailTitle').textContent = tableId ? `Rezervare Nouă · Masa ${tableId}` : 'Rezervare Nouă';
  $('detailCaption').textContent = 'Introduceți detaliile clientului';
  
  const now = new Date();
  const dateVal = `${now.getFullYear()}-${pad2(now.getMonth()+1)}-${pad2(now.getDate())}T${pad2(now.getHours())}:00`;

  const body = $('detailBody');
  body.innerHTML = `
    <div style="display:flex; flex-direction:column; gap:16px;">
      <div class="detail-note" style="margin-top:0">
        <label>Nume Client</label>
        <input type="text" id="inNume" class="detail-input" placeholder="ex: Ion Popescu" style="width:100%; padding:10px; border:1px solid var(--border); border-radius:6px; outline:none; font-family:var(--font);">
      </div>
      <div style="display:grid; grid-template-columns: 1fr 1fr; gap:12px;">
        <div class="detail-note" style="margin-top:0">
          <label>Persoane</label>
          <input type="number" id="inPers" class="detail-input" value="2" min="1" max="10" style="width:100%; padding:10px; border:1px solid var(--border); border-radius:6px; outline:none; font-family:var(--font);">
        </div>
        <div class="detail-note" style="margin-top:0">
          <label>Amplasare</label>
          <select id="inLoc" class="detail-input" style="width:100%; padding:10px; border:1px solid var(--border); border-radius:6px; outline:none; font-family:var(--font); height:38px;">
            <option value="INTERIOR" ${tableId && TABLE_LAYOUT.find(t=>t.id===tableId).zone==='INTERIOR' ? 'selected' : ''}>Interior</option>
            <option value="EXTERIOR" ${tableId && TABLE_LAYOUT.find(t=>t.id===tableId).zone==='EXTERIOR' ? 'selected' : ''}>Exterior</option>
          </select>
        </div>
      </div>
      <div class="detail-note" style="margin-top:0">
        <label>Data & Ora</label>
        <input type="datetime-local" id="inData" value="${dateVal}" style="width:100%; padding:10px; border:1px solid var(--border); border-radius:6px; outline:none; font-family:var(--font);">
      </div>
      <div class="detail-note" style="margin-top:0">
        <label>Specificul Rezervării</label>
        <select id="inSpec" style="width:100%; padding:10px; border:1px solid var(--border); border-radius:6px; outline:none; font-family:var(--font); height:38px;">
          <option value="FAMILIE">Familie</option>
          <option value="PRIETENI">Prieteni</option>
          <option value="INTALNIRE">Întâlnire</option>
          <option value="ZI_DE_NASTERE">Zi de naștere</option>
        </select>
      </div>
    </div>
  `;
  
  const confirmBtn = $('btnConfirm');
  confirmBtn.textContent = 'Confirmă Rezervarea';
  confirmBtn.onclick = submitReservation;
  
  $('detailPanel').classList.add('open');
}

async function submitReservation() {
  const data = {
    numeClient: $('inNume').value,
    nrPersoane: parseInt($('inPers').value),
    dataOra: $('inData').value + ':00',
    amplasare: $('inLoc').value,
    specific: $('inSpec').value
  };
  
  if (!data.numeClient) { alert('Vă rugăm introduceți numele clientului.'); return; }

  try {
    const res = await fetch(`${API}/api/reservation`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    const result = await res.json();
    if (result.success) {
      closePanel();
      loadData();
    } else {
      alert(result.message || 'Eroare la crearea rezervării.');
    }
  } catch {
    alert('Eroare de conexiune la server.');
  }
}

function closePanel() {
  selectedTable = null;
  $('detailPanel').classList.remove('open');
  if (currentView === 'dashboard') renderFloorPlan();
}

function propRow(label, value) {
  return `<div class="prop-row"><span class="prop-label">${label}</span><span class="prop-value">${value}</span></div>`;
}

function pillClass(specific) {
  return { ZI_DE_NASTERE:'pill-amber', FAMILIE:'pill-green', PRIETENI:'pill-blue', INTALNIRE:'pill-purple' }[specific] || 'pill-slate';
}

function stringToColor(str) {
  let hash = 0;
  for (let i = 0; i < str.length; i++) hash = str.charCodeAt(i) + ((hash << 5) - hash);
  const h = Math.abs(hash) % 360;
  return { bg: `hsl(${h}, 40%, 95%)`, text: `hsl(${h}, 60%, 30%)` };
}

function renderResListSidebar() {
  const el = $('resList');
  $('resCount').textContent = reservations.length;

  if (!reservations.length) {
    el.innerHTML = '<div class="empty-state">Nicio rezervare pentru această zi.</div>';
    return;
  }
  el.innerHTML = '';
  reservations.forEach(r => {
    const tc = TYPE_COLORS[r.specific] || { bar:'#9CA3AF', bg:'#F3F4F6', text:'#374151' };
    const initials = r.numeClient.split(' ').map(w => w[0]).join('').slice(0,2).toUpperCase();
    const dt = new Date(r.dataOra);
    const timeStr = isNaN(dt) ? '' : `${pad2(dt.getHours())}:${pad2(dt.getMinutes())}`;

    const card = document.createElement('div');
    card.className = 'res-card';
    card.innerHTML = `
      <div class="res-bar" style="background:${tc.bar}"></div>
      <div class="res-avatar" style="background:${tc.bg};color:${tc.text}">${initials}</div>
      <div class="res-info">
        <div class="res-name">${esc(r.numeClient)}</div>
        <div class="res-details">Masa ${r.masaId} &middot; ${timeStr}</div>
      </div>
      <div class="res-badge">${r.nrPersoane} pers.</div>`;
    card.style.cursor = 'pointer';
    card.onclick = () => {
      if (currentView !== 'dashboard') switchView('dashboard');
      selectTable(r.masaId);
    };
    el.appendChild(card);
  });
}

function pad2(n) { return String(n).padStart(2, '0'); }
function esc(s) { return String(s||'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
function fmtISO(d, h, m) {
  return `${d.getFullYear()}-${pad2(d.getMonth()+1)}-${pad2(d.getDate())}T${pad2(h)}:${pad2(m)}:00`;
}
