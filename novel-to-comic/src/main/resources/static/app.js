// ─── State ─────────────────────────────────────────────────────────
const S = {
  text: '', title: '', segCount: 90, curIdx: -1, steps: [], init: false, busy: false, es: null,
};

// ─── DOM ────────────────────────────────────────────────────────────
const $ = s => document.querySelector(s);
const $$ = s => document.querySelectorAll(s);

// ─── Tabs ───────────────────────────────────────────────────────────
$$('.tab').forEach(t => t.onclick = () => {
  $$('.tab').forEach(x => x.classList.remove('active'));
  t.classList.add('active');
  $$('.tab-content').forEach(x => x.classList.remove('active'));
  $('#tab' + (t.dataset.tab === 'paste' ? 'Paste' : 'Upload')).classList.add('active');
});

// ─── Upload ─────────────────────────────────────────────────────────
const uz = $('#uploadZone'), fi = $('#fileInput'), uf = $('#uploadInfo');
$('#btnSelectFile').onclick = () => fi.click();
uz.onclick = e => { if (e.target !== $('#btnSelectFile')) fi.click(); };
uz.ondragover = e => { e.preventDefault(); uz.classList.add('dragover'); };
uz.ondragleave = () => uz.classList.remove('dragover');
uz.ondrop = e => { e.preventDefault(); uz.classList.remove('dragover'); if (e.dataTransfer.files[0]) upFile(e.dataTransfer.files[0]); };
fi.onchange = () => { if (fi.files[0]) upFile(fi.files[0]); };
$('#btnClearFile').onclick = () => { uf.style.display = 'none'; uz.style.display = ''; fi.value = ''; S.text = ''; updStart(); };

async function upFile(file) {
  if (!file.name.endsWith('.txt')) { alert('仅支持 .txt'); return; }
  const fd = new FormData(); fd.append('file', file);
  const r = await fetch('/api/upload', { method: 'POST', body: fd });
  const d = await r.json();
  if (!d.success) { alert(d.error); return; }
  S.text = d.text; S.title = d.title || ''; $('#novelTitle').value = S.title;
  $('#novelText').value = d.text.slice(0, 500) + (d.text.length > 500 ? '\n\n... (完整内容已加载)' : '');
  $('#uploadFileName').textContent = d.filename;
  $('#uploadFileSize').textContent = fmtSize(d.size);
  uf.style.display = 'flex'; uz.style.display = 'none'; updStart();
}

$('#novelText').oninput = () => { S.text = $('#novelText').value; updStart(); };
$('#novelTitle').oninput = () => S.title = $('#novelTitle').value;
$('#segmentCount').oninput = () => { S.segCount = Math.min(140, Math.max(1, parseInt($('#segmentCount').value) || 90)); $('#segmentCount').value = S.segCount; };
function updStart() { $('#btnStart').disabled = !S.text.trim(); }

// ─── Auto-restore on page load ──────────────────────────────────────
(async function checkSavedState() {
  try {
    const r = await fetch('/api/state');
    const d = await r.json();
    if (!d.hasState) return;
    // Show restore banner
    const doneCount = d.completedSteps;
    const totalCount = d.totalSteps;
    const nextIdx = d.steps.findIndex(s => s.status !== 'done');
    const allDone = doneCount === totalCount;

    $('#bannerRestore').style.display = 'flex';
    $('#bannerTitle').textContent = d.title;
    $('#bannerProgress').textContent = allDone ? '全部完成 🎉' : `已完成 ${doneCount}/${totalCount} 步${nextIdx >= 0 ? ` · 下一步: ${d.steps[nextIdx].name}` : ''}`;
    if (allDone) {
      $('#bannerProgress').style.color = '#1a8a3f';
      $('#btnRestore').textContent = '📋 查看结果';
    } else {
      $('#btnRestore').textContent = '▶ 继续执行';
    }
    $('#btnRestore').onclick = () => restorePipeline(d, nextIdx, allDone);
    $('#btnRestoreNew').onclick = () => {
      $('#bannerRestore').style.display = 'none';
    };
  } catch (_) {}
})();

async function restorePipeline(stateData, nextIdx, allDone) {
  // Load full status
  const r = await fetch('/api/status');
  const d = await r.json();
  S.steps = d.steps;
  S.init = true;
  S.segCount = d.segmentCount;
  S.title = d.title;
  S.text = d.novelLength > 0 ? '(已恢复)' : '';

  $('#panelInput').style.display = 'none';
  $('#panelResult').style.display = 'block';
  $('#bannerRestore').style.display = 'none';
  renderSteps();
  updateBtns();

  // Show all completed step results
  const completedSteps = S.steps.filter(s => s.status === 'done');
  if (completedSteps.length > 0) {
    $('#resultTitle').textContent = `📋 ${d.title}`;
    if (allDone) {
      $('#resultStatus').textContent = '✅ 全部完成';
      $('#resultStatus').className = 'badge success';
      $('#btnContinue').style.display = 'none';
    } else {
      $('#resultStatus').textContent = `已完成 ${completedSteps.length}/${S.steps.length}`;
      $('#resultStatus').className = 'badge success';
    }

    // Fetch and display each completed step's result
    for (const s of completedSteps) {
      try {
        const rr = await fetch(`/api/results/${s.id}`);
        if (rr.ok) {
          const result = await rr.json();
          S.steps[s.index].result = result;
          showResult(s.index, result);
        }
      } catch (_) {}
    }

    // Show next step button if not all done
    if (!allDone && nextIdx >= 0) {
      showNextStepInfo(nextIdx);
      setupBtn(nextIdx);
    } else if (allDone) {
      $('#resultContent').insertAdjacentHTML('beforeend', `
        <div style="text-align:center;margin-top:24px;padding:24px;background:#f0fdf4;border-radius:12px;">
          <div style="font-size:32px;">🎉</div>
          <div style="font-size:18px;font-weight:700;color:#1a8a3f;">全部完成！</div>
          <div style="color:var(--text-secondary);">点击左下角「下载结果」获取 JSON</div>
        </div>
      `);
    }
  } else if (nextIdx >= 0) {
    // No steps done yet but init was called
    showReady({ novelLength: d.novelLength, segmentCount: d.segmentCount, estimatedMaxSegments: Math.floor(d.novelLength / 20) });
    setupBtn(nextIdx);
  }
}

function showNextStepInfo(nextIdx) {
  if (nextIdx >= S.steps.length) return;
  $('#resultContent').insertAdjacentHTML('beforeend', `
    <div style="margin-top:20px;padding:16px;background:#fff3cd;border:1px solid #ffc107;border-radius:8px;color:#856404;">
      下一步：<strong>${S.steps[nextIdx].name}</strong> — ${S.steps[nextIdx].desc}
    </div>
  `);
}

// ─── Init ───────────────────────────────────────────────────────────
$('#btnStart').onclick = async () => {
  if (!S.text.trim()) return;
  S.title = $('#novelTitle').value || '未命名作品';
  S.segCount = parseInt($('#segmentCount').value) || 90;

  const r = await fetch('/api/init', {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text: S.text, segmentCount: S.segCount, title: S.title }),
  });
  const d = await r.json();
  if (!d.success) { alert(d.error); return; }
  S.steps = d.steps; S.init = true; S.segCount = d.segmentCount;
  showWarn(d.warning);
  renderSteps();
  $('#panelInput').style.display = 'none';
  $('#panelResult').style.display = 'block';
  $('#bannerRestore').style.display = 'none';
  showReady(d);
  setupBtn(0);
  updateBtns();
};

// ─── Steps Panel ────────────────────────────────────────────────────
function renderSteps() {
  $('#stepList').innerHTML = S.steps.map((s, i) => `
    <div class="step-item ${i===S.curIdx?'active':''} ${s.status==='done'?'completed':''}" data-step="${i}" title="${s.status==='done'?'点击查看此步骤结果':''}">
      <div class="step-icon ${s.status}">${s.icon}</div>
      <div class="step-body">
        <div class="step-name">${s.name} ${s.status==='done'?'✅':''}</div>
        <div class="step-desc">${s.desc}</div>
        <span class="step-status ${s.status}">${stText(s.status)}</span>
      </div>
      <div class="step-actions-mini">
        ${(s.status==='done' && s.result) ? `<button class="btn btn-xs view-btn" data-step="${i}" title="查看此步骤结果">👁️ 查看</button>` : ''}
        ${(s.status==='done'||s.status==='error') ? `<button class="btn btn-xs rerun-btn" data-step="${i}" title="从此步重新执行">🔄</button>` : ''}
      </div>
    </div>
  `).join('');

  // Attach "view result" handlers
  $$('.view-btn').forEach(btn => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const stepIdx = parseInt(btn.dataset.step);
      await viewStepResult(stepIdx);
    };
  });

  // Attach rerun handlers
  $$('.rerun-btn').forEach(btn => {
    btn.onclick = async (e) => {
      e.stopPropagation();
      const stepIdx = parseInt(btn.dataset.step);
      if (!confirm(`确定从「${S.steps[stepIdx].name}」开始重新执行？\n此操作将清除该步骤及之后的所有结果。`)) return;
      await rerunFrom(stepIdx);
    };
  });

  // Click step item to view result (if done), or do nothing (if running/pending)
  $$('.step-item.completed').forEach(item => {
    item.style.cursor = 'pointer';
    item.onclick = async () => {
      const stepIdx = parseInt(item.dataset.step);
      await viewStepResult(stepIdx);
    };
  });
}

// View a completed step's result (highlight it in the result panel)
async function viewStepResult(stepIdx) {
  const step = S.steps[stepIdx];
  if (!step || step.status !== 'done') return;

  // If result not loaded yet, fetch it
  if (!step.result) {
    try {
      const rr = await fetch(`/api/results/${step.id}`);
      if (rr.ok) {
        step.result = await rr.json();
        S.steps[stepIdx].result = step.result;
      } else {
        alert('该步骤结果尚未保存');
        return;
      }
    } catch (_) {
      alert('获取结果失败');
      return;
    }
  }

  // Clear result panel and show this step
  $('#resultTitle').textContent = `👁️ ${step.name} 结果`;
  $('#resultStatus').textContent = '✅ 已完成';
  $('#resultStatus').className = 'badge success';
  $('#resultContent').innerHTML = '';
  showResult(stepIdx, step.result);

  // Scroll to result section
  setTimeout(() => {
    const section = $('#resultContent').querySelector('.result-section');
    if (section) section.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, 100);

  // Show continue button for next step if applicable
  const nx = stepIdx + 1;
  if (nx < S.steps.length && S.steps[nx].status !== 'done') {
    setupBtn(nx);
  } else if (S.steps.every(s => s.status === 'done')) {
    $('#btnContinue').style.display = 'none';
  } else {
    // Find next pending step
    const nextPending = S.steps.findIndex(s => s.status === 'pending');
    if (nextPending >= 0) setupBtn(nextPending);
    else $('#btnContinue').style.display = 'none';
  }
}

async function rerunFrom(stepIdx) {
  S.busy = true;
  $('#btnContinue').disabled = true;
  $('#resultContent').innerHTML = `<p style="color:var(--text-secondary);">🔄 正在重置步骤...</p>`;

  const r = await fetch(`/api/rerun/${stepIdx}`, { method: 'POST' });
  const d = await r.json();
  if (!d.success) { alert(d.error); S.busy = false; return; }

  // Update local state
  S.steps = d.steps.map((s, i) => ({ ...S.steps[i], ...s }));
  S.busy = false;
  renderSteps();
  updateBtns();

  // Setup to run from this step
  $('#resultTitle').textContent = '🔄 重新执行';
  $('#resultStatus').textContent = `将从第 ${stepIdx+1} 步开始`;
  $('#resultStatus').className = 'badge running';
  $('#resultContent').innerHTML = `<p style="color:var(--text-secondary);">已重置步骤 ${stepIdx+1} ~ ${S.steps.length}，准备重新执行</p>`;
  setupBtn(stepIdx);
}

function stText(s) { return {pending:'等待中',running:'⏳ 执行中...',done:'✅ 完成',error:'❌ 失败'}[s]||s; }

function showWarn(w) {
  $('#warningBox').style.display = w ? 'block' : 'none';
  if (w) $('#warningBox').textContent = w;
}
function showReady(d) {
  $('#resultTitle').textContent = '🎯 准备就绪';
  $('#resultStatus').textContent = `已加载 ${fmtSize(d.novelLength)} · ${d.segCount} 段`;
  $('#resultStatus').className = 'badge success';
  $('#resultContent').innerHTML = `
    <div class="stats-row">
      <div class="stat-card"><div class="stat-value">${fmtSize(d.novelLength)}</div><div class="stat-label">文本大小</div></div>
      <div class="stat-card"><div class="stat-value">${d.segCount}</div><div class="stat-label">目标片段</div></div>
      <div class="stat-card"><div class="stat-value">${d.estimatedMaxSegments||'—'}</div><div class="stat-label">预估最大段</div></div>
    </div>
    ${d.warning ? `<div class="warning-box">${d.warning}</div>` : ''}
    <p style="margin-top:16px;color:var(--text-secondary);">点击下方按钮开始：<strong>${S.steps[0].name}</strong></p>
  `;
}
function setupBtn(idx) {
  $('#btnContinue').style.display = 'inline-flex';
  $('#btnContinue').textContent = `▶ 执行第${idx+1}步：${S.steps[idx].name}`;
  $('#btnContinue').dataset.next = String(idx);
  $('#btnContinue').disabled = false;
}

// ─── Run Step ───────────────────────────────────────────────────────
$('#btnContinue').onclick = () => {
  const idx = parseInt($('#btnContinue').dataset.next);
  if (!isNaN(idx)) runStep(idx);
};

async function runStep(idx) {
  if (S.busy) return;
  S.busy = true; S.curIdx = idx;
  S.steps[idx].status = 'running'; S.steps[idx].error = null;
  renderSteps();
  $('#btnContinue').disabled = true;
  $('#resultStatus').textContent = '⏳ 执行中...';
  $('#resultStatus').className = 'badge running';

  // Setup live output
  $('#resultContent').innerHTML = `
    <div class="live-output"><div class="live-label">📡 实时流输出</div><pre id="liveText"></pre></div>
    <div class="progress-bar" id="progressBar"><div class="progress-fill" id="progressFill"></div><span class="progress-text" id="progressText"></span></div>
  `;

  // 1) Start the step (fire-and-forget)
  const r = await fetch(`/api/step/${idx}`, { method: 'POST' });
  const d = await r.json();
  if (!d.success) { alert(d.error); S.busy = false; return; }
  if (d.status === 'already_running') { console.log('Step already running'); }

  // 2) Connect SSE to observe
  connectSSE(idx);

  // 3) Poll status as fallback (in case SSE misses the done event)
  startPolling(idx);
}

// ─── SSE ────────────────────────────────────────────────────────────
function connectSSE(idx) {
  if (S.es) { S.es.close(); S.es = null; }
  const liveText = $('#liveText');
  const ac = {}; // accumulated text per index

  const es = new EventSource(`/api/step/${idx}/stream`);
  S.es = es;

  es.addEventListener('chunk', e => {
    const d = JSON.parse(e.data);
    if (d.replay) { liveText.textContent = d.text; return; }
    const key = d.index !== undefined ? d.index : '_main';
    if (!ac[key]) ac[key] = '';
    ac[key] += d.text;
    const parts = Object.keys(ac).sort((a,b) => a === '_main' ? -1 : b === '_main' ? 1 : a-b).map(k => ac[k]);
    liveText.textContent = parts.join('\n');
    liveText.scrollTop = liveText.scrollHeight;
  });

  es.addEventListener('progress', e => {
    const d = JSON.parse(e.data);
    const pct = d.total > 0 ? Math.round(d.done / d.total * 100) : 0;
    $('#progressFill').style.width = pct + '%';
    $('#progressText').textContent = d.message || `${d.done}/${d.total}`;
  });

  es.addEventListener('status', e => {
    const d = JSON.parse(e.data);
    if (d.message) {
      $('#progressText').textContent = d.message;
    }
  });

  es.addEventListener('result', e => {
    const d = JSON.parse(e.data);
    S.steps[idx].result = d;
  });

  es.addEventListener('done', e => {
    finishStep(idx);
  });

  es.addEventListener('error', e => {
    let msg = '未知错误';
    try { msg = JSON.parse(e.data).message; } catch (_) {}
  });

  es.onerror = () => { /* ignore, polling is fallback */ };
}

// ─── Polling (fallback) ─────────────────────────────────────────────
let pollTimer = null;

function startPolling(idx) {
  if (pollTimer) clearInterval(pollTimer);
  pollTimer = setInterval(async () => {
    try {
      const r = await fetch('/api/status');
      const d = await r.json();
      S.steps = d.steps;
      if (!d.running.includes(idx)) {
        if (S.steps[idx].status === 'done') finishStep(idx);
        else if (S.steps[idx].status === 'error') failStep(idx, S.steps[idx].error);
        clearInterval(pollTimer); pollTimer = null;
      }
    } catch (_) {}
  }, 1000);
}

function finishStep(idx) {
  clearInterval(pollTimer); pollTimer = null;
  if (S.es) { S.es.close(); S.es = null; }
  S.steps[idx].status = 'done';
  S.busy = false;
  renderSteps();
  updateBtns();

  $('#progressBar').style.display = 'none';
  $('#resultStatus').textContent = '✅ 完成';
  $('#resultStatus').className = 'badge success';

  const r = S.steps[idx].result;
  if (r) showResult(idx, r);

  const nx = idx + 1;
  if (nx < S.steps.length) setupBtn(nx);
  else {
    $('#btnContinue').style.display = 'none';
    $('#resultContent').insertAdjacentHTML('beforeend', `
      <div style="text-align:center;margin-top:24px;padding:24px;background:#f0fdf4;border-radius:12px;">
        <div style="font-size:32px;">🎉</div>
        <div style="font-size:18px;font-weight:700;color:#1a8a3f;">全部完成！</div>
        <div style="color:var(--text-secondary);">点击左下角「下载结果」获取 JSON</div>
      </div>
    `);
  }
}

function failStep(idx, msg) {
  clearInterval(pollTimer); pollTimer = null;
  if (S.es) { S.es.close(); S.es = null; }
  S.steps[idx].status = 'error'; S.steps[idx].error = msg;
  S.busy = false;
  renderSteps();
  $('#resultStatus').textContent = '❌ 失败';
  $('#resultStatus').className = 'badge error';
  $('#resultContent').insertAdjacentHTML('beforeend', `
    <div class="warning-box" style="background:#ffe5e5;border-color:#fecaca;color:#991b1b;margin-top:12px;">
      <strong>失败：</strong>${msg}
    </div>
    <div style="margin-top:12px;"><button class="btn btn-outline" onclick="retry(${idx})">🔄 重试</button></div>
  `);
  updateBtns();
  setupBtn(idx);
}

async function retry(idx) {
  S.steps[idx].status = 'pending';
  S.steps[idx].error = null;
  renderSteps();
  $('#resultContent').innerHTML = '';
  await runStep(idx);
}

// ─── Result Display ─────────────────────────────────────────────────
function showResult(idx, r) {
  const wrapper = document.createElement('div');
  wrapper.className = 'result-section';
  wrapper.innerHTML = renderResult(S.steps[idx].id, r, idx);
  $('#resultContent').appendChild(wrapper);
}

// Collapse long text to preview, with expand button
function collapsible(text, maxLen = 150) {
  if (!text || text.length <= maxLen) return esc(text);
  const id = 'cp_' + Math.random().toString(36).slice(2, 8);
  const preview = esc(text.slice(0, maxLen));
  const full = esc(text);
  return `<span class="collapsible-text" id="${id}">
    <span class="collapsible-preview">${preview}...</span>
    <span class="collapsible-full" style="display:none">${full}</span>
    <button class="btn-expand" onclick="toggleCollapse('${id}')" data-collapse-id="${id}">展开全文 ▼</button>
  </span>`;
}

// Item copy button (small inline)
function copyIcon(text, label) {
  return `<button class="btn-copy-inline copy-btn" data-text="${escAttr(text)}" title="复制${label||'内容'}">📋</button>`;
}

// Section "Copy All" button
function copyAllBtn(allText, label) {
  return `<button class="btn btn-xs btn-outline copy-all-btn copy-btn" data-text="${escAttr(allText)}">📋 复制全部${label||''}</button>`;
}

function renderResult(id, r, idx) {
  const header = (icon, title, extra) =>
    `<div class="result-section-header">
      <h3>${icon} ${title}</h3>
      <div class="result-section-actions">${extra||''}</div>
    </div>`;

  switch (id) {
    case 'split': {
      const all = (r.segments||[]).map((s,i) => `[${i+1}] ${s}`).join('\n\n');
      return header('✅', `拆分完成: ${r.count} 段`, copyAllBtn(all, `(${r.count}段)`)) +
        `<ol class="segment-list">${r.segments.map((s,i) =>
          `<li>
            <div class="seg-text">${collapsible(s)}</div>
            <div class="seg-actions">${copyIcon(s, `第${i+1}段`)}</div>
          </li>`
        ).join('')}</ol>`;
    }
    case 'storyboard': {
      const all = (r.storyboards||[]).map((s,i) => `[分镜#${i+1}]\n${s}`).join('\n\n');
      return header('✅', `分镜完成: ${r.count}/${r.total}`, copyAllBtn(all, `(${r.count}个)`)) +
        `${r.storyboards.map((s,i) =>
          `<div class="prompt-card">
            <div class="prompt-index">分镜 #${i+1} <span class="copy-row-actions">${copyIcon(s, `分镜#${i+1}`)}</span></div>
            <div class="prompt-full">${collapsible(s)}</div>
          </div>`
        ).join('')}`;
    }
    case 'characters': {
      const all = (r.characters||[]).map(c => `${c.name}：${c.description}`).join('\n\n');
      return header('✅', `角色提取: ${r.count} 人`, copyAllBtn(all, `(${r.count}人)`)) +
        `<div class="char-list">${(r.characters||[]).map(c =>
          `<div class="char-card">
            <div class="char-name">
              ${esc(c.name)}
              <span class="char-actions">${copyIcon(`${c.name}：${c.description}`, c.name)}</span>
            </div>
            <div class="char-desc">${collapsible(c.description)}</div>
          </div>`
        ).join('')}</div>`;
    }
    case 'turnaround':
    case 'turnarounds': {
      const allCombo = (r.turnarounds||[]).map(t => `[${t.name}]\n${t.comboPrompt||t.frontPrompt||''}`).join('\n\n');
      return header('✅', `角色三视图: ${r.count} 个角色`, copyAllBtn(allCombo, '三视图')) +
        `${(r.turnarounds||[]).map(t =>
          `<div class="turnaround-card">
            <div class="turnaround-header">
              <div class="turnaround-char-name">🎨 ${esc(t.name)}</div>
              <div class="turnaround-char-actions">${copyIcon(t.comboPrompt||t.frontPrompt||'', t.name)}</div>
            </div>
            <div class="turnaround-views">
              ${t.comboPrompt ? `
              <div class="turnaround-view combo-view">
                <div class="turnaround-view-label">🔲 综合三视图</div>
                <div class="turnaround-view-content">${collapsible(t.comboPrompt)}</div>
                <div class="turnaround-view-copy">${copyIcon(t.comboPrompt, '综合')}</div>
              </div>` : ''}
              ${t.frontPrompt ? `
              <div class="turnaround-view">
                <div class="turnaround-view-label">🧍 正面视图</div>
                <div class="turnaround-view-content">${collapsible(t.frontPrompt)}</div>
                <div class="turnaround-view-copy">${copyIcon(t.frontPrompt, '正面')}</div>
              </div>` : ''}
              ${t.backPrompt ? `
              <div class="turnaround-view">
                <div class="turnaround-view-label">🔙 背面视图</div>
                <div class="turnaround-view-content">${collapsible(t.backPrompt)}</div>
                <div class="turnaround-view-copy">${copyIcon(t.backPrompt, '背面')}</div>
              </div>` : ''}
              ${t.sidePrompt ? `
              <div class="turnaround-view">
                <div class="turnaround-view-label">👈 侧面视图</div>
                <div class="turnaround-view-content">${collapsible(t.sidePrompt)}</div>
                <div class="turnaround-view-copy">${copyIcon(t.sidePrompt, '侧面')}</div>
              </div>` : ''}
            </div>
          </div>`
        ).join('')}`;
    }
    case 'rename': {
      const all = (r.mapping||[]).map(m => `${m.from} → ${m.to}`).join('\n');
      return header('✅', `角色改名: ${(r.mapping||[]).length} 项变更`, copyAllBtn(all, '对照表')) +
        `${r.mapping?.length
          ? `<table class="mapping-table"><tr><th>原名</th><th></th><th>新名</th><th>操作</th></tr>
            ${r.mapping.map(m => `<tr>
              <td>${esc(m.from)}</td>
              <td class="arrow">→</td>
              <td>${esc(m.to)}</td>
              <td>${copyIcon(m.to, m.to)}</td>
            </tr>`).join('')}</table>`
          : '<p>无变更</p>'}`;
    }
    case 'prompts': {
      const all = (r.prompts||[]).map(p => `[提示词 #${p.index}]\n${p.fullPrompt}`).join('\n\n');
      return header('✅', `组装完成: ${r.count} 个提示词`, copyAllBtn(all, `(${r.count}个)`)) +
        `${r.prompts.map(p =>
          `<div class="prompt-card">
            <div class="prompt-index">提示词 #${p.index} <span class="copy-row-actions">${copyIcon(p.fullPrompt, `提示词#${p.index}`)}</span></div>
            <div class="prompt-full">${collapsible(p.fullPrompt)}</div>
          </div>`
        ).join('')}`;
    }
    case 'lastpage': {
      return header('✅', '末张引流', copyAllBtn(r.prompt, '')) +
        `<div class="prompt-card">
          <div class="prompt-index">末张引流页 <span class="copy-row-actions">${copyIcon(r.prompt, '引流')}</span></div>
          <div class="prompt-full">${collapsible(r.prompt)}</div>
        </div>`;
    }
    default: return '';
  }
}

// Toggle collapse/expand for long text
function toggleCollapse(id) {
  const el = document.getElementById(id);
  if (!el) return;
  const preview = el.querySelector('.collapsible-preview');
  const full = el.querySelector('.collapsible-full');
  const btn = el.querySelector('.btn-expand');
  if (!preview || !full || !btn) return;
  if (full.style.display === 'none') {
    preview.style.display = 'none';
    full.style.display = '';
    btn.textContent = '收起 ▲';
  } else {
    preview.style.display = '';
    full.style.display = 'none';
    btn.textContent = '展开全文 ▼';
  }
}

// ─── Buttons ────────────────────────────────────────────────────────
function updateBtns() {
  $('#btnReset').disabled = !S.init || S.busy;
  $('#btnDownload').disabled = !S.steps.some(s => s.status === 'done') || S.busy;
}

$('#btnReset').onclick = async () => {
  if (!confirm('确定重置？这将清除所有结果。')) return;
  await fetch('/api/reset', { method: 'POST' });
  S.text = ''; S.title = ''; S.segCount = 90; S.curIdx = -1; S.steps = []; S.init = false; S.busy = false;
  if (S.es) { S.es.close(); S.es = null; }
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
  $('#panelInput').style.display = 'block'; $('#panelResult').style.display = 'none';
  $('#btnContinue').style.display = 'none'; $('#warningBox').style.display = 'none';
  $('#bannerRestore').style.display = 'none';
  $('#novelText').value = ''; $('#novelTitle').value = ''; $('#segmentCount').value = '90';
  $('#stepList').innerHTML = ''; uf.style.display = 'none'; uz.style.display = ''; fi.value = '';
  updateBtns(); updStart();
};

$('#btnDownload').onclick = () => window.open('/api/download', '_blank');

document.addEventListener('click', e => {
  if (e.target.classList.contains('copy-btn')) {
    navigator.clipboard.writeText(e.target.dataset.text).then(() => {
      const btn = e.target;
      const orig = btn.textContent;
      const isInline = btn.classList.contains('btn-copy-inline');
      btn.textContent = isInline ? '✅' : '✅ 已复制!';
      btn.style.opacity = '1';
      setTimeout(() => {
        btn.textContent = orig;
        if (isInline) btn.style.opacity = '';
      }, 1200);
    }).catch(() => {
      // Fallback for older browsers
      const ta = document.createElement('textarea');
      ta.value = e.target.dataset.text;
      ta.style.position = 'fixed'; ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      try { document.execCommand('copy'); } catch (_) {}
      document.body.removeChild(ta);
      const btn = e.target;
      const orig = btn.textContent;
      const isInline = btn.classList.contains('btn-copy-inline');
      btn.textContent = isInline ? '✅' : '✅ 已复制!';
      setTimeout(() => btn.textContent = orig, 1200);
    });
  }
});

// ─── Helpers ────────────────────────────────────────────────────────
function esc(s) { const d = document.createElement('div'); d.textContent = s || ''; return d.innerHTML; }
function escAttr(s) { return (s||'').replace(/&/g,'&amp;').replace(/"/g,'&quot;').replace(/'/g,'&#39;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
function fmtSize(b) { if(!b) return '0B'; return b<1024?b+'B':b<1048576?(b/1024).toFixed(1)+'KB':(b/1048576).toFixed(1)+'MB'; }

updStart();

// ─── Project Management ─────────────────────────────────────────────
let allProjects = [];
let currentTaskId = null;
let projectFilter = 'all';

// Track current task ID from init/restore
const origInit = S;
const origRestore = restorePipeline;

// Open/close overlay
$('#btnProjects').onclick = () => { openProjects(); };
$('#btnCloseProjects').onclick = () => closeProjects();
$('#overlayBackdrop').onclick = () => closeProjects();

function openProjects() {
  $('#projectOverlay').style.display = 'flex';
  loadProjects();
}

function closeProjects() {
  $('#projectOverlay').style.display = 'none';
}

// Close overlay with Escape key
document.addEventListener('keydown', e => {
  if (e.key === 'Escape' && $('#projectOverlay').style.display !== 'none') {
    closeProjects();
  }
});

// Filter buttons
$$('.filter-btn').forEach(btn => {
  btn.onclick = () => {
    $$('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    projectFilter = btn.dataset.filter;
    renderProjects();
  };
});

async function loadProjects() {
  try {
    const r = await fetch('/api/projects');
    allProjects = await r.json();

    // Get current task ID
    try {
      const sr = await fetch('/api/status');
      const sd = await sr.json();
      currentTaskId = sd.taskId;
    } catch (_) {}

    renderProjects();
  } catch (e) {
    $('#projectList').innerHTML = '<div class="project-empty">加载失败: ' + e.message + '</div>';
  }
}

function renderProjects() {
  let filtered = allProjects;

  // Get current state info
  const isAnyLoaded = S.init && S.steps.length > 0;

  if (projectFilter === 'active') {
    filtered = filtered.filter(p => p.status !== 'COMPLETED' && p.completedSteps < p.totalSteps);
  } else if (projectFilter === 'done') {
    filtered = filtered.filter(p => p.status === 'COMPLETED' || p.completedSteps === p.totalSteps);
  }

  $('#projectCount').textContent = `共 ${filtered.length} 个项目`;

  if (filtered.length === 0) {
    $('#projectList').innerHTML = '<div class="project-empty">📭 暂无项目<br><small>创建新任务后这里会显示</small></div>';
    return;
  }

  $('#projectList').innerHTML = filtered.map(p => {
    const pct = p.totalSteps > 0 ? Math.round(p.completedSteps / p.totalSteps * 100) : 0;
    const allDone = p.status === 'COMPLETED' || p.completedSteps === p.totalSteps;
    const isActive = currentTaskId === p.id;
    const isLoaded = isAnyLoaded && isActive;

    // Determine status badge
    let statusText = '';
    let statusColor = '';
    if (allDone) {
      statusText = '✅ 已完成';
      statusColor = 'var(--success)';
    } else if (p.completedSteps > 0) {
      statusText = `⏳ ${p.completedSteps}/${p.totalSteps}`;
      statusColor = 'var(--warning)';
    } else {
      statusText = '📝 待开始';
      statusColor = 'var(--text-muted)';
    }

    // Action button
    let actionBtn = '';
    if (isLoaded) {
      actionBtn = `<button class="btn btn-xs btn-outline" onclick="event.stopPropagation();continueCurrent()" title="继续执行当前任务">▶ 继续</button>`;
    } else if (allDone) {
      actionBtn = `<button class="btn btn-xs btn-outline" onclick="event.stopPropagation();loadAndView('${p.id}')" title="加载并查看结果">👁️ 查看</button>`;
    } else {
      actionBtn = `<button class="btn btn-xs btn-primary" onclick="event.stopPropagation();loadAndResume('${p.id}')" title="加载并继续执行">▶ 继续</button>`;
    }

    // Progress bar color
    const barColor = allDone ? 'var(--success)' : 'var(--primary)';

    return `
      <div class="project-card ${allDone ? 'completed' : ''} ${isActive ? 'active-task' : ''}"
           onclick="${isLoaded ? 'closeProjects()' : (allDone ? `loadAndView('${p.id}')` : `loadAndResume('${p.id}')`)}"
           title="${isLoaded ? '已是当前任务' : '点击加载此项目'}">
        <div class="pc-icon">${getStepIcon(p)}</div>
        <div class="pc-body">
          <div class="pc-title">${esc(p.title)}</div>
          <div class="pc-meta">
            <span>📏 ${fmtSize(p.novelLength)}</span>
            <span>📐 ${p.segmentCount} 段</span>
            <span style="color:${statusColor}">${statusText}</span>
            <span>🕐 ${fmtDate(p.updatedAt)}</span>
          </div>
       </div>
        <div class="pc-progress">
          <div class="pc-progress-bar">
            <div class="pc-progress-fill" style="width:${pct}%;background:${barColor}"></div>
          </div>
          <div class="pc-progress-text">${pct}%</div>
        </div>
        <div class="pc-actions">
          ${actionBtn}
          <button class="btn btn-xs btn-danger" onclick="event.stopPropagation();deleteProject('${p.id}','${escAttr(p.title)}')" title="删除项目">🗑️</button>
        </div>
      </div>
    `;
  }).join('');
}

function getStepIcon(p) {
  if (p.completedSteps === p.totalSteps) return '🎉';
  if (p.completedSteps > 0) return '🔄';
  return '📄';
}

async function loadAndResume(taskId) {
  closeProjects();
  showLoading('正在加载项目...');
  try {
    const r = await fetch(`/api/projects/${taskId}/load`, { method: 'POST' });
    if (!r.ok) {
      const d = await r.json();
      alert(d.error || '加载失败');
      return;
    }
    const d = await r.json();
    currentTaskId = taskId;
    await applyLoadedState(d);
  } catch (e) {
    alert('加载失败: ' + e.message);
  }
}

async function loadAndView(taskId) {
  closeProjects();
  showLoading('正在加载项目...');
  try {
    const r = await fetch(`/api/projects/${taskId}/load`, { method: 'POST' });
    if (!r.ok) {
      const d = await r.json();
      alert(d.error || '加载失败');
      return;
    }
    const d = await r.json();
    currentTaskId = taskId;
    await applyLoadedState(d);
  } catch (e) {
    alert('加载失败: ' + e.message);
  }
}

async function applyLoadedState(d) {
  // Reset current state
  S.busy = false;
  if (S.es) { S.es.close(); S.es = null; }
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }

  S.init = d.hasState;
  S.segCount = d.segmentCount;
  S.title = d.title;
  S.text = d.hasState ? '(已恢复)' : '';

  // Refresh step status
  const sr = await fetch('/api/status');
  const sd = await sr.json();
  S.steps = sd.steps;
  currentTaskId = sd.taskId;

  // Show result panel
  $('#panelInput').style.display = 'none';
  $('#panelResult').style.display = 'block';
  $('#bannerRestore').style.display = 'none';
  renderSteps();
  updateBtns();

  const completedSteps = S.steps.filter(s => s.status === 'done');
  const allDone = completedSteps.length === S.steps.length;
  const nextIdx = S.steps.findIndex(s => s.status !== 'done');

  if (allDone) {
    $('#resultTitle').textContent = '📋 ' + d.title;
    $('#resultStatus').textContent = '✅ 全部完成';
    $('#resultStatus').className = 'badge success';
    $('#btnContinue').style.display = 'none';
    $('#resultContent').innerHTML = '';

    // Load all completed results
    for (const s of completedSteps) {
      try {
        const rr = await fetch(`/api/results/${s.id}`);
        if (rr.ok) {
          const result = await rr.json();
          S.steps[s.index].result = result;
          showResult(s.index, result);
        }
      } catch (_) {}
    }

    $('#resultContent').insertAdjacentHTML('beforeend', `
      <div style="text-align:center;margin-top:24px;padding:24px;background:#f0fdf4;border-radius:12px;">
        <div style="font-size:32px;">🎉</div>
        <div style="font-size:18px;font-weight:700;color:#1a8a3f;">全部完成！</div>
      </div>
    `);
  } else if (completedSteps.length > 0) {
    $('#resultTitle').textContent = '📋 ' + d.title;
    $('#resultStatus').textContent = `已完成 ${completedSteps.length}/${S.steps.length}`;
    $('#resultStatus').className = 'badge success';
    $('#resultContent').innerHTML = '';

    for (const s of completedSteps) {
      try {
        const rr = await fetch(`/api/results/${s.id}`);
        if (rr.ok) {
          const result = await rr.json();
          S.steps[s.index].result = result;
          showResult(s.index, result);
        }
      } catch (_) {}
    }

    if (nextIdx >= 0) {
      showNextStepInfo(nextIdx);
      setupBtn(nextIdx);
    }
  } else if (nextIdx >= 0) {
    // Init done but no steps completed
    showReady({ novelLength: sd.novelLength, segmentCount: sd.segmentCount, estimatedMaxSegments: Math.floor(sd.novelLength / 20) });
    setupBtn(nextIdx);
  }
}

function continueCurrent() {
  closeProjects();
  // Find next pending step
  const nextIdx = S.steps.findIndex(s => s.status === 'pending');
  const firstErr = S.steps.findIndex(s => s.status === 'error');

  if (firstErr >= 0 && (nextIdx < 0 || firstErr < nextIdx)) {
    // There's an error — show retry
    $('#resultContent').insertAdjacentHTML('beforeend', `
      <div class="warning-box" style="margin-top:12px;background:#ffe5e5;border-color:#fecaca;color:#991b1b;">
        步骤「${S.steps[firstErr].name}」失败，请重试
      </div>
      <div style="margin-top:12px;"><button class="btn btn-outline" onclick="retry(${firstErr})">🔄 重试</button></div>
    `);
    setupBtn(firstErr);
  } else if (nextIdx >= 0) {
    setupBtn(nextIdx);
  }
}

async function deleteProject(taskId, title) {
  if (!confirm(`确定删除项目「${title}」？\n此操作不可恢复！`)) return;
  try {
    const r = await fetch(`/api/projects/${taskId}`, { method: 'DELETE' });
    const d = await r.json();
    if (!d.success) { alert(d.error); return; }

    // If we deleted the current project, reset UI
    if (taskId === currentTaskId) {
      S.text = ''; S.title = ''; S.segCount = 90; S.curIdx = -1; S.steps = []; S.init = false; S.busy = false;
      if (S.es) { S.es.close(); S.es = null; }
      if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
      $('#panelInput').style.display = 'block'; $('#panelResult').style.display = 'none';
      $('#btnContinue').style.display = 'none'; $('#warningBox').style.display = 'none';
      $('#bannerRestore').style.display = 'none';
      $('#novelText').value = ''; $('#novelTitle').value = ''; $('#segmentCount').value = '90';
      $('#stepList').innerHTML = '';
      updateBtns(); updStart();
      currentTaskId = null;
    }

    // Reload project list
    await loadProjects();
  } catch (e) {
    alert('删除失败: ' + e.message);
  }
}

function showLoading(msg) {
  $('#panelInput').style.display = 'none';
  $('#panelResult').style.display = 'block';
  $('#resultTitle').textContent = '⏳ 加载中';
  $('#resultStatus').textContent = msg;
  $('#resultStatus').className = 'badge running';
  $('#resultContent').innerHTML = '<p style="text-align:center;padding:48px;color:var(--text-secondary);">🔄 ' + msg + '</p>';
}

function fmtDate(iso) {
  if (!iso) return '';
  try {
    const d = new Date(iso);
    const now = new Date();
    const diff = now - d;
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return Math.round(diff/60000) + '分钟前';
    if (diff < 86400000) return Math.round(diff/3600000) + '小时前';
    const y = d.getFullYear();
    const m = String(d.getMonth()+1).padStart(2,'0');
    const day = String(d.getDate()).padStart(2,'0');
    const h = String(d.getHours()).padStart(2,'0');
    const min = String(d.getMinutes()).padStart(2,'0');
    return `${y}-${m}-${day} ${h}:${min}`;
  } catch (_) { return iso.slice(0,16); }
}

console.log('🎬 v5.0 持久化 + 多项目管理 前端就绪');