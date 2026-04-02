const API = "http://localhost:8080";
let chart;

function initClock() {
  setInterval(() => {
    document.getElementById("clock").textContent =
      new Date().toLocaleTimeString("it-IT");
  }, 1000);
}

function initChart() {
  const ctx = document.getElementById("levelChart");
  if (!ctx) return;

  chart = new Chart(ctx.getContext("2d"), {
    type: "line",
    data: {
      labels: [],
      datasets: [
        {
          label: "Distance (cm)",
          data: [],
          borderColor: "#00d4ff",
          backgroundColor: "rgba(0,212,255,0.05)",
          borderWidth: 2,
          pointRadius: 3,
          pointBackgroundColor: "#00d4ff",
          tension: 0.3,
          fill: true,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      animation: { duration: 300 },
      plugins: { legend: { display: false } },
      scales: {
        x: {
          ticks: {
            color: "#4a6070",
            font: { family: "Share Tech Mono", size: 10 },
          },
          grid: { color: "#1c2a3a" },
        },
        y: {
          ticks: {
            color: "#4a6070",
            font: { family: "Share Tech Mono", size: 10 },
          },
          grid: { color: "#1c2a3a" },
        },
      },
    },
  });
}

async function fetchLevel() {
  try {
    const res = await fetch(API + "/api/data");
    const data = await res.json();
    if (!data.length) return;

    document.getElementById("distValue").textContent = data[0].value.toFixed(1);

    const sorted = [...data].reverse();
    chart.data.labels = sorted.map((p) =>
      new Date(p.time).toLocaleTimeString("it-IT"),
    );
    chart.data.datasets[0].data = sorted.map((p) => p.value);
    chart.update();

    setConnected(true);
  } catch {
    setConnected(false);
  }
}

async function fetchStatus() {
  try {
    const res = await fetch(API + "/api/status");
    const data = await res.json();
    setStatus(data.mode);
    setConnected(true);
  } catch {
    setStatus("NOT_AVAILABLE");
    setConnected(false);
  }
}

async function fetchValve() {
  try {
    const res = await fetch(API + "/api/valve");
    const data = await res.json();
    const pct = Math.round(data.percent);
    document.getElementById("valveValue").textContent = pct + "%";
    document.getElementById("valveFill").style.width = pct + "%";
    document.getElementById("valveSlider").value = pct;
    document.getElementById("sliderValue").textContent = pct + "%";
  } catch {}
}

function setStatus(mode) {
  const badge = document.getElementById("statusBadge");
  badge.className = "status-badge status-" + mode;
  document.getElementById("statusText").textContent = mode.replace("_", " ");
  const sliderWrap = document.getElementById("sliderWrap");
  const isManual = mode === "MANUAL";
  sliderWrap.style.opacity = isManual ? "1" : "0.3";
  sliderWrap.style.pointerEvents = isManual ? "auto" : "none";
  
  document.getElementById("btnAuto").disabled = (mode === "AUTOMATIC");
  document.getElementById("btnManual").disabled = (mode === "MANUAL");
}

function setConnected(ok) {
  document.getElementById("connDot").className =
    "conn-dot " + (ok ? "conn-ok" : "conn-err");
  document.getElementById("connLabel").textContent = ok
    ? "CONNECTED"
    : "NOT CONNECTED";
}

async function setMode(mode) {
  try {
    await fetch(API + "/api/status", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ mode }),
    });
    setStatus(mode);
    if (mode === "MANUAL") {
      fetchValve();
    }
  } catch {
    alert("CUS non raggiungibile");
  }
}

function updateSliderValue(slider) {
  const pct = parseInt(slider.value);
  document.getElementById("sliderValue").textContent = pct + "%";
  
  fetch(API + "/api/valve", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ percent: pct }),
  }).catch(() => {});
}

async function sendValve() {}

function startPolling() {
  setInterval(fetchLevel, 1000);
  setInterval(fetchStatus, 1000);
  setInterval(fetchValve, 1000);
  fetchLevel();
  fetchStatus();
  fetchValve();
}

document.addEventListener("DOMContentLoaded", () => {
  initClock();
  initChart();
  startPolling();
});
