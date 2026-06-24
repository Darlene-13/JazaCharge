
const API = 'https://elimination-cordless-checks-ensure.trycloudflare.com/api';
const REFRESH_INTERVAL = 10000; // 10 seconds

// INIT
document.addEventListener('DOMContentLoaded', () => {
    loadAll();
    setInterval(loadAll, REFRESH_INTERVAL);
});

function loadAll() {
    loadStations();
    loadReservations();
    loadStats();
    updateTimestamp();
}

function updateTimestamp() {
    document.getElementById('last-updated').textContent =
        'Updated ' + new Date().toLocaleTimeString('en-KE');
}

// STATS
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

        document.getElementById('total-stations').textContent = activeStations;
        document.getElementById('total-batteries').textContent = totalBatteries;
        document.getElementById('total-reservations').textContent = activeRes;
        document.getElementById('total-riders').textContent = riders.length || riders.totalElements || '--';
    } catch (e) {
        console.error('Stats error:', e);
    }
}

// STATIONS
async function loadStations() {
    const grid = document.getElementById('stations-grid');
    try {
        const stations = await fetch(`${API}/stations`).then(r => r.json());
        if (!stations.length) {
            grid.innerHTML = '<div class="loading">No stations found</div>';
            return;
        }
        grid.innerHTML = stations.map(renderStation).join('');
    } catch (e) {
        grid.innerHTML = '<div class="loading">Could not load stations</div>';
    }
}

function renderStation(s) {
    const max = 10; // assume max 10 batteries per station for bar display
    const pct = Math.min(100, Math.round(((s.availableBatteries || 0) / max) * 100));
    const level = pct === 0 ? 'empty' : pct <= 30 ? 'low' : '';
    const barClass = pct === 0 ? 'empty' : pct <= 30 ? 'low' : '';

    return `
    <div class="station-card ${level}">
      <div class="station-name">${s.name}</div>
      <div class="station-location">📍 ${s.location}</div>
      <div class="battery-bar-wrap">
        <div class="battery-bar ${barClass}" style="width:${pct}%"></div>
      </div>
      <div class="station-meta">
        <span> ${s.availableBatteries ?? 0} available</span>
        <span>📋 ${s.activeReservations ?? 0} reserved</span>
      </div>
    </div>
  `;
}

// RESERVATIONS
async function loadReservations() {
    const list = document.getElementById('reservations-list');
    const badge = document.getElementById('reservations-count');
    try {
        const reservations = await fetch(`${API}/reservations`).then(r => r.json());
        const active = reservations.filter(r => r.status === 'ACTIVE');
        badge.textContent = active.length;

        if (!reservations.length) {
            list.innerHTML = '<div class="loading">No reservations yet</div>';
            return;
        }

        list.innerHTML = reservations
            .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
            .slice(0, 20)
            .map(renderReservation)
            .join('');
    } catch (e) {
        list.innerHTML = '<div class="loading">Could not load reservations</div>';
    }
}

function renderReservation(r) {
    const time = r.createdAt
        ? new Date(r.createdAt).toLocaleTimeString('en-KE', { hour: '2-digit', minute: '2-digit' })
        : '--';
    return `
    <div class="reservation-item">
      <div>
        <div class="res-code">${r.reservationCode}</div>
        <div class="res-phone">${r.rider?.phoneNumber || r.phoneNumber || '--'}</div>
        <div class="res-station">${r.station?.name || r.stationName || '--'}</div>
      </div>
      <div style="text-align:right">
        <span class="status-pill ${r.status}">${r.status}</span>
        <div style="font-size:0.7rem;color:#9ca3af;margin-top:4px">${time}</div>
      </div>
    </div>
  `;
}

// SMS LOG (called from backend when SMS comes in)
// You can push to this from your Spring Boot SSE endpoint
// or just poll /api/sms-log if you add that endpoint
function addSmsLogEntry(phone, message, action, success) {
    const tbody = document.getElementById('sms-log-body');
    const emptyRow = tbody.querySelector('.empty');
    if (emptyRow) emptyRow.parentElement.remove();

    const time = new Date().toLocaleTimeString('en-KE');
    const row = document.createElement('tr');
    row.innerHTML = `
    <td>${time}</td>
    <td>${phone}</td>
    <td>${message}</td>
    <td>${action}</td>
    <td class="${success ? 'tag-success' : 'tag-fail'}">${success ? '✓ Sent' : '✗ Failed'}</td>
  `;
    tbody.prepend(row);

    const badge = document.getElementById('sms-count');
    badge.textContent = parseInt(badge.textContent || 0) + 1;
}