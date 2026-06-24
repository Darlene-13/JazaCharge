const API = 'http://localhost:8080/api';
const REFRESH_INTERVAL = 10000;

let allReservations = [];
let activeFilter = 'ALL';

// ── INIT ──
document.addEventListener('DOMContentLoaded', () => {
    setupNav();
    setupFilters();
    loadAll();
    setInterval(loadAll, REFRESH_INTERVAL);
});

// ── NAVIGATION ──
function setupNav() {
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', e => {
            e.preventDefault();
            const section = item.dataset.section;
            document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
            document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
            item.classList.add('active');
            document.getElementById(`section-${section}`).classList.add('active');
            document.getElementById('page-title').textContent =
                section.charAt(0).toUpperCase() + section.slice(1);
        });
    });
}

// ── FILTERS ──
function setupFilters() {
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            activeFilter = btn.dataset.filter;
            renderReservationsTable(allReservations);
        });
    });
}

// ── LOAD ALL ──
function loadAll() {
    const icon = document.getElementById('refresh-icon');
    icon.style.display = 'inline-block';
    icon.style.animation = 'spin 0.6s linear infinite';

    Promise.all([loadStations(), loadReservations(), loadStats()]).finally(() => {
        icon.style.animation = '';
        updateTimestamp();
    });
}

function updateTimestamp() {
    document.getElementById('last-updated').textContent =
        new Date().toLocaleTimeString('en-KE', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

// ── STATS ──
async function loadStats() {
    try {
        const [stations, reservations, riders] = await Promise.all([
            fetch(`${API}/stations`).then(r => r.json()),
            fetch(`${API}/reservations`).then(r => r.json()),
            fetch(`${API}/riders`).then(r => r.json()),
        ]);

        const activeStations = stations.filter(s => s.isActive).length;
        const totalBatteries = stations.reduce((sum, s) => sum + (s.availableBatteries || 0), 0);
        const activeRes = reservations.filter(r => r.status === 'ACTIVE').length;
        const riderCount = Array.isArray(riders)
            ? riders.length
            : (riders.totalElements ?? riders.content?.length ?? '--');

        animateCount('total-stations', activeStations);
        animateCount('total-batteries', totalBatteries);
        animateCount('total-reservations', activeRes);
        animateCount('total-riders', riderCount);

        document.getElementById('stations-online').textContent = `${activeStations} online`;
        document.getElementById('reservations-count').textContent = `${activeRes} active`;

    } catch (e) {
        console.error('Stats error:', e);
    }
}

function animateCount(id, target) {
    if (typeof target !== 'number') {
        document.getElementById(id).textContent = target;
        return;
    }
    const el = document.getElementById(id);
    const start = parseInt(el.textContent) || 0;
    const diff = target - start;
    const duration = 400;
    const startTime = performance.now();
    function step(now) {
        const t = Math.min((now - startTime) / duration, 1);
        el.textContent = Math.round(start + diff * easeOut(t));
        if (t < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
}

function easeOut(t) { return 1 - Math.pow(1 - t, 3); }

// ── STATIONS ──
async function loadStations() {
    try {
        const stations = await fetch(`${API}/stations`).then(r => r.json());
        renderStationsOverview(stations);
        renderStationsTable(stations);
    } catch (e) {
        document.getElementById('stations-grid').innerHTML =
            '<div style="padding:24px;color:var(--text-3);text-align:center">Could not reach backend</div>';
    }
}

function stationLevel(s) {
    const pct = ((s.availableBatteries || 0) / 10) * 100;
    if (pct === 0) return 'critical';
    if (pct <= 30) return 'low';
    return 'ok';
}

function renderStationsOverview(stations) {
    const el = document.getElementById('stations-grid');
    if (!stations.length) {
        el.innerHTML = '<div style="padding:24px;color:var(--text-3);text-align:center">No stations found</div>';
        return;
    }
    el.innerHTML = stations.map(s => {
        const pct = Math.min(100, Math.round(((s.availableBatteries || 0) / 10) * 100));
        const lvl = stationLevel(s);
        return `
    <div class="station-row ${lvl}">
      <span class="srow-status ${s.isActive ? lvl : 'offline'}"></span>
      <div class="srow-info">
        <div class="srow-name">${s.name}</div>
        <div class="srow-loc">📍 ${s.location}</div>
      </div>
      <div class="srow-bar-wrap">
        <div class="srow-bar ${lvl}" style="width:${pct}%"></div>
      </div>
      <div class="srow-count">${s.availableBatteries ?? 0}</div>
    </div>`;
    }).join('');
}

function renderStationsTable(stations) {
    const tbody = document.getElementById('stations-tbody');
    if (!stations.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-cell">No stations</td></tr>';
        return;
    }
    tbody.innerHTML = stations.map(s => {
        const pct = Math.min(100, Math.round(((s.availableBatteries || 0) / 10) * 100));
        const lvl = stationLevel(s);
        return `
    <tr>
      <td class="td-name">${s.name}</td>
      <td>${s.location}</td>
      <td class="td-mono">${s.availableBatteries ?? 0}
        <span class="mini-bar-wrap"><span class="mini-bar ${lvl}" style="width:${pct}%"></span></span>
      </td>
      <td class="td-mono">${s.activeReservations ?? 0}</td>
      <td><span class="pill ${s.isActive ? 'ACTIVE' : 'EXPIRED'}">${s.isActive ? 'Online' : 'Offline'}</span></td>
      <td class="td-mono">${pct}%</td>
    </tr>`;
    }).join('');
}

// ── RESERVATIONS ──
async function loadReservations() {
    try {
        const reservations = await fetch(`${API}/reservations`).then(r => r.json());
        allReservations = reservations.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        renderReservationsFeed(allReservations);
        renderReservationsTable(allReservations);
    } catch (e) {
        document.getElementById('reservations-list').innerHTML =
            '<div style="padding:24px;color:var(--text-3);text-align:center">Could not load reservations</div>';
    }
}

function renderReservationsFeed(reservations) {
    const el = document.getElementById('reservations-list');
    const recent = reservations.slice(0, 15);
    if (!recent.length) {
        el.innerHTML = '<div style="padding:24px;color:var(--text-3);text-align:center">No reservations yet</div>';
        return;
    }
    el.innerHTML = recent.map(r => {
        const time = r.createdAt
            ? new Date(r.createdAt).toLocaleTimeString('en-KE', { hour: '2-digit', minute: '2-digit' })
            : '--';
        const phone = r.rider?.phoneNumber || r.phoneNumber || '--';
        const station = r.station?.name || r.stationName || '--';
        return `
    <div class="feed-item">
      <div class="feed-left">
        <div class="feed-code">${r.reservationCode || '--'}</div>
        <div class="feed-phone">${phone}</div>
        <div class="feed-station">${station}</div>
      </div>
      <div class="feed-right">
        <span class="pill ${r.status}">${r.status}</span>
        <span class="feed-time">${time}</span>
      </div>
    </div>`;
    }).join('');
}

function renderReservationsTable(reservations) {
    const tbody = document.getElementById('reservations-tbody');
    const filtered = activeFilter === 'ALL'
        ? reservations
        : reservations.filter(r => r.status === activeFilter);

    if (!filtered.length) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-cell">No ${activeFilter === 'ALL' ? '' : activeFilter.toLowerCase() + ' '}reservations</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(r => {
        const created = r.createdAt ? new Date(r.createdAt).toLocaleString('en-KE') : '--';
        const expires = r.expiresAt ? new Date(r.expiresAt).toLocaleString('en-KE') : '--';
        const phone = r.rider?.phoneNumber || r.phoneNumber || '--';
        const station = r.station?.name || r.stationName || '--';
        const battery = r.battery?.batteryType || r.batteryType || '--';
        return `
    <tr>
      <td class="td-mono">${r.reservationCode || '--'}</td>
      <td class="td-mono">${phone}</td>
      <td>${station}</td>
      <td class="td-mono">${battery}</td>
      <td class="td-mono" style="font-size:11px">${created}</td>
      <td class="td-mono" style="font-size:11px">${expires}</td>
      <td><span class="pill ${r.status}">${r.status}</span></td>
    </tr>`;
    }).join('');
}

// ── SMS LOG ──
function addSmsLogEntry(phone, message, action, success) {
    const tbody = document.getElementById('sms-log-body');
    const emptyRow = tbody.querySelector('td[colspan]');
    if (emptyRow) emptyRow.closest('tr').remove();

    const time = new Date().toLocaleTimeString('en-KE');
    const row = document.createElement('tr');
    row.innerHTML = `
    <td class="td-mono" style="font-size:11px">${time}</td>
    <td class="td-mono">${phone}</td>
    <td>${message}</td>
    <td>${action}</td>
    <td class="${success ? 'result-ok' : 'result-fail'}">${success ? '✓ Sent' : '✗ Failed'}</td>`;
    tbody.prepend(row);

    const badge = document.getElementById('sms-count');
    const n = parseInt(badge.textContent) || 0;
    badge.textContent = `${n + 1} messages`;
}

// Spin animation for refresh button
const style = document.createElement('style');
style.textContent = '@keyframes spin { to { transform: rotate(360deg); } }';
document.head.appendChild(style);