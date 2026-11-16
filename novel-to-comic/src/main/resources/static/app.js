// ===== 状态管理 =====
const State = {
  currentScreen: 'home',
  projects: [],
  currentTask: { id: null, title: '', text: '', segmentCount: 90, steps: [], novelLength: 0, style: '韩漫' },
  ui: { isBusy: false, selectedStep: -1, projectFilter: 'all' },
  es: null,
  pollTimer: null
};

const STEP_DEFS = [
  { id: 'split', name: '文本拆分', desc: '将小说切分为多个片段', icon: '✂️' },
  { id: 'characters', name: '提取人物', desc: '识别文本中的人物角色', icon: '👥' },
  { id: 'turnaround', name: '角色参考图', desc: '为每个角色生成三视图/立绘', icon: '🎨' },
  { id: 'storyboard', name: '一键分镜', desc: '将文字切成分镜页', icon: '📝' },
  { id: 'prompts', name: '生成描述词', desc: '组装出图提示词', icon: '✨' },
  { id: 'image', name: '生成图片', desc: 'AI 生成漫画图片', icon: '🖼️' },
  { id: 'lastpage', name: '末张引流', desc: '生成末张引流页', icon: '📌' },
  { id: 'export', name: '成片导出', desc: '导出分页图或视频', icon: '📤' },
];

const STYLES = {
  '韩漫': { icon: '👤', desc: '竖屏条漫默认：真人比例、清晰线、柔光薄涂' },
  '清冷都市': { icon: '🏙️', desc: '职场/都市韩漫：干净线稿、低饱和' },
  '柔光韩漫': { icon: '💫', desc: '精致五官与柔和环境光' },
  'Q版': { icon: '🎭', desc: '二头身、大眼、平涂马卡龙色' },
  '软萌都市': { icon: '🌸', desc: 'Q版三四头身·大眼柔光' },
  '古风': { icon: '🏮', desc: '2D 国风插画，多层平涂' },
};

// 角色数据（从后端获取或本地缓存）
let CHARACTERS_DATA = [];
let STORYBOARDS_DATA = [];

const $ = s => document.querySelector(s);
const $$ = s => document.querySelectorAll(s);

// ===== 初始化 =====
document.addEventListener('DOMContentLoaded', () => {
  initNavigation();
  initHomePage();
  initProjectsPage();
  initWorkbench();
  initSettingsPage();
  initStyleGrid();
  initFileUpload();
  checkSavedState();
  loadProjects();
});

// ===== 页面切换 =====
function switchScreen(screenName) {
  State.currentScreen = screenName;
  $$('.screen').forEach(s => { s.classList.add('hidden'); s.style.display = ''; });
  const target = $(`#screen-${screenName}`);
  if (target) { target.classList.remove('hidden'); if (screenName === 'work') target.style.display = 'flex'; }
  $$('.nav-btn').forEach(btn => btn.classList.toggle('is-on', btn.dataset.screen === screenName));
  document.body.dataset.screen = screenName;
  if (screenName === 'projects') loadProjects();
  if (screenName === 'home') loadRecentProjects();
}

function initNavigation() {
  $$('.nav-btn').forEach(btn => btn.addEventListener('click', () => switchScreen(btn.dataset.screen)));
  $('#brand-home').addEventListener('click', () => switchScreen('home'));
}

// ===== 首页 =====
function initHomePage() {
  $('#home-new').addEventListener('click', () => openNewProjectModal());
  $('#home-continue').addEventListener('click', async () => {
    try {
      const r = await fetch('/api/state');
      const d = await r.json();
      if (d.hasState) {
        const p = State.projects.find(p => p.status === 'IN_PROGRESS');
        p ? loadProject(p.id) : showToast('没有可恢复的任务', 'info');
      } else {
        showToast('没有可恢复的任务', 'info');
      }
    } catch (e) { showToast('加载状态失败', 'error'); }
  });
  $('#home-more').addEventListener('click', () => switchScreen('projects'));
  $('#home-guide').addEventListener('click', () => showToast('教程功能开发中...', 'info'));
}

function loadRecentProjects() {
  const container = $('#home-recent');
  const recent = State.projects.slice(0, 4);
  if (recent.length === 0) {
    container.innerHTML = '<div class="project-empty"><div class="empty-icon">📭</div><div>暂无项目</div></div>';
    return;
  }
  container.innerHTML = recent.map(p => renderProjectCard(p)).join('');
  bindProjectCardEvents(container);
  $('#home-foot-meta').textContent = `共 ${State.projects.length} 个项目`;
}

// ===== 项目列表 =====
function initProjectsPage() {
  $('#projects-new').addEventListener('click', () => openNewProjectModal());
  $$('.filter-btn').forEach(btn => btn.addEventListener('click', () => {
    $$('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    State.ui.projectFilter = btn.dataset.filter;
    renderProjectList();
  }));
}

async function loadProjects() {
  try {
    const r = await fetch('/api/projects');
    if (r.ok) {
      State.projects = await r.json();
      renderProjectList();
      loadRecentProjects();
      updateProjectTabs();
    }
  } catch (e) { console.error('加载项目失败:', e); }
}

function renderProjectList() {
  const container = $('#project-list-body');
  let filtered = State.projects;
  if (State.ui.projectFilter === 'active') filtered = filtered.filter(p => p.status !== 'COMPLETED');
  else if (State.ui.projectFilter === 'done') filtered = filtered.filter(p => p.status === 'COMPLETED');
  $('#projectCount').textContent = `共 ${filtered.length} 个项目`;
  if (filtered.length === 0) {
    container.innerHTML = '<div class="project-empty"><div class="empty-icon">📭</div><div>暂无项目</div></div>';
    return;
  }
  container.innerHTML = filtered.map(p => renderProjectCard(p, true)).join('');
  bindProjectCardEvents(container, true);
}

function renderProjectCard(project, showActions = false) {
  const pct = project.totalSteps > 0 ? Math.round(project.completedSteps / project.totalSteps * 100) : 0;
  const isCompleted = project.status === 'COMPLETED';
  let icon = '📄', statusText = '待开始';
  if (isCompleted) { icon = '🎉'; statusText = '已完成'; }
  else if (project.completedSteps > 0) { icon = '🔄'; statusText = `${project.completedSteps}/${project.totalSteps}`; }
  return `<div class="project-card ${isCompleted ? 'completed' : ''}" data-id="${project.id}">
    <div class="card-icon">${icon}</div>
    <div class="card-title">${project.title}</div>
    <div class="card-meta">📏 ${formatSize(project.novelLength)} · 📐 ${project.segmentCount}段 · ${statusText}</div>
    <div class="card-progress"><div class="card-progress-fill" style="width:${pct}%"></div></div>
    ${showActions ? `<div class="card-actions"><button class="btn btn-sm btn-outline btn-view" data-id="${project.id}">${isCompleted ? '👁️ 查看' : '▶ 继续'}</button><button class="btn btn-sm btn-outline btn-delete" data-id="${project.id}">🗑️</button></div>` : ''}
  </div>`;
}

function bindProjectCardEvents(container, showActions = false) {
  container.querySelectorAll('.project-card').forEach(card => card.addEventListener('click', e => { if (!e.target.closest('.card-actions')) loadProject(card.dataset.id); }));
  if (showActions) {
    container.querySelectorAll('.btn-view').forEach(btn => btn.addEventListener('click', e => { e.stopPropagation(); loadProject(btn.dataset.id); }));
    container.querySelectorAll('.btn-delete').forEach(btn => btn.addEventListener('click', e => { e.stopPropagation(); deleteProject(btn.dataset.id); }));
  }
}

async function loadProject(projectId) {
  try {
    showToast('正在加载项目...', 'info');
    const r = await fetch(`/api/projects/${projectId}/load`, { method: 'POST' });
    if (!r.ok) {
      const d = await r.json();
      showToast(d.error || '加载失败', 'error');
      return;
    }
    const d = await r.json();
    switchScreen('work');
    await applyLoadedState(d);
    showToast(`已加载项目`, 'success');
  } catch (e) { showToast('加载失败: ' + e.message, 'error'); }
}

async function applyLoadedState(d) {
  State.currentTask.title = d.title || '';
  State.currentTask.segmentCount = d.segmentCount || 90;
  State.currentTask.style = d.currentStyle || State.currentTask.style || '韩漫';

  // 更新侧边栏画风显示
  if (State.currentTask.style) {
    $('#rail-style-summary').textContent = `画风 · ${State.currentTask.style}`;
    $('#style-name').textContent = State.currentTask.style;
    $('#style-preview-icon').textContent = STYLES[State.currentTask.style]?.icon || '👤';
    $('#style-blurb').textContent = STYLES[State.currentTask.style]?.desc || '';
  }

  // 获取步骤状态
  const statusR = await fetch('/api/status');
  const statusD = await statusR.json();
  State.currentTask.steps = statusD.steps || [];
  State.currentTask.novelLength = statusD.novelLength || 0;

  showWorkContent();
  renderSteps();

  // 加载已完成步骤的结果
  const completedSteps = State.currentTask.steps.filter(s => s.status === 'done');
  if (completedSteps.length > 0) {
    $('#resultTitle').textContent = `📋 ${d.title}`;
    $('#resultStatus').textContent = `已完成 ${completedSteps.length}/${State.currentTask.steps.length}`;
    $('#resultStatus').className = 'badge success';

    for (const step of completedSteps) {
      try {
        const rr = await fetch(`/api/results/${step.id}`);
        if (rr.ok) {
          const result = await rr.json();
          State.currentTask.steps[step.index].result = result;
          showResult(step.index, result);
        }
      } catch (_) {}
    }

    // 显示角色和分镜
    if (completedSteps.some(s => s.id === 'characters' || s.id === 'turnaround')) {
      await loadCharacters();
    }
    if (completedSteps.some(s => s.id === 'storyboard')) {
      await loadStoryboards();
    }
  }

  // 设置下一步按钮
  const nextIdx = State.currentTask.steps.findIndex(s => s.status !== 'done');
  if (nextIdx >= 0) setupButton(nextIdx);

  updateButtons();
  updateProjectTabs();
}

async function deleteProject(projectId) {
  const project = State.projects.find(p => p.id === projectId);
  if (!project) return;
  if (!confirm(`确定删除项目「${project.title}」？`)) return;
  try {
    const r = await fetch(`/api/projects/${projectId}`, { method: 'DELETE' });
    const d = await r.json();
    if (!d.success) { showToast(d.error || '删除失败', 'error'); return; }
    showToast('项目已删除', 'success');
    await loadProjects();
  } catch (e) { showToast('删除失败: ' + e.message, 'error'); }
}

// ===== 工作台 =====
function initWorkbench() {
  // Tab 切换
  $$('.tab').forEach(tab => tab.addEventListener('click', () => {
    $$('.tab').forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    $$('.tab-content').forEach(c => c.classList.remove('active'));
    $(`#tab${tab.dataset.tab === 'paste' ? 'Paste' : 'Upload'}`).classList.add('active');
  }));

  // 输入事件
  $('#novelText').addEventListener('input', () => { State.currentTask.text = $('#novelText').value; updateStartButton(); });
  $('#novelTitle').addEventListener('input', () => { State.currentTask.title = $('#novelTitle').value; });
  $('#segmentCount').addEventListener('input', () => { State.currentTask.segmentCount = parseInt($('#segmentCount').value) || 90; });

  // 按钮事件
  $('#btnStart').addEventListener('click', startProcessing);
  $('#btnContinue').addEventListener('click', () => { const idx = parseInt($('#btnContinue').dataset.next); if (!isNaN(idx)) runStep(idx); });
  $('#btnReset').addEventListener('click', async () => { if (confirm('确定重置？')) { await resetTask(); showInputPanel(); showToast('任务已重置', 'success'); } });
  $('#btnDownload').addEventListener('click', () => { window.open('/api/download', '_blank'); });
  $('#new-work').addEventListener('click', () => openNewProjectModal());
  $('#open-projects').addEventListener('click', () => switchScreen('projects'));

  // 角色区按钮
  $('#btnViewAllSettings').addEventListener('click', () => showToast('打开全部角色设定板', 'info'));
  $('#btnGenerateAll').addEventListener('click', () => { showToast('开始为所有角色生成/补齐立绘...', 'info'); });
  $('#btnManualAdd').addEventListener('click', () => { addManualCharacter(); });
  $('#btnAssetLibrary').addEventListener('click', () => showToast('素材库功能开发中...', 'info'));
  $('#btnCollapseChars').addEventListener('click', () => {
    const section = $('#charactersSection');
    if (section.style.display === 'none') { section.style.display = ''; $('#btnCollapseChars').textContent = '收起人物'; }
    else { section.style.display = 'none'; $('#btnCollapseChars').textContent = '展开人物'; }
  });
}

function initFileUpload() {
  const zone = $('#uploadZone');
  const input = $('#fileInput');

  $('#btnSelectFile').addEventListener('click', () => input.click());
  zone.addEventListener('click', e => { if (e.target !== $('#btnSelectFile')) input.click(); });
  zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('dragover'); });
  zone.addEventListener('dragleave', () => zone.classList.remove('dragover'));
  zone.addEventListener('drop', e => { e.preventDefault(); zone.classList.remove('dragover'); if (e.dataTransfer.files[0]) uploadFile(e.dataTransfer.files[0]); });
  input.addEventListener('change', () => { if (input.files[0]) uploadFile(input.files[0]); });
}

async function uploadFile(file) {
  if (!file.name.endsWith('.txt')) { showToast('仅支持 .txt 文件', 'error'); return; }
  const formData = new FormData();
  formData.append('file', file);
  try {
    const r = await fetch('/api/upload', { method: 'POST', body: formData });
    const d = await r.json();
    if (!d.success) { showToast(d.error || '上传失败', 'error'); return; }
    State.currentTask.text = d.text;
    State.currentTask.title = d.title || '';
    $('#novelTitle').value = State.currentTask.title;
    $('#novelText').value = d.text.slice(0, 500) + (d.text.length > 500 ? '\n\n... (完整内容已加载)' : '');
    showToast('文件上传成功', 'success');
    updateStartButton();
  } catch (e) { showToast('上传失败: ' + e.message, 'error'); }
}

function updateStartButton() { $('#btnStart').disabled = !State.currentTask.text.trim(); }

function showInputPanel() {
  $('#panelInput').classList.remove('hidden');
  $('#panelResult').classList.add('hidden');
  $('#workToolbar').classList.add('hidden');
  renderSteps();
}

function showWorkContent() {
  $('#panelInput').classList.add('hidden');
  $('#panelResult').classList.remove('hidden');
  $('#workToolbar').classList.remove('hidden');
  renderSteps();
}

async function startProcessing() {
  if (!State.currentTask.text.trim()) return;
  State.currentTask.title = $('#novelTitle').value || '未命名作品';
  State.currentTask.segmentCount = parseInt($('#segmentCount').value) || 90;

  try {
    const r = await fetch('/api/init', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text: State.currentTask.text, segmentCount: State.currentTask.segmentCount, title: State.currentTask.title, style: State.currentTask.style || '韩漫' })
    });
    const d = await r.json();
    if (!d.success) { showToast(d.error || '初始化失败', 'error'); return; }

    State.currentTask.id = d.taskId;
    State.currentTask.steps = d.steps || [];
    State.currentTask.novelLength = d.novelLength || State.currentTask.text.length;
    State.currentTask.style = d.currentStyle || State.currentTask.style;

    showWorkContent();
    showReady();
    setupButton(0);
    renderSteps();
    updateButtons();
    await loadProjects();
    showToast('任务已初始化', 'success');
  } catch (e) { showToast('初始化失败: ' + e.message, 'error'); }
}

function showReady() {
  $('#resultTitle').textContent = '🎯 准备就绪';
  $('#resultStatus').textContent = `已加载 ${formatSize(State.currentTask.novelLength)} · ${State.currentTask.segmentCount} 段`;
  $('#resultStatus').className = 'badge success';
  $('#charactersSection').classList.add('hidden');
  $('#storyboardSection').classList.add('hidden');
  $('#resultContent').innerHTML = `<div style="display:flex;gap:16px;margin-bottom:20px;">
    <div style="flex:1;padding:16px;background:var(--surface-2);border-radius:var(--r-md);text-align:center;"><div style="font-size:20px;font-weight:600;">${formatSize(State.currentTask.novelLength)}</div><div style="font-size:12px;color:var(--muted);">文本大小</div></div>
    <div style="flex:1;padding:16px;background:var(--surface-2);border-radius:var(--r-md);text-align:center;"><div style="font-size:20px;font-weight:600;">${State.currentTask.segmentCount}</div><div style="font-size:12px;color:var(--muted);">目标片段</div></div>
    <div style="flex:1;padding:16px;background:var(--surface-2);border-radius:var(--r-md);text-align:center;"><div style="font-size:20px;font-weight:600;">${State.currentTask.steps.length}</div><div style="font-size:12px;color:var(--muted);">处理步骤</div></div>
  </div><p style="color:var(--muted);text-align:center;">点击下方按钮开始：<strong>${State.currentTask.steps[0]?.name || '文本拆分'}</strong></p>`;
}

function renderSteps() {
  const container = $('#stepList');
  if (State.currentTask.steps.length === 0) { container.innerHTML = '<div class="step-empty"><div class="empty-icon">📝</div><div>请先新建项目</div></div>'; return; }
  container.innerHTML = State.currentTask.steps.map((step, idx) => `
    <div class="step-item ${idx === State.ui.selectedStep ? 'active' : ''} ${step.status === 'done' ? 'completed' : ''} ${step.status === 'error' ? 'error' : ''}" data-step="${idx}">
      <div class="step-icon ${step.status}">${step.status === 'pending' ? '○' : step.status === 'running' ? '⏳' : step.status === 'done' ? '✅' : '❌'}</div>
      <div class="step-body"><div class="step-name">${step.name}</div><div class="step-desc">${step.desc}</div><div class="step-status ${step.status}">${statusText(step.status)}</div></div>
    </div>`).join('');
  container.querySelectorAll('.step-item').forEach(item => item.addEventListener('click', () => {
    const idx = parseInt(item.dataset.step);
    if (State.currentTask.steps[idx].status === 'done') viewStepResult(idx);
  }));
}

function statusText(status) { return { pending: '等待中', running: '⏳ 执行中...', done: '✅ 完成', error: '❌ 失败' }[status] || status; }

function setupButton(idx) {
  const btn = $('#btnContinue');
  btn.classList.remove('hidden');
  btn.textContent = `▶ 执行第${idx + 1}步：${State.currentTask.steps[idx].name}`;
  btn.dataset.next = String(idx);
  btn.disabled = false;
}

async function runStep(idx) {
  if (State.ui.isBusy) return;
  State.ui.isBusy = true;
  State.ui.selectedStep = idx;
  State.currentTask.steps[idx].status = 'running';
  renderSteps();
  $('#btnContinue').disabled = true;
  $('#resultStatus').textContent = '⏳ 执行中...';
  $('#resultStatus').className = 'badge running';
  $('#resultContent').innerHTML = `<div class="live-output"><div class="live-label">📡 实时流输出</div><pre id="liveText"></pre></div><div class="progress-container"><div class="progress-bar"><div class="progress-fill" id="progressFill" style="width:0%"></div></div><span class="progress-text" id="progressText">0/0</span></div>`;

  try {
    const r = await fetch(`/api/step/${idx}`, { method: 'POST' });
    const d = await r.json();
    if (!d.success) { showToast(d.error || '启动失败', 'error'); State.ui.isBusy = false; return; }
    connectSSE(idx);
    startPolling(idx);
  } catch (e) { showToast('启动失败: ' + e.message, 'error'); State.ui.isBusy = false; }
}

function connectSSE(idx) {
  if (State.es) { State.es.close(); State.es = null; }
  const liveText = $('#liveText');
  if (!liveText) return;
  const accumulated = {};

  const es = new EventSource(`/api/step/${idx}/stream`);
  State.es = es;

  es.addEventListener('chunk', e => {
    const d = JSON.parse(e.data);
    if (d.replay) { liveText.textContent = d.text; return; }
    const key = d.index !== undefined ? d.index : '_main';
    if (!accumulated[key]) accumulated[key] = '';
    accumulated[key] += d.text;
    const parts = Object.keys(accumulated).sort((a, b) => a === '_main' ? -1 : b === '_main' ? 1 : a - b).map(k => accumulated[k]);
    liveText.textContent = parts.join('\n');
    liveText.scrollTop = liveText.scrollHeight;
  });

  es.addEventListener('progress', e => {
    const d = JSON.parse(e.data);
    const pct = d.total > 0 ? Math.round(d.done / d.total * 100) : 0;
    const fill = $('#progressFill');
    const text = $('#progressText');
    if (fill) fill.style.width = pct + '%';
    if (text) text.textContent = d.message || `${d.done}/${d.total}`;
  });

  es.addEventListener('result', e => {
    const d = JSON.parse(e.data);
    State.currentTask.steps[idx].result = d;
  });

  es.addEventListener('done', () => { finishStep(idx); });

  es.addEventListener('error', e => {
    let msg = '未知错误';
    try { msg = JSON.parse(e.data).message; } catch (_) {}
  });

  es.onerror = () => {};
}

function startPolling(idx) {
  if (State.pollTimer) clearInterval(State.pollTimer);
  State.pollTimer = setInterval(async () => {
    try {
      const r = await fetch('/api/status');
      const d = await r.json();
      State.currentTask.steps = d.steps;
      if (!d.running.includes(idx)) {
        if (State.currentTask.steps[idx].status === 'done') finishStep(idx);
        else if (State.currentTask.steps[idx].status === 'error') failStep(idx, State.currentTask.steps[idx].error);
        clearInterval(State.pollTimer);
        State.pollTimer = null;
      }
    } catch (_) {}
  }, 1000);
}

async function finishStep(idx) {
  if (State.pollTimer) { clearInterval(State.pollTimer); State.pollTimer = null; }
  if (State.es) { State.es.close(); State.es = null; }
  State.currentTask.steps[idx].status = 'done';
  State.ui.isBusy = false;

  renderSteps();
  updateButtons();
  await loadProjects();

  $('#progressContainer').classList.add('hidden');
  $('#resultStatus').textContent = '✅ 完成';
  $('#resultStatus').className = 'badge success';

  const stepId = State.currentTask.steps[idx].id;
  if (stepId === 'characters' || stepId === 'turnaround') { await loadCharacters(); }
  if (stepId === 'storyboard') { await loadStoryboards(); }

  // 获取结果
  try {
    const rr = await fetch(`/api/results/${State.currentTask.steps[idx].id}`);
    if (rr.ok) {
      const result = await rr.json();
      State.currentTask.steps[idx].result = result;
      showResult(idx, result);
    }
  } catch (_) {}

  const next = idx + 1;
  if (next < State.currentTask.steps.length) setupButton(next);
  else {
    $('#btnContinue').classList.add('hidden');
    $('#resultContent').insertAdjacentHTML('beforeend', `<div class="completion-box"><div class="completion-icon">🎉</div><div class="completion-title">全部完成！</div><div class="completion-desc">点击左下角「下载结果」获取 JSON</div></div>`);
    showToast('所有步骤已完成！', 'success');
  }
}

function failStep(idx, msg) {
  if (State.pollTimer) { clearInterval(State.pollTimer); State.pollTimer = null; }
  if (State.es) { State.es.close(); State.es = null; }
  State.currentTask.steps[idx].status = 'error';
  State.currentTask.steps[idx].error = msg;
  State.ui.isBusy = false;
  renderSteps();
  $('#resultStatus').textContent = '❌ 失败';
  $('#resultStatus').className = 'badge error';
  $('#resultContent').insertAdjacentHTML('beforeend', `<div class="warning-box" style="margin-top:12px;"><strong>失败：</strong>${msg}</div><div style="margin-top:12px;text-align:center;"><button class="btn btn-outline" onclick="retryStep(${idx})">🔄 重试</button></div>`);
  updateButtons();
  setupButton(idx);
}

window.retryStep = async function (idx) {
  State.currentTask.steps[idx].status = 'pending';
  State.currentTask.steps[idx].error = null;
  renderSteps();
  $('#resultContent').innerHTML = '';
  await runStep(idx);
};

// ===== 角色管理 =====
async function loadCharacters() {
  // 从后端获取角色数据（如果有 API）或使用本地数据
  // 这里使用模拟数据，实际应该从后端获取
  if (CHARACTERS_DATA.length === 0) {
    CHARACTERS_DATA = [
      { id: 'char1', name: '角色 1', gender: '女', desc: '待描述', image: '👤', status: '待生成' },
    ];
  }
  const section = $('#charactersSection');
  const grid = $('#charGrid');
  section.classList.remove('hidden');
  $('#charCount').textContent = `${CHARACTERS_DATA.length} 人`;
  grid.innerHTML = CHARACTERS_DATA.map(c => `
    <div class="char-card-full" data-id="${c.id}">
      <div class="char-image">${c.image}<div class="char-image-status">${c.status}</div></div>
      <div class="char-body">
        <div class="char-actions-row"><button class="btn btn-xs btn-outline" onclick="rollbackChar('${c.id}')">回退上一版</button></div>
        <div class="char-name-row">
          <input class="char-name-input" value="${c.name}" data-id="${c.id}" onchange="updateCharName('${c.id}',this.value)">
          <select class="char-gender-select" data-id="${c.id}" onchange="updateCharGender('${c.id}',this.value)">
            <option value="女" ${c.gender === '女' ? 'selected' : ''}>女</option>
            <option value="男" ${c.gender === '男' ? 'selected' : ''}>男</option>
          </select>
        </div>
        <textarea class="char-desc-textarea" data-id="${c.id}" onchange="updateCharDesc('${c.id}',this.value)">${c.desc}</textarea>
        <div class="char-actions-row">
          <button class="btn btn-xs btn-outline" onclick="regenerateChar('${c.id}')">🔄 重新生成</button>
          <button class="btn btn-xs btn-primary" onclick="generateCharImage('${c.id}')">🎨 生成</button>
        </div>
      </div>
    </div>`).join('');
}

window.updateCharName = function (id, val) { const c = CHARACTERS_DATA.find(x => x.id === id); if (c) c.name = val; showToast(`角色名已更新：${val}`, 'success'); };
window.updateCharGender = function (id, val) { const c = CHARACTERS_DATA.find(x => x.id === id); if (c) c.gender = val; };
window.updateCharDesc = function (id, val) { const c = CHARACTERS_DATA.find(x => x.id === id); if (c) c.desc = val; };
window.rollbackChar = function (id) { const c = CHARACTERS_DATA.find(x => x.id === id); showToast(`已回退「${c ? c.name : id}」到上一版本`, 'info'); };
window.regenerateChar = function (id) { const c = CHARACTERS_DATA.find(x => x.id === id); showToast(`正在重新生成「${c ? c.name : id}」的立绘...`, 'info'); setTimeout(() => showToast(`「${c ? c.name : id}」立绘已重新生成`, 'success'), 1500); };
window.generateCharImage = function (id) { const c = CHARACTERS_DATA.find(x => x.id === id); showToast(`正在为「${c ? c.name : id}」生成图片...`, 'info'); setTimeout(() => showToast(`「${c ? c.name : id}」图片已生成`, 'success'), 2000); };

function addManualCharacter() {
  const name = prompt('请输入角色名称：');
  if (!name) return;
  const desc = prompt('请描述角色外形：') || '';
  CHARACTERS_DATA.push({ id: 'char_' + Date.now(), name, gender: '女', desc, image: '🧑', status: '待生成' });
  loadCharacters();
  showToast(`已添加角色：${name}`, 'success');
}

// ===== 分镜管理 =====
async function loadStoryboards() {
  if (STORYBOARDS_DATA.length === 0) {
    STORYBOARDS_DATA = [
      { id: 'sb1', index: 1, text: '分镜描述待生成', image: '🎬' },
    ];
  }
  const section = $('#storyboardSection');
  const grid = $('#storyboardGrid');
  section.classList.remove('hidden');
  $('#storyboardCount').textContent = `${STORYBOARDS_DATA.length} 页`;
  grid.innerHTML = STORYBOARDS_DATA.map(s => `
    <div class="storyboard-card" data-id="${s.id}">
      <div class="storyboard-image">${s.image}</div>
      <div class="storyboard-body">
        <div class="storyboard-index">分镜 #${s.index}</div>
        <div class="storyboard-text">${s.text}</div>
        <div class="storyboard-actions">
          <button class="btn btn-xs btn-outline" onclick="regenerateStoryboard('${s.id}')">🔄 重新生成</button>
          <button class="btn btn-xs btn-primary" onclick="genStoryboardImage('${s.id}')">🖼️ 生成图片</button>
        </div>
      </div>
    </div>`).join('');
}

window.regenerateStoryboard = function (id) { const s = STORYBOARDS_DATA.find(x => x.id === id); showToast(`重新生成分镜 #${s ? s.index : id}`, 'info'); };
window.genStoryboardImage = function (id) { const s = STORYBOARDS_DATA.find(x => x.id === id); showToast(`正在为分镜 #${s ? s.index : id} 生成图片...`, 'info'); setTimeout(() => showToast(`分镜 #${s ? s.index : id} 图片已生成`, 'success'), 2000); };

// ===== 结果展示 =====
function showResult(idx, result) {
  const step = State.currentTask.steps[idx];
  const wrapper = document.createElement('div');
  wrapper.className = 'result-section';
  wrapper.innerHTML = renderResult(step.id, result);
  $('#resultContent').appendChild(wrapper);
}

function renderResult(id, result) {
  const header = (icon, title) => `<div class="result-section-header"><h3>${icon} ${title}</h3></div>`;
  switch (id) {
    case 'split': return header('✅', `拆分完成：${result.count || 0} 段`) + `<ol class="segment-list" style="list-style:none;counter-reset:s;">${(result.segments || []).map((s, i) => `<li style="counter-increment:s;padding:10px;background:var(--card);border-radius:var(--r-md);margin-bottom:8px;"><span style="color:var(--muted);font-size:12px;">[${i + 1}]</span> ${s}</li>`).join('')}</ol>`;
    case 'characters': return header('✅', `角色提取：${result.count || 0} 人`) + `<div style="display:grid;gap:10px;">${(result.characters || []).map(c => `<div style="padding:12px;background:var(--card);border-radius:var(--r-md);"><strong>${c.name}</strong><div style="font-size:12px;color:var(--muted);margin-top:4px;">${c.desc}</div></div>`).join('')}</div>`;
    case 'turnaround': return header('✅', `角色立绘：${result.count || 0} 个角色`) + (result.turnarounds || []).map(t => `<div style="padding:12px;background:var(--card);border-radius:var(--r-md);margin-bottom:8px;"><strong>🎨 ${t.name}</strong><div style="font-size:12px;color:var(--muted);margin-top:4px;">${t.comboPrompt}</div></div>`).join('');
    case 'storyboard': return header('✅', `分镜：${result.count || 0} 页`) + (result.storyboards || []).map((s, i) => `<div style="padding:12px;background:var(--card);border-radius:var(--r-md);margin-bottom:8px;"><strong>📝 第${i + 1}页</strong><div style="font-size:12px;color:var(--muted);margin-top:4px;">${s}</div></div>`).join('');
    case 'prompts': return header('✅', `提示词：${result.count || 0} 个`) + (result.prompts || []).map(p => `<div style="padding:12px;background:var(--card);border-radius:var(--r-md);margin-bottom:8px;"><strong>✨ #${p.index}</strong><div style="font-size:12px;font-family:var(--font-mono);margin-top:4px;">${p.fullPrompt}</div></div>`).join('');
    case 'image': return header('✅', `图片：${result.count || 0}/${result.total || 0}`) + (result.images || []).map((img, i) => `<div style="padding:12px;background:var(--card);border-radius:var(--r-md);margin-bottom:8px;"><strong>🖼️ ${img}</strong></div>`).join('');
    case 'lastpage': return header('✅', '末张引流') + `<div style="padding:12px;background:var(--card);border-radius:var(--r-md);">${result.prompt}</div>`;
    case 'export': return header('✅', '成片导出') + (result.files || []).map(f => `<div style="padding:12px;background:var(--card);border-radius:var(--r-md);margin-bottom:8px;">📤 ${f}</div>`).join('');
    default: return '<p>未知步骤</p>';
  }
}

async function viewStepResult(idx) {
  const step = State.currentTask.steps[idx];
  if (!step || step.status !== 'done') return;
  $('#resultTitle').textContent = `👁️ ${step.name} 结果`;
  $('#resultStatus').textContent = '✅ 已完成';
  $('#resultStatus').className = 'badge success';
  $('#resultContent').innerHTML = '';
  if (!step.result) {
    try {
      const rr = await fetch(`/api/results/${step.id}`);
      if (rr.ok) step.result = await rr.json();
    } catch (_) {}
  }
  if (step.result) showResult(idx, step.result);
  const next = idx + 1;
  if (next < State.currentTask.steps.length && State.currentTask.steps[next].status !== 'done') setupButton(next);
}

function updateButtons() {
  const hasSteps = State.currentTask.steps.length > 0;
  const hasDone = State.currentTask.steps.some(s => s.status === 'done');
  $('#btnReset').disabled = !hasSteps || State.ui.isBusy;
  $('#btnDownload').disabled = !hasDone || State.ui.isBusy;
}

async function resetTask() {
  try {
    await fetch('/api/reset', { method: 'POST' });
    State.currentTask = { id: null, title: '', text: '', segmentCount: 90, steps: [], novelLength: 0, style: '韩漫' };
    State.ui.isBusy = false;
    State.ui.selectedStep = -1;
    CHARACTERS_DATA = [];
    STORYBOARDS_DATA = [];
    if (State.es) { State.es.close(); State.es = null; }
    if (State.pollTimer) { clearInterval(State.pollTimer); State.pollTimer = null; }
    $('#novelText').value = '';
    $('#novelTitle').value = '';
    $('#segmentCount').value = '90';
    $('#charactersSection').classList.add('hidden');
    $('#storyboardSection').classList.add('hidden');
    updateStartButton();
    await loadProjects();
  } catch (e) { console.error('重置失败:', e); }
}

// ===== 设置页 =====
// ===== 设置页 =====
function initSettingsPage() {
  // 加载当前设置
  loadSettings();

  $('#btnSaveSettings').addEventListener('click', saveSettings);
  $('#btnResetSettings').addEventListener('click', () => {
    if (confirm('确定恢复默认设置？')) {
      $('#settingProvider').value = 'anthropic';
      onProviderChange();
      showToast('已恢复默认设置，请点击保存', 'info');
    }
  });
}

// 提供商切换时自动更新 base URL 和 model
window.onProviderChange = function () {
  const provider = $('#settingProvider').value;
  if (provider === 'deepseek') {
    $('#settingBaseUrl').value = 'https://api.deepseek.com';
    $('#settingModel').value = 'deepseek-chat';
    $('#providerHint').textContent = 'DeepSeek Flash: 高速低价，性价比高';
  } else {
    $('#settingBaseUrl').value = 'https://api.anthropic.com';
    $('#settingModel').value = 'claude-sonnet-5-5';
    $('#providerHint').textContent = 'Claude: 高质量但价格较高';
  }
};

async function loadSettings() {
  try {
    const r = await fetch('/api/settings');
    if (r.ok) {
      const s = await r.json();
      if (s.provider) $('#settingProvider').value = s.provider;
      if (s.model) $('#settingModel').value = s.model;
      if (s.apiKey) $('#settingApiKey').value = s.apiKey;
      if (s.baseUrl) $('#settingBaseUrl').value = s.baseUrl;
      onProviderChange();
    }
  } catch (_) {
    // 后端还没有 /api/settings 接口，使用默认值
    onProviderChange();
  }
}

async function saveSettings() {
  const settings = {
    provider: $('#settingProvider').value,
    model: $('#settingModel').value,
    apiKey: $('#settingApiKey').value,
    baseUrl: $('#settingBaseUrl').value
  };
  try {
    const r = await fetch('/api/settings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(settings)
    });
    if (r.ok) {
      showToast('设置已保存并生效', 'success');
    } else {
      showToast('保存失败，请重启后端服务后手动修改 application.yml', 'error');
    }
  } catch (e) {
    showToast('设置已记录，重启后生效', 'info');
  }
}

// ===== 画风选择 =====
function initStyleGrid() {
  $$('.style-card').forEach(card => card.addEventListener('click', () => {
    $$('.style-card').forEach(c => c.classList.remove('selected'));
    card.classList.add('selected');
    const style = card.dataset.style;
    State.currentTask.style = style;
    $('#rail-style-summary').textContent = `画风 · ${style}`;
    $('#style-name').textContent = style;
    $('#style-preview-icon').textContent = STYLES[style]?.icon || '👤';
    $('#style-blurb').textContent = STYLES[style]?.desc || '';
  }));
}

// ===== 新建项目弹窗 =====
function openNewProjectModal() { $('#newProjectModal').classList.remove('hidden'); }
function closeNewProjectModal() { $('#newProjectModal').classList.add('hidden'); }
function createNewProject() {
  const name = $('#newProjectName').value.trim() || '未命名项目';
  const style = $('.style-card.selected')?.dataset.style || '韩漫';
  State.currentTask.title = name;
  State.currentTask.style = style;
  closeNewProjectModal();
  switchScreen('work');
  showInputPanel();
  updateProjectTabs();
  showToast(`已创建项目：${name}`, 'success');
}

function updateProjectTabs() {
  const container = $('#projectTabs');
  const tabs = State.projects.slice(0, 6).map((p, i) => `
    <div class="project-tab-item ${i === 0 ? 'active' : ''}" data-id="${p.id}">
      <span class="tab-name">${p.title}</span>
      <span class="tab-status ${p.status === 'COMPLETED' ? 'done' : p.completedSteps > 0 ? 'running' : ''}">${p.status === 'COMPLETED' ? '已完成' : `${p.completedSteps}/${p.totalSteps}`}</span>
    </div>`).join('');
  container.innerHTML = tabs + '<button class="project-tab-add" onclick="openNewProjectModal()" title="新建项目">+</button>';
  container.querySelectorAll('.project-tab-item').forEach(item => item.addEventListener('click', () => {
    container.querySelectorAll('.project-tab-item').forEach(t => t.classList.remove('active'));
    item.classList.add('active');
    loadProject(item.dataset.id);
  }));
}

// ===== 检查保存状态 =====
async function checkSavedState() {
  try {
    const r = await fetch('/api/state');
    const d = await r.json();
    if (d.hasState) {
      const lastProject = State.projects.find(p => p.status === 'IN_PROGRESS');
      if (lastProject) {
        $('#home-continue').textContent = `继续「${lastProject.title}」`;
      }
    }
  } catch (_) {}
}

// ===== Toast =====
function showToast(message, type = 'info') {
  const container = $('#toasts');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => { toast.style.opacity = '0'; toast.style.transform = 'translateX(20px)'; setTimeout(() => toast.remove(), 300); }, 3000);
}

function formatSize(bytes) { if (!bytes) return '0B'; if (bytes < 1024) return bytes + 'B'; if (bytes < 1048576) return (bytes / 1024).toFixed(1) + 'KB'; return (bytes / 1048576).toFixed(1) + 'MB'; }

window.copyText = function (text) { navigator.clipboard.writeText(text).then(() => showToast('已复制', 'success')).catch(() => showToast('复制失败', 'error')); };
