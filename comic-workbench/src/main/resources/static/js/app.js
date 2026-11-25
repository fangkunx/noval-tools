/**
 * 小说推文漫画工作台 - 前端应用
 * 连接后端 API 实现数据持久化
 */
const API_BASE = '';
const view = document.querySelector('#view');
const modalRoot = document.querySelector('#modal-root');
const toast = document.querySelector('#toast');
const scriptList = document.querySelector('#script-list');

let currentNovelId = null;
let currentView = 'novels';
let storyPage = 1;
let storyPageSize = 10;
let storyTotal = 0;
let storyRenderToken = 0; // 防止并发请求时旧响应覆盖新页面

// ========== API 调用 ==========
async function api(path, options = {}) {
  const resp = await fetch(API_BASE + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  });
  if (!resp.ok) {
    const err = await resp.json().catch(() => ({ error: 'Unknown error' }));
    throw new Error(err.message || err.error || 'Request failed');
  }
  if (resp.status === 204) return null;
  return resp.json();
}

// ========== 小说列表（剧本列表） ==========
async function loadNovelList() {
  try {
    const novels = await api('/api/novels');
    scriptList.innerHTML = novels.map((n, i) =>
      `<div class="script ${i === 0 ? 'active' : ''}" data-novel-id="${n.id}">▤ <span>${n.title || '未命名'}</span><i>id:${n.id}</i><b></b></div>`
    ).join('');
    if (novels.length > 0) {
      currentNovelId = novels[0].id;
    }
  } catch (e) {
    scriptList.innerHTML = '<div class="script">加载失败</div>';
  }
}

// ========== 视图渲染 ==========
function shell(title, sub, actions = '') {
  return `<div class="page-head"><div><h1>${title}</h1><p>${sub}</p></div><div class="head-actions">${actions}</div></div>`;
}

function chips(items, activeIndex = 0) {
  return `<div class="chips">${items.map((x, i) =>
    `<button class="chip ${i === activeIndex ? 'active' : ''}">${x}</button>`
  ).join('')}</div>`;
}

function addCard(label = '创建新内容') {
  return `<div class="card add-card" data-action="add"><strong>＋</strong><span>${label}</span></div>`;
}

async function show(viewName = 'novels') {
  currentView = viewName;
  document.querySelectorAll('.main-nav button').forEach(b =>
    b.classList.toggle('active', b.dataset.view === viewName)
  );

  switch (viewName) {
    case 'novels': await renderNovels(); break;
    case 'assets': await renderAssets(); break;
    case 'styles': await renderStyles(); break;
    case 'prompts': await renderPrompts(); break;
    case 'accounts': await renderAccounts(); break;
    case 'story': await renderStory(); break;
  }
}

// ========== 小说库 ==========
async function renderNovels() {
  let novels = [];
  try { novels = await api('/api/novels'); } catch (e) {}

  view.innerHTML = `
    <div class="novels">
      ${shell('我的小说', '历史导入的小说内容，以卡片形式展示并支持编辑。',
        `<button class="btn gold" data-action="select">选择</button>
         <input class="search" placeholder="⌕ 搜索小说名字或者小说内容..." />`)}
      ${chips(['全部', '知乎', '番茄', '老福特', '其他'])}
      <div class="grid novels-grid">
        ${addCard('创建小说')}
        ${novels.map(n => `
          <div class="card novel-card ${n.vip ? 'vip' : ''}" data-novel-id="${n.id}" data-action="open-novel">
            <div class="bookmark">${n.vip ? '♛' : '♡'}</div>
            <h3>${n.title || '未命名'}</h3>
            <p class="meta">${n.vip ? 'VIP 持续更新' : (n.source || '')}</p>
            <div class="novel-text">${n.content || '暂无内容'}</div>
          </div>
        `).join('')}
      </div>
    </div>`;
}

// ========== 资产库 ==========
async function renderAssets() {
  let assets = [];
  try { assets = await api('/api/assets'); } catch (e) {}

  view.innerHTML = `
    <div class="assets">
      ${shell('我的资产', '角色 / 场景 / 道具的三视图与生成结果。',
        `<button class="btn gold" data-action="select">选择资产</button>
         <button class="btn teal" data-action="batch">一键形象</button>
         <button class="btn gold" data-action="batch">一键打标</button>
         <button class="btn" data-action="batch">一键链接</button>
         <input class="search" placeholder="⌕ 搜索资产名称或描述..." />`)}
      <div class="toolbar">
        <span class="filter-label">类型：</span>
        <button class="chip active" data-filter-type="all">全部</button>
        <button class="chip" data-filter-type="CHARACTER">角色</button>
        <button class="chip" data-filter-type="SCENE">场景</button>
        <button class="chip" data-filter-type="PROP">道具</button>
        <span class="filter-label">内容：</span>
        <button class="chip active">古代</button>
        <button class="chip">现代</button>
        <button class="chip">科幻</button>
        <button class="chip">玄幻</button>
      </div>
      <div class="grid">
        ${addCard('创建资产')}
        ${assets.map(a => `
          <div class="card" data-asset-id="${a.id}">
            <div class="asset-art ${a.type === 'CHARACTER' ? 'character' : ''}">
              ${a.type === 'CHARACTER' ? '♙' : a.type === 'SCENE' ? '▧' : '◉'}
            </div>
            <div class="card-body">
              <strong>${a.name || '未命名'}</strong>
              <div class="meta">♟ ${a.type || '角色'}<br/>ID: ${a.id}</div>
            </div>
          </div>
        `).join('')}
      </div>
    </div>`;
}

// ========== 画风库 ==========
async function renderStyles() {
  let styles = [];
  try { styles = await api('/api/styles'); } catch (e) {}

  view.innerHTML = `
    <div>
      ${shell('画风库', '生成任务三视图与基础图片的画风提示词模板。',
        `<button class="btn teal" data-action="style-add">＋ 新增画风</button>
         <input class="search" placeholder="⌕ 搜索画风..." />`)}
      <div class="grid">
        ${addCard('新增画风')}
        ${styles.map(s => `
          <div class="card" data-style-id="${s.id}">
            <div class="asset-art">✦</div>
            <h3>${s.name || '未命名'}</h3>
            <p>${s.description || '暂无描述'}</p>
            <div class="meta">已使用 ${s.usageCount || 0} 次</div>
          </div>
        `).join('')}
      </div>
    </div>`;
}

// ========== 提示词 ==========
async function renderPrompts() {
  let prompts = [];
  try { prompts = await api('/api/prompts'); } catch (e) {}

  view.innerHTML = `
    <div>
      ${shell('我的提示词', '按业务场景管理可复用的前后缀与推理模板。',
        `<button class="btn gold" data-action="select">选择</button>
         <input class="search" placeholder="⌕ 搜索提示词标题、内容或分类..." />`)}
      ${chips(['全部', '角色设定', '场景描述', '情节发展', '对话生成', '画风描述', '前后缀', '爆款开头', '其他'])}
      <div class="grid">
        ${addCard('添加提示词')}
        ${prompts.map(p => `
          <div class="card" data-prompt-id="${p.id}">
            <p># ${p.title || '未命名'}</p>
            <p style="margin-top:14px">${p.content || '暂无内容'}</p>
            <div class="tags">
              <span class="tag teal">${p.category || '其他'}</span>
              <button class="btn teal" style="margin-left:auto;padding:4px 9px" data-action="copy" data-content="${encodeURIComponent(p.content || '')}">复制</button>
            </div>
            <div class="meta">id:${p.id}</div>
          </div>
        `).join('')}
      </div>
    </div>`;
}

// ========== 账号池 ==========
async function renderAccounts() {
  let accounts = [];
  try { accounts = await api('/api/accounts'); } catch (e) {}

  view.innerHTML = `
    <div>
      ${shell('账号池服务管理（API）', '管理各个生成服务的开启状态及快捷操作。',
        `<button class="btn" data-action="export">⇩</button>
         <button class="btn" data-action="export">⇧</button>`)}
      <div class="grid">
        ${accounts.map(a => `
          <div class="card service-card ${a.enabled ? '' : 'off'}" data-account-id="${a.id}">
            <div class="brandline">
              <span>◉ ${a.displayName || a.name || '未命名'}</span>
              <span class="switch ${a.enabled ? 'on' : ''}" data-action="toggle-account" data-id="${a.id}"></span>
            </div>
            <p>${a.protocol || 'openai'} 协议</p>
            <div class="tags">
              ${(a.capabilities || []).map(c => `<span class="tag">${c === 'TEXT' ? '文本' : '图像'}</span>`).join('')}
            </div>
            <div class="status">${a.enabled ? '● 服务可用' : '○ 服务已关闭'}</div>
            <button class="btn" style="width:100%" data-action="account-settings" data-id="${a.id}">密钥与设置</button>
          </div>
        `).join('')}
        <div class="card service-card">
          <div class="brandline"><span>♛ VIP 自定义线路</span><span class="switch on"></span></div>
          <p>需激活 VIP，添加自备中转线路，支持文本推理与图像生成。</p>
          <div class="status">● 已配置 文本2 · 图像1</div>
          <button class="btn gold" style="width:100%" data-action="custom-line">管理自定义线路</button>
        </div>
        ${addCard('添加账号')}
      </div>
    </div>`;
}

// ========== 分镜台 ==========
async function renderStory() {
  if (!currentNovelId) {
    view.innerHTML = shell('小说分镜台', '请先选择一个小说。可在"小说库"中点击卡片进入。');
    return;
  }

  const token = ++storyRenderToken;
  const novelId = currentNovelId;
  const page = storyPage;
  const size = storyPageSize;

  let shots = [];
  let total = 0;
  try {
    const result = await api(`/api/shots?novelId=${novelId}&page=${page}&size=${size}`);
    if (token !== storyRenderToken) return; // 已有更新的渲染请求，丢弃本次结果
    shots = result.data || [];
    total = result.total || 0;
    storyTotal = total;
  } catch (e) {}
  let novel = null;
  try { novel = await api(`/api/novels/${novelId}`); } catch (e) {}
  if (token !== storyRenderToken) return;

  const totalPages = storyPageSize > 0 ? Math.ceil(total / storyPageSize) : 1;

  view.innerHTML = `
    <div class="story-view">
      ${shell('小说分镜台', `《${novel ? novel.title : '未命名'}》 · 共 ${total} 个分镜`,
        `<button class="btn" data-action="batch-infer">批量推理</button>
         <button class="btn teal" data-action="batch-generate">批量生图</button>
         <button class="btn" data-action="comic-settings">漫画设置</button>
         <button class="btn gold" data-action="export">导出漫画</button>`)}
      <div class="storyboard">
        <table class="storyboard-table">
          <thead>
            <tr>
              <th class="col-seq">序号</th>
              <th class="col-content">画面内容</th>
              <th class="col-preset">预设 <button class="btn-sm" data-action="open-preset-panel" title="设置预设">⚙</button></th>
              <th class="col-prompt">图片描述词</th>
              <th class="col-main">主图</th>
              <th class="col-alt">备选图</th>
              <th class="col-op">操作</th>
            </tr>
          </thead>
          <tbody>
            ${shots.length === 0 ? `<tr><td colspan="7" class="empty-row">暂无分镜，请从"小说库"打开小说并点击"导入剧本"</td></tr>` : ''}
            ${shots.map(s => `
              <tr data-shot-id="${s.id}">
                <td class="col-seq"><span class="seq-num">${s.sequence}</span></td>
                <td class="col-content"><textarea class="cell-edit" data-shot-id="${s.id}" data-field="originalText" title="失焦自动保存">${escapeHtml(s.originalText || '')}</textarea></td>
                <td class="col-preset">
                  <div class="cell-tags">
                    ${(s.tags || []).map(t => `<span class="tag teal">${escapeHtml(t)}</span>`).join('')}
                    ${(s.tags || []).length === 0 ? '<span class="muted">待推理</span>' : ''}
                  </div>
                </td>
                <td class="col-prompt"><textarea class="cell-edit" data-shot-id="${s.id}" data-field="promptText" title="失焦自动保存">${escapeHtml(s.promptText || '')}</textarea></td>
                <td class="col-main">
                  <div class="main-image-cell">
                    ${s.mainImageUrl
                      ? `<img src="${s.mainImageUrl}" class="main-thumb" data-shot-id="${s.id}" data-action="preview-main" />`
                      : `<div class="empty-main">暂无主图</div>`}
                  </div>
                </td>
                <td class="col-alt">
                  <div class="alt-images-cell">
                    ${(s.altImageUrls || []).map(url =>
                      `<img src="${url}" class="alt-thumb ${s.mainImageUrl === url ? 'selected' : ''}"
                         data-shot-id="${s.id}" data-image-url="${url}" data-action="select-main-image"
                         title="点击设为主图" />`
                    ).join('')}
                    ${(s.altImageUrls || []).length === 0 ? '<div class="empty-alt">待生成</div>' : ''}
                  </div>
                </td>
                <td class="col-op">
                  <div class="cell-actions">
                    <button class="btn-sm teal" data-action="infer-shot" data-shot-id="${s.id}" title="推理提示词">推理词</button>
                    <button class="btn-sm" data-action="generate-shot" data-shot-id="${s.id}" title="生成图片">生图</button>
                    <button class="btn-sm" data-action="edit-shot" data-shot-id="${s.id}" title="编辑分镜">编辑</button>
                    <button class="btn-sm danger" data-action="delete-shot" data-shot-id="${s.id}" title="删除分镜">删除</button>
                  </div>
                </td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>
      <div class="bottom-bar pagination-bar">
        <div class="pagination-info">
          <span>共 <b style="color:var(--teal)">${total}</b> 条</span>
          <span>每页</span>
          <select class="page-size-select" data-action="change-page-size">
            <option value="10" ${storyPageSize === 10 ? 'selected' : ''}>10</option>
            <option value="20" ${storyPageSize === 20 ? 'selected' : ''}>20</option>
            <option value="50" ${storyPageSize === 50 ? 'selected' : ''}>50</option>
            <option value="100" ${storyPageSize === 100 ? 'selected' : ''}>100</option>
            <option value="0" ${storyPageSize === 0 ? 'selected' : ''}>全部</option>
          </select>
          <span>条</span>
        </div>
        <div class="pagination-controls">
          <button class="btn" data-action="first-page" ${storyPage <= 1 ? 'disabled' : ''}>首页</button>
          <button class="btn" data-action="prev-page" ${storyPage <= 1 ? 'disabled' : ''}>上一页</button>
          <span class="page-indicator">${storyPageSize > 0 ? `第 ${storyPage} / ${totalPages} 页` : '全部'}</span>
          <button class="btn" data-action="next-page" ${storyPageSize <= 0 || storyPage >= totalPages ? 'disabled' : ''}>下一页</button>
          <button class="btn" data-action="last-page" ${storyPageSize <= 0 || storyPage >= totalPages ? 'disabled' : ''}>末页</button>
        </div>
      </div>
    </div>`;
}

// ========== 弹窗 ==========
function openModal(title, body) {
  modalRoot.innerHTML = `
    <div class="modal-back">
      <div class="modal">
        <h2>${title}</h2>
        ${body}
        <div class="modal-foot">
          <button class="btn" data-close>取消</button>
          <button class="btn teal" data-save>保存</button>
        </div>
      </div>
    </div>`;
}

function notify(msg) {
  toast.textContent = msg;
  toast.classList.add('show');
  setTimeout(() => toast.classList.remove('show'), 1800);
}

/**
 * 打开小说详情编辑弹窗（参照设计稿 4.3 小说详情）
 * - 编辑标题 / 内容
 * - 清理序号（去除行首的 "1." / "第1章" / "【1】" 等）
 * - 导入剧本（按行拆分为分镜，一行一个分镜）
 */
async function openNovelDetail(novelId) {
  let novel;
  try {
    novel = await api(`/api/novels/${novelId}`);
  } catch (e) {
    notify('加载小说失败: ' + e.message);
    return;
  }
  if (!novel || !novel.id) {
    notify('小说不存在');
    return;
  }

  modalRoot.innerHTML = `
    <div class="modal-back novel-detail-back">
      <div class="modal novel-detail-modal">
        <div class="novel-detail-head">
          <h2>${novel.title || '未命名'}</h2>
          <div class="novel-detail-tags">
            ${novel.vip ? '<span class="tag gold">VIP</span>' : ''}
            ${novel.source ? `<span class="tag teal">${novel.source}</span>` : ''}
            <span class="tag">ID: ${novel.id}</span>
          </div>
        </div>

        <div class="novel-detail-toolbar">
          <label class="novel-detail-label">书名</label>
          <input id="novel-detail-title" class="novel-detail-title-input"
                 value="${escapeHtml(novel.title || '')}" placeholder="请输入书名" />
          <div class="novel-detail-actions">
            <button class="btn" data-action="clean-numbers" data-novel-id="${novel.id}">清理序号</button>
            <button class="btn teal" data-action="import-shots" data-novel-id="${novel.id}">导入剧本</button>
          </div>
        </div>

        <div class="novel-detail-body">
          <label class="novel-detail-label">小说内容 <small style="color:#888">（每行将作为一个分镜段落；可点击右上角"清理序号"去除行首编号；"导入剧本"后自动进入分镜台）</small></label>
          <textarea id="novel-detail-content" class="novel-detail-content"
                    placeholder="在此粘贴或编辑小说内容，一行一段...">${escapeHtml(novel.content || '')}</textarea>
        </div>

        <div class="modal-foot">
          <span id="novel-detail-hint" style="margin-right:auto;color:#888;font-size:12px"></span>
          <button class="btn" data-close>取消</button>
          <button class="btn gold" data-action="save-novel-detail" data-novel-id="${novel.id}">保存</button>
        </div>
      </div>
    </div>`;
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

// ========== 事件处理 ==========
// 处理 select 的 change 事件（分页大小选择）
document.addEventListener('change', async (e) => {
  const target = e.target;
  if (target.matches('.page-size-select')) {
    storyPageSize = parseInt(target.value);
    storyPage = 1;
    renderStory();
  }
});

// 分镜表格内编辑：失焦自动保存
let saveTimer = null;
document.addEventListener('focusout', async (e) => {
  const target = e.target;
  if (target.matches('.cell-edit')) {
    const shotId = target.dataset.shotId;
    const field = target.dataset.field;
    const value = target.value;
    if (!shotId || !field) return;
    // 防抖：300ms 后保存
    clearTimeout(saveTimer);
    saveTimer = setTimeout(async () => {
      try {
        // 先取完整分镜，再合并本次修改的字段后整体提交（PUT 为全量语义）
        const shot = await api(`/api/shots/${shotId}`);
        await api(`/api/shots/${shotId}`, {
          method: 'PUT',
          body: JSON.stringify({
            novelId: shot.novelId,
            sequence: shot.sequence,
            originalText: shot.originalText,
            sceneDescription: shot.sceneDescription,
            promptText: shot.promptText,
            mainImageUrl: shot.mainImageUrl,
            altImageUrls: shot.altImageUrls,
            tags: shot.tags,
            state: shot.state,
            [field]: value
          })
        });
        target.style.borderColor = 'var(--teal)';
        setTimeout(() => { target.style.borderColor = ''; }, 800);
      } catch (err) {
        target.style.borderColor = '#e74c3c';
        setTimeout(() => { target.style.borderColor = ''; }, 1500);
        notify('保存失败: ' + err.message);
      }
    }, 300);
  }
});

document.addEventListener('click', async (e) => {
  const target = e.target.closest('[data-view], [data-action], [data-novel-id], [data-filter-type], [data-close], [data-save], [data-tab]');
  if (!target) return;

  const viewName = target.dataset.view;
  const action = target.dataset.action;
  const novelId = target.dataset.novelId;
  const filterType = target.dataset.filterType;
  const tabName = target.dataset.tab;

  // Tab 切换（最高优先级）
  if (tabName && target.matches('.sp-tab')) {
    document.querySelectorAll('.sp-tab').forEach(b => b.classList.toggle('active', b.dataset.tab === tabName));
    const section = document.querySelector(`.sp-section[data-section="${tabName}"]`);
    if (section) {
      const body = document.getElementById('preset-panel-body');
      if (body) body.scrollTo({ top: section.offsetTop - 10, behavior: 'smooth' });
    }
    return;
  }

  // 导航切换
  if (viewName) {
    show(viewName);
    return;
  }

  // 动作处理（优先于 novelId，因为小说卡片同时有 data-action 和 data-novel-id）
  if (action === 'open-novel') {
    // 单击小说卡片 → 打开小说详情编辑（参照设计稿 4.3）
    console.log('点击小说卡片，打开详情:', novelId);
    openNovelDetail(parseInt(novelId));
    return;
  }

  // 小说选择（侧边栏剧本）
  if (novelId && !action) {
    currentNovelId = parseInt(novelId);
    storyPage = 1; // 重置分页
    document.querySelectorAll('.script').forEach(s => s.classList.remove('active'));
    target.classList.add('active');
    if (currentView === 'story') renderStory();
    return;
  }

  // 资产类型筛选
  if (filterType) {
    document.querySelectorAll('[data-filter-type]').forEach(b => b.classList.remove('active'));
    target.classList.add('active');
    return;
  }

  switch (action) {
    case 'save-novel-detail': {
      const nId = target.dataset.novelId;
      const newTitle = document.querySelector('#novel-detail-title')?.value || '';
      const newContent = document.querySelector('#novel-detail-content')?.value || '';
      try {
        await api(`/api/novels/${nId}`, {
          method: 'PUT',
          body: JSON.stringify({ title: newTitle, content: newContent })
        });
        notify('小说已保存');
        modalRoot.innerHTML = '';
        loadNovelList();
        renderNovels();
      } catch (e) {
        notify('保存失败: ' + e.message);
      }
      break;
    }

    case 'clean-numbers': {
      const nId = target.dataset.novelId;
      try {
        const updated = await api(`/api/novels/${nId}/clean-numbers`, { method: 'POST' });
        // 更新弹窗内容
        const ta = document.querySelector('#novel-detail-content');
        const titleInput = document.querySelector('#novel-detail-title');
        if (ta) ta.value = updated.content || '';
        if (titleInput) titleInput.value = updated.title || '';
        const hint = document.querySelector('#novel-detail-hint');
        if (hint) hint.textContent = '✓ 已清理行首序号';
        setTimeout(() => { if (hint) hint.textContent = ''; }, 2500);
        notify('序号已清理');
      } catch (e) {
        notify('清理失败: ' + e.message);
      }
      break;
    }

    case 'import-shots': {
      const nId = target.dataset.novelId;
      try {
        const res = await api(`/api/novels/${nId}/import-shots`, { method: 'POST' });
        notify(`已导入 ${res.importedCount} 个分镜`);
        currentNovelId = parseInt(nId);
        storyPage = 1; // 重置分页
        modalRoot.innerHTML = '';
        currentView = 'story';
        show('story');
      } catch (e) {
        notify('导入失败: ' + e.message);
      }
      break;
    }

    case 'new-script':
      openModal('新建剧本', `
        <div class="form-grid">
          <div class="field full"><label>名称</label><input id="novel-title" placeholder="请输入名称" /></div>
          <div class="field full"><label>内容描述</label><textarea id="novel-content" placeholder="请输入内容"></textarea></div>
        </div>`);
      break;

    case 'add':
      if (currentView === 'novels') {
        openModal('创建小说', `
          <div class="form-grid">
            <div class="field full"><label>标题</label><input id="novel-title" placeholder="请输入标题" /></div>
            <div class="field full"><label>内容</label><textarea id="novel-content" placeholder="请输入内容"></textarea></div>
            <div class="field"><label>来源</label><input id="novel-source" placeholder="如：知乎" /></div>
            <div class="field"><label>VIP</label><select id="novel-vip"><option value="false">否</option><option value="true">是</option></select></div>
          </div>`);
      } else if (currentView === 'prompts') {
        openModal('添加提示词', `
          <div class="form-grid">
            <div class="field full"><label>标题</label><input id="prompt-title" placeholder="请输入标题" /></div>
            <div class="field"><label>分类</label><input id="prompt-category" placeholder="如：角色设定" /></div>
            <div class="field"><label>内容</label><textarea id="prompt-content" placeholder="请输入内容"></textarea></div>
          </div>`);
      } else if (currentView === 'styles') {
        openModal('新增画风', `
          <div class="form-grid">
            <div class="field"><label>中文名称</label><input id="style-name" placeholder="如：Q版精致日系" /></div>
            <div class="field"><label>画风图片</label><input type="file" /></div>
            <div class="field full"><label>输出画风描述</label><textarea id="style-desc" placeholder="输入生成画风的提示词"></textarea></div>
          </div>`);
      } else if (currentView === 'assets') {
        openModal('创建资产', `
          <div class="form-grid">
            <div class="field"><label>名称</label><input id="asset-name" placeholder="请输入名称" /></div>
            <div class="field"><label>类型</label><select id="asset-type"><option value="CHARACTER">角色</option><option value="SCENE">场景</option><option value="PROP">道具</option></select></div>
            <div class="field"><label>风格</label><select id="asset-style"><option value="ANCIENT">古代</option><option value="MODERN">现代</option><option value="SCIFI">科幻</option><option value="FANTASY">玄幻</option></select></div>
            <div class="field full"><label>描述</label><textarea id="asset-desc" placeholder="请输入描述"></textarea></div>
          </div>`);
      } else if (currentView === 'accounts') {
        openModal('添加账号', `
          <div class="form-grid">
            <div class="field"><label>显示名称</label><input id="acc-display" placeholder="模型别名" /></div>
            <div class="field"><label>技术标准</label><select id="acc-protocol"><option>openai</option><option>gemini</option><option>anthropic</option><option>custom</option></select></div>
            <div class="field full"><label>API 地址</label><input id="acc-url" placeholder="https://api.example.com/v1" /></div>
            <div class="field"><label>ApiKey</label><input id="acc-key" type="password" placeholder="请求凭证" /></div>
            <div class="field"><label>模型编号</label><input id="acc-model" placeholder="model-id" /></div>
            <div class="field"><label>能力</label><select id="acc-cap" multiple><option value="TEXT">文本推理</option><option value="IMAGE">图片生成</option></select></div>
          </div>`);
      }
      break;

    case 'style-add':
      openModal('新增画风', `
        <div class="form-grid">
          <div class="field"><label>中文名称</label><input id="style-name" placeholder="如：Q版精致日系" /></div>
          <div class="field"><label>画风图片</label><input type="file" /></div>
          <div class="field full"><label>输出画风描述</label><textarea id="style-desc" placeholder="输入生成画风的提示词"></textarea></div>
        </div>`);
      break;

    case 'account-settings':
    case 'custom-line':
      openModal(action === 'custom-line' ? '管理自定义线路' : '密钥与设置', `
        <div class="form-grid">
          <div class="field"><label>技术标准</label><select><option>openai 协议</option><option>gemini 协议</option><option>anthropic 协议</option><option>自定义探测</option></select></div>
          <div class="field"><label>能力</label><select><option>文本推理</option><option>图片生成</option></select></div>
          <div class="field full"><label>API 地址</label><input placeholder="https://api.example.com/v1" /></div>
          <div class="field"><label>ApiKey</label><input type="password" placeholder="请求凭证" /></div>
          <div class="field"><label>模型编号</label><input placeholder="model-id" /></div>
          <div class="field full"><label>显示名称</label><input placeholder="模型别名" /></div>
        </div>`);
      break;

    case 'comic-settings':
    case 'preset':
      openModal(action === 'comic-settings' ? '漫画设置' : '预设设置', `
        <div class="form-grid">
          <div class="field"><label>图片推导词</label><select><option>图片推导词默认前缀</option><option>角色三视图</option><option>前后缀-测试</option></select></div>
          <div class="field"><label>推理文本模型</label><select><option>DeepSeek · 文本</option><option>Kimi · 文本</option></select></div>
          <div class="field"><label>推理图片模型</label><select><option>Grsai · 图像</option><option>VIP 自定义线路</option></select></div>
          <div class="field"><label>宽高比</label><select><option>9:16</option><option>4:3</option></select></div>
        </div>`);
      break;

    case 'copy':
      const content = decodeURIComponent(target.dataset.content || '');
      navigator.clipboard?.writeText(content);
      notify('提示词已复制');
      break;

    case 'toggle-account':
      const accId = target.dataset.id;
      try {
        await api(`/api/accounts/${accId}/toggle`, { method: 'POST' });
        renderAccounts();
      } catch (e) {
        notify('操作失败');
      }
      break;

    case 'select-main-image': {
      const shotId = target.dataset.shotId;
      const imageUrl = target.dataset.imageUrl;
      if (!shotId || !imageUrl) break;
      try {
        await api(`/api/shots/${shotId}/select-main-image?imageUrl=${encodeURIComponent(imageUrl)}`, { method: 'POST' });
        notify('已设为主图');
        renderStory();
      } catch (e) {
        notify('设置失败: ' + e.message);
      }
      break;
    }

    case 'infer-shot': {
      const shotId = target.dataset.shotId;
      notify('推理提示词功能待实现');
      break;
    }

    case 'generate-shot': {
      const shotId = target.dataset.shotId;
      notify('生成图片功能待实现');
      break;
    }

    case 'change-page-size': {
      const select = target.closest('.page-size-select');
      if (select) {
        storyPageSize = parseInt(select.value);
        storyPage = 1; // 重置到第一页
        renderStory();
      }
      break;
    }

    case 'first-page':
      if (storyPage > 1) {
        storyPage = 1;
        renderStory();
      }
      break;

    case 'prev-page':
      if (storyPage > 1) {
        storyPage--;
        renderStory();
      }
      break;

    case 'next-page': {
      const totalPages = storyPageSize > 0 ? Math.ceil(storyTotal / storyPageSize) : 1;
      if (storyPage < totalPages) {
        storyPage++;
        renderStory();
      }
      break;
    }

    case 'last-page': {
      const totalPages = storyPageSize > 0 ? Math.ceil(storyTotal / storyPageSize) : 1;
      if (storyPage < totalPages) {
        storyPage = totalPages;
        renderStory();
      }
      break;
    }

    case 'open-preset-panel':
      openPresetPanel();
      break;

    case 'close-preset-panel':
      closePresetPanel();
      break;

    case 'switch-preset-tab': {
      const tab = target.dataset.tab;
      if (!tab) break;
      // 高亮 tab 按钮
      document.querySelectorAll('.sp-tab').forEach(b => b.classList.toggle('active', b.dataset.tab === tab));
      // 滚动到对应 section
      const section = document.querySelector(`.sp-section[data-section="${tab}"]`);
      if (section) {
        const body = document.getElementById('preset-panel-body');
        if (body) body.scrollTo({ top: section.offsetTop - 10, behavior: 'smooth' });
      }
      break;
    }

    case 'preset-settings':
      openPresetPanel();
      break;

    case 'comic-settings':
      openComicSettings();
      break;

    case 'batch-infer':
      openBatchScopeModal('infer');
      break;

    case 'batch-generate':
      openBatchScopeModal('generate');
      break;

    case 'batch-scope': {
      const scope = target.dataset.scope;
      const type = target.dataset.type;
      modalRoot.innerHTML = '';
      try {
        const params = (scope === 'all' || storyPageSize <= 0)
          ? `novelId=${currentNovelId}&scope=all`
          : `novelId=${currentNovelId}&scope=page&page=${storyPage}&size=${storyPageSize}`;
        const endpoint = type === 'infer' ? '/api/shots/batch-infer' : '/api/shots/batch-generate';
        await api(`${endpoint}?${params}`, { method: 'POST' });
        notify(scope === 'all' ? `已提交全部 ${storyTotal} 个分镜` : '已提交当前页');
        renderStory();
      } catch (e) {
        notify('操作失败: ' + e.message);
      }
      break;
    }

    case 'save-preset': {
      try {
        const plotSummary = document.querySelector('#preset-plot-summary')?.value || '';
        const characters = collectPresetItems('character');
        const scenes = collectPresetItems('scene');
        const props = collectPresetItems('prop');
        await api('/api/presets', {
          method: 'POST',
          body: JSON.stringify({ novelId: currentNovelId, plotSummary, characters, scenes, props })
        });
        notify('预设已保存');
        closePresetPanel();
        renderStory();
      } catch (e) {
        notify('保存失败: ' + e.message);
      }
      break;
    }

    case 'save-comic-settings': {
      try {
        const body = {
          novelId: currentNovelId,
          textModelId: document.querySelector('#comic-text-model')?.value || null,
          imageModelId: document.querySelector('#comic-image-model')?.value || null,
          artStyleId: document.querySelector('#comic-art-style')?.value || null,
          prefixPrompt: document.querySelector('#comic-prefix')?.value || null,
          suffixPrompt: document.querySelector('#comic-suffix')?.value || null,
          aspectRatio: document.querySelector('#comic-aspect-ratio')?.value || '9:16'
        };
        await api('/api/comic-settings', { method: 'POST', body: JSON.stringify(body) });
        notify('漫画设置已保存');
        modalRoot.innerHTML = '';
      } catch (e) {
        notify('保存失败: ' + e.message);
      }
      break;
    }

    case 'smart-character':
    case 'smart-scene':
    case 'smart-prop': {
      const type = action.replace('smart-', '');
      if (!currentNovelId) { notify('请先选择小说'); return; }
      try {
        const typeName = type === 'character' ? '角色' : type === 'scene' ? '场景' : '道具';
        notify(`正在智能生成${typeName}...`);
        const result = await api(`/api/presets/smart?novelId=${currentNovelId}&type=${type}`, { method: 'POST' });
        const listId = type === 'character' ? 'preset-characters' : type === 'scene' ? 'preset-scenes' : 'preset-props';
        const listEl = document.querySelector(`#${listId}`);
        if (listEl && result.items) {
          listEl.innerHTML = result.items.length > 0
            ? result.items.map(item => renderPresetItem(item, type)).join('')
            : `<div class="sp-empty">暂无${typeName}</div>`;
          refreshAllPresetList();
        }
        notify(`已生成 ${result.items?.length || 0} 个${typeName}`);
      } catch (err) {
        notify('智能生成失败: ' + err.message);
      }
      break;
    }

    case 'preset-select-all': {
      const panel = document.getElementById('preset-panel-body');
      if (!panel) break;
      panel.querySelectorAll('.preset-check').forEach(b => { b.checked = true; });
      updatePresetSelectedCount();
      break;
    }

    case 'preset-batch-delete': {
      const panel = document.getElementById('preset-panel-body');
      if (!panel) break;
      const cards = panel.querySelectorAll('.preset-card');
      const toRemove = [];
      cards.forEach(c => { if (c.querySelector('.preset-check:checked')) toRemove.push(c); });
      if (toRemove.length === 0) { notify('请先勾选要删除的预设'); break; }
      toRemove.forEach(c => c.remove());
      refreshAllPresetList();
      updatePresetSelectedCount();
      notify(`已删除 ${toRemove.length} 个预设`);
      break;
    }

    case 'remove-preset-item': {
      const type = target.dataset.type;
      const name = target.dataset.name;
      const list = document.querySelector(`#preset-${type}s`);
      const allList = document.querySelector('#preset-all');
      [list, allList].forEach(l => {
        if (!l) return;
        l.querySelectorAll('.preset-card').forEach(el => {
          const nameInput = el.querySelector('.preset-card-name');
          if (nameInput && nameInput.value === name) el.remove();
        });
      });
      updatePresetSelectedCount();
      break;
    }

    case 'add-preset-item': {
      const type = target.dataset.type;
      const list = document.querySelector(`#preset-${type}s`);
      if (!list) break;
      // 打开弹窗新建预设，传入 null 表示新建模式
      openPresetEditModal(type, '', '', null, list);
      break;
    }

    case 'edit-preset-item': {
      const card = target.closest('.preset-card');
      if (!card) break;
      const type = card.dataset.type || target.dataset.type;
      // Read current values from the card
      const titleDiv = card.querySelector('.preset-card-title');
      const descDiv = card.querySelector('.preset-card-desc');
      const name = titleDiv?.textContent || target.dataset.name || '';
      const desc = descDiv?.textContent || target.dataset.desc || '';
      openPresetEditModal(type, name, desc, card);
      break;
    }

    case 'save-preset-edit': {
      const modal = modalRoot.querySelector('.preset-edit-modal');
      if (!modal) break;
      const cardEl = modal._editingCard;
      const targetList = modal._targetList;
      const presetType = modal._presetType || (cardEl ? cardEl.dataset.type : 'character');
      const oldName = modal.dataset.originalName || '';
      const newName = modal.querySelector('#preset-edit-name')?.value?.trim() || '';
      const rawDesc = modal.querySelector('#preset-edit-desc')?.value?.trim() || '';
      // 合并类型专属字段到描述中
      const newDesc = mergeTypeSpecificDesc(presetType, rawDesc, modal);
      if (!newName) { notify('请输入名称'); break; }

      if (cardEl) {
        // 编辑模式：更新现有卡片
        const type = cardEl.dataset.type;
        // Find all cards with the old name in both individual sections and "all" tab
        const allCards = document.querySelectorAll(`.preset-card[data-type="${type}"]`);
        allCards.forEach(card => {
          const titleDiv = card.querySelector('.preset-card-title');
          const descDiv = card.querySelector('.preset-card-desc');
          if (titleDiv && (titleDiv.textContent === oldName || card === cardEl)) {
            titleDiv.textContent = newName;
            if (descDiv) descDiv.textContent = newDesc || '暂无描述';
            // Update edit button data attributes
            const editBtn = card.querySelector('[data-action="edit-preset-item"]');
            if (editBtn) {
              editBtn.dataset.name = newName;
              editBtn.dataset.desc = newDesc;
            }
            // Update delete button data attributes
            const delBtn = card.querySelector('[data-action="remove-preset-item"]');
            if (delBtn) delBtn.dataset.name = newName;
          }
        });
      } else if (targetList) {
        // 新建模式：创建新卡片并插入列表
        const type = targetList.id.replace('preset-', '').replace(/s$/, ''); // 从 id 提取类型
        const newCard = document.createElement('div');
        newCard.className = 'preset-card';
        newCard.dataset.type = type;
        newCard.innerHTML = `<input type="checkbox" class="preset-check" onchange="updatePresetSelectedCount()" />
          <div class="preset-card-main">
            <div class="preset-card-title">${escapeHtml(newName)}</div>
            <div class="preset-card-desc">${escapeHtml(newDesc || '暂无描述')}</div>
          </div>
          <div class="preset-card-actions">
            <button class="preset-act-add">添加</button>
            <button class="preset-act-edit" data-action="edit-preset-item" data-type="${type}" data-name="${escapeHtml(newName)}" data-desc="${escapeHtml(newDesc)}">编辑</button>
            <button class="preset-act-del" data-action="remove-preset-item" data-type="${type}" data-name="${escapeHtml(newName)}">删除</button>
          </div>`;
        targetList.prepend(newCard);

        // 清除空状态提示
        const emptyEl = targetList.querySelector('.sp-empty');
        if (emptyEl) emptyEl.remove();
      }

      refreshAllPresetList();
      modalRoot.innerHTML = '';
      notify(cardEl ? '预设已保存' : '预设已添加');
      break;
    }

    case 'cancel-preset-edit':
      modalRoot.innerHTML = '';
      break;

    case 'generate-preset-image': {
      const modal = modalRoot.querySelector('.preset-edit-modal');
      if (!modal) break;
      const type = modal._presetType || 'character';
      const name = modal.querySelector('#preset-edit-name')?.value?.trim() || '';
      const desc = modal.querySelector('#preset-edit-desc')?.value?.trim() || '';
      const styleId = modal.querySelector('#preset-edit-style')?.value || '';
      const aspectRatio = modal.querySelector('#preset-edit-ratio')?.value || '9:16';
      const customPrompt = modal.querySelector('#preset-edit-prompt')?.value?.trim() || '';
      if (!name) { notify('请先输入名称'); break; }

      const preview = modal.querySelector('.preset-edit-image-preview');
      const btn = modal.querySelector('.preset-edit-generate-btn');
      if (!preview || !btn) break;

      // Show loading state
      btn.disabled = true;
      btn.textContent = '生成中...';

      // 移除旧的 loading / 图片
      preview.querySelectorAll('.preset-edit-loading, img').forEach(el => el.remove());
      const loadingEl = document.createElement('div');
      loadingEl.className = 'preset-edit-loading';
      loadingEl.innerHTML = '<div class="preset-edit-spinner"></div><div>正在生成图片...</div>';
      preview.appendChild(loadingEl);

      try {
        const prompt = buildPresetPrompt(type, name, desc, modal, customPrompt);
        const arts = await loadStyleList();
        if (styleId) {
          const style = arts.find(s => String(s.id) === String(styleId));
          if (style?.description) prompt.promptText += `，画风：${style.description}`;
        }

        notify(`图片生成功能待接入后端 AI 服务（${prompt.typeName} / ${aspectRatio}）`);
        loadingEl.remove();
        // 展示占位提示
        const empty = document.createElement('div');
        empty.className = 'preset-edit-image-empty';
        empty.textContent = '待接入 AI 服务，暂无法出图';
        preview.appendChild(empty);
      } catch (e) {
        loadingEl.remove();
        notify('生成失败: ' + e.message);
      } finally {
        btn.disabled = false;
        btn.textContent = '生成图片';
      }
      break;
    }

    case 'edit-shot': {
      const shotId = target.dataset.shotId;
      if (shotId) openEditShot(parseInt(shotId));
      break;
    }

    case 'delete-shot': {
      const shotId = target.dataset.shotId;
      if (shotId) deleteShot(parseInt(shotId));
      break;
    }

    case 'save-shot-edit': {
      const shotId = target.dataset.shotId;
      const originalText = document.querySelector('#shot-original-text')?.value || '';
      const sceneDescription = document.querySelector('#shot-scene-desc')?.value || '';
      const promptText = document.querySelector('#shot-prompt')?.value || '';
      const tagsRaw = document.querySelector('#shot-tags')?.value || '';
      const tags = tagsRaw.split(/[,，、]/).map(s => s.trim()).filter(Boolean);
      try {
        const shot = await api(`/api/shots/${shotId}`);
        await api(`/api/shots/${shotId}`, {
          method: 'PUT',
          body: JSON.stringify({
            novelId: shot.novelId,
            sequence: shot.sequence,
            originalText,
            sceneDescription,
            promptText,
            tags,
            state: shot.state
          })
        });
        notify('分镜已保存');
        modalRoot.innerHTML = '';
        renderStory();
      } catch (e) {
        notify('保存失败: ' + e.message);
      }
      break;
    }

    case 'one-click-character':
      notify('一键形象功能需要配置账号池，待实现');
      break;

    case 'batch-infer':
      if (currentNovelId) {
        try {
          await api(`/api/shots/batch-infer?novelId=${currentNovelId}`, { method: 'POST' });
          notify('已加入批量推理队列');
          renderStory();
        } catch (e) {
          notify('操作失败');
        }
      }
      break;

    case 'batch-generate':
      if (currentNovelId) {
        try {
          await api(`/api/shots/batch-generate?novelId=${currentNovelId}`, { method: 'POST' });
          notify('已加入批量生图队列');
          renderStory();
        } catch (e) {
          notify('操作失败');
        }
      }
      break;

    case 'batch':
    case 'image':
    case 'rewrite':
    case 'export':
    case 'select':
      notify({
        batch: '已加入批处理队列',
        image: '已提交图片推导任务',
        rewrite: '已生成改写结果',
        export: '操作已完成',
        select: '已进入选择模式'
      }[action] || '已执行');
      break;

    case 'open-novel':
      // 已在前置分支处理
      break;
  }

  // 关闭弹窗
  if (target.matches('[data-close]')) {
    modalRoot.innerHTML = '';
  }

  // 保存弹窗
  if (target.matches('[data-save]')) {
    await handleSave();
  }
});

// ========== 保存处理 ==========
async function handleSave() {
  try {
    if (currentView === 'novels' || document.querySelector('#novel-title')) {
      const title = document.querySelector('#novel-title')?.value;
      const content = document.querySelector('#novel-content')?.value;
      const source = document.querySelector('#novel-source')?.value || '';
      const vip = document.querySelector('#novel-vip')?.value === 'true';

      if (title) {
        await api('/api/novels', {
          method: 'POST',
          body: JSON.stringify({ title, content, source, vip, status: 'active' })
        });
        notify('小说已创建');
        modalRoot.innerHTML = '';
        loadNovelList();
        renderNovels();
        return;
      }
    }

    if (document.querySelector('#prompt-title')) {
      const title = document.querySelector('#prompt-title').value;
      const category = document.querySelector('#prompt-category')?.value || '其他';
      const content = document.querySelector('#prompt-content')?.value || '';

      if (title) {
        await api('/api/prompts', {
          method: 'POST',
          body: JSON.stringify({ title, category, content })
        });
        notify('提示词已创建');
        modalRoot.innerHTML = '';
        renderPrompts();
        return;
      }
    }

    if (document.querySelector('#style-name')) {
      const name = document.querySelector('#style-name').value;
      const description = document.querySelector('#style-desc')?.value || '';

      if (name) {
        await api('/api/styles', {
          method: 'POST',
          body: JSON.stringify({ name, description, imageUrl: '' })
        });
        notify('画风已创建');
        modalRoot.innerHTML = '';
        renderStyles();
        return;
      }
    }

    if (document.querySelector('#asset-name')) {
      const name = document.querySelector('#asset-name').value;
      const type = document.querySelector('#asset-type')?.value || 'CHARACTER';
      const style = document.querySelector('#asset-style')?.value || 'ANCIENT';
      const description = document.querySelector('#asset-desc')?.value || '';

      if (name) {
        await api('/api/assets', {
          method: 'POST',
          body: JSON.stringify({ name, type, style, description, imageUrl: '', novelId: currentNovelId })
        });
        notify('资产已创建');
        modalRoot.innerHTML = '';
        renderAssets();
        return;
      }
    }

    if (document.querySelector('#acc-display')) {
      const displayName = document.querySelector('#acc-display').value;
      const protocol = document.querySelector('#acc-protocol')?.value || 'openai';
      const apiUrl = document.querySelector('#acc-url')?.value || '';
      const apiKey = document.querySelector('#acc-key')?.value || '';
      const modelId = document.querySelector('#acc-model')?.value || '';

      if (displayName) {
        await api('/api/accounts', {
          method: 'POST',
          body: JSON.stringify({
            name: displayName,
            protocol,
            apiUrl,
            apiKey,
            modelId,
            displayName,
            capabilities: ['TEXT'],
            enabled: true,
            vip: false
          })
        });
        notify('账号已创建');
        modalRoot.innerHTML = '';
        renderAccounts();
        return;
      }
    }

    modalRoot.innerHTML = '';
    notify('已保存');
  } catch (e) {
    notify('保存失败: ' + e.message);
  }
}

// ========== 编辑分镜弹窗 ==========
async function openEditShot(shotId) {
  try {
    const shot = await api(`/api/shots/${shotId}`);
    modalRoot.innerHTML = `
      <div class="modal-back">
        <div class="modal shot-edit-modal">
          <div class="shot-edit-header">
            <h3>编辑分镜 <small style="color:#8899aa">第 ${shot.sequence} 镜</small></h3>
            <button class="side-panel-close" data-close>✕</button>
          </div>
          <div class="shot-edit-body">
            <div class="sp-block">
              <div class="sp-block-title">画面内容</div>
              <textarea id="shot-original-text" class="sp-plot" placeholder="分镜对应的小说原文…">${escapeHtml(shot.originalText || '')}</textarea>
            </div>
            <div class="sp-block">
              <div class="sp-block-title">场景描述</div>
              <textarea id="shot-scene-desc" class="sp-plot" placeholder="场景构图、镜头…">${escapeHtml(shot.sceneDescription || '')}</textarea>
            </div>
            <div class="sp-block">
              <div class="sp-block-title">图片描述词</div>
              <textarea id="shot-prompt" class="sp-plot" placeholder="AI 生图提示词…">${escapeHtml(shot.promptText || '')}</textarea>
            </div>
            <div class="sp-block">
              <div class="sp-block-title">标签（逗号分隔）</div>
              <input id="shot-tags" class="sp-tag-input" value="${(shot.tags || []).join(', ')}" placeholder="角色, 场景, 道具" />
            </div>
          </div>
          <div class="side-panel-footer">
            <button class="btn" data-close>取消</button>
            <button class="btn gold" data-action="save-shot-edit" data-shot-id="${shot.id}">保存</button>
          </div>
        </div>
      </div>`;
  } catch (e) {
    notify('加载分镜失败: ' + e.message);
  }
}

// ========== 删除分镜 ==========
async function deleteShot(shotId) {
  if (!confirm(`确认删除分镜 #${shotId}？\n此操作不可恢复。`)) return;
  try {
    await api(`/api/shots/${shotId}`, { method: 'DELETE' });
    notify('分镜已删除');
    renderStory();
  } catch (e) {
    notify('删除失败: ' + e.message);
  }
}

// ========== 预设设置 - 左侧滑入面板 ==========
async function openPresetPanel() {
  if (!currentNovelId) { notify('请先选择小说'); return; }
  let preset = {};
  try { preset = await api(`/api/presets?novelId=${currentNovelId}`); } catch (e) {}

  const chars = (preset.characters || []).map(c => renderPresetItem(c, 'character')).join('');
  const scenes = (preset.scenes || []).map(c => renderPresetItem(c, 'scene')).join('');
  const props = (preset.props || []).map(c => renderPresetItem(c, 'prop')).join('');
  const allItems = chars + scenes + props;

  let panel = document.getElementById('preset-panel');
  if (!panel) {
    panel = document.createElement('div');
    panel.id = 'preset-panel';
    document.body.appendChild(panel);
  }
  panel.className = 'side-panel side-panel--left side-panel--open';
  panel.innerHTML = `
    <div class="side-panel-header">
      <h3>⚙ 预设设置</h3>
      <button class="side-panel-close" data-action="close-preset-panel">✕</button>
    </div>
    <div class="sp-toolbar">
      <span class="sp-selected">已选择 <b id="preset-selected-count">0</b> 个标签</span>
      <button class="btn-sm" data-action="preset-select-all">全部选择</button>
      <button class="btn-sm danger" data-action="preset-batch-delete">批量删除</button>
    </div>
    <div class="side-panel-body" id="preset-panel-body">
      <div class="sp-block">
        <div class="sp-block-title">剧情梗概</div>
        <textarea id="preset-plot-summary" class="sp-plot" placeholder="**剧情梗概：** 概括本书主线与人物关系，供后续分镜推理参考…">${escapeHtml(preset.plotSummary || '')}</textarea>
      </div>
      <div class="sp-block">
        <div class="sp-block-head">
          <div class="sp-block-title">当前预设</div>
          <div class="sp-tabs">
            <button class="sp-tab active" data-tab="all" data-action="switch-preset-tab">全部</button>
            <button class="sp-tab" data-tab="character" data-action="switch-preset-tab">角色</button>
            <button class="sp-tab" data-tab="scene" data-action="switch-preset-tab">场景</button>
            <button class="sp-tab" data-tab="prop" data-action="switch-preset-tab">道具</button>
          </div>
        </div>
        <div class="sp-section" data-section="all">
          <div class="preset-list" id="preset-all">${allItems || '<div class="sp-empty">暂无预设，切换到各分类添加或智能生成</div>'}</div>
        </div>
        <div class="sp-section" data-section="character">
          <div class="sp-section-title">角色</div>
          <div class="preset-list" id="preset-characters">${chars || '<div class="sp-empty">暂无角色</div>'}</div>
        </div>
        <div class="sp-section" data-section="scene">
          <div class="sp-section-title">场景</div>
          <div class="preset-list" id="preset-scenes">${scenes || '<div class="sp-empty">暂无场景</div>'}</div>
        </div>
        <div class="sp-section" data-section="prop">
          <div class="sp-section-title">道具</div>
          <div class="preset-list" id="preset-props">${props || '<div class="sp-empty">暂无道具</div>'}</div>
        </div>
      </div>
    </div>
    <div class="side-panel-footer sp-footer-btns">
      <button class="btn-sm teal" data-action="add-preset-item" data-type="character">＋ 角色</button>
      <button class="btn-sm teal" data-action="add-preset-item" data-type="scene">＋ 场景</button>
      <button class="btn-sm teal" data-action="add-preset-item" data-type="prop">＋ 道具</button>
      <button class="btn-sm" data-action="smart-character">智能角色</button>
      <button class="btn-sm" data-action="smart-scene">智能场景</button>
      <button class="btn-sm" data-action="smart-prop">智能道具</button>
      <span class="sp-footer-spacer"></span>
      <button class="btn" data-action="close-preset-panel">取消</button>
      <button class="btn gold" data-action="save-preset">保存预设</button>
    </div>`;
  updatePresetSelectedCount();
}

function closePresetPanel() {
  const panel = document.getElementById('preset-panel');
  if (panel) {
    panel.className = 'side-panel side-panel--left';
    setTimeout(() => { panel.innerHTML = ''; }, 300);
  }
}

// ========== 预设编辑弹窗 ==========
// cardEl: 编辑时传入被编辑的卡片，新建时传入 null
// targetList: 新建时传入目标列表容器，用于插入新卡片
async function openPresetEditModal(type, name, desc, cardEl, targetList = null) {
  const label = type === 'character' ? '角色' : type === 'scene' ? '场景' : '道具';
  const isNew = !cardEl;

  // 拆出描述中的类型专属字段，避免编辑时重复叠加
  const parsed = splitTypeSpecificDesc(type, desc);
  const baseDesc = parsed.base;

  // 根据类型生成不同的专属字段
  const typeFields = {
    character: `
      <div class="preset-edit-field">
        <label>外貌</label>
        <input id="preset-edit-appearance" type="text" placeholder="如：黑发长发、红瞳、高个子" />
      </div>
      <div class="preset-edit-field">
        <label>服饰</label>
        <input id="preset-edit-clothing" type="text" placeholder="如：古风长裙、现代休闲装" />
      </div>`,
    scene: `
      <div class="preset-edit-field">
        <label>时段</label>
        <select id="preset-edit-time">
          <option value="">不限</option>
          <option value="morning">清晨</option>
          <option value="noon">正午</option>
          <option value="afternoon">午后</option>
          <option value="evening">傍晚</option>
          <option value="night">夜晚</option>
        </select>
      </div>
      <div class="preset-edit-field">
        <label>氛围</label>
        <input id="preset-edit-mood" type="text" placeholder="如：温馨、压抑、热闹" />
      </div>`,
    prop: `
      <div class="preset-edit-field">
        <label>材质</label>
        <input id="preset-edit-material" type="text" placeholder="如：金属、木质、玉石" />
      </div>
      <div class="preset-edit-field">
        <label>年代风格</label>
        <select id="preset-edit-era">
          <option value="">不限</option>
          <option value="ancient">古代</option>
          <option value="medieval">中世纪</option>
          <option value="modern">现代</option>
          <option value="future">未来</option>
        </select>
      </div>`
  };

  // Load styles for dropdown
  let styles = [];
  try { styles = await api('/api/styles'); } catch (e) {}

  const styleOptions = styles.map(s =>
    `<option value="${s.id}">${escapeHtml(s.name)}</option>`
  ).join('');

  modalRoot.innerHTML = `
    <div class="modal-back preset-edit-back">
      <div class="modal preset-edit-modal">
        <div class="preset-edit-header">
          <h3>${isNew ? '添加' : '编辑'}${label}<small>${isNew ? '' : escapeHtml(name)}</small></h3>
          <button class="side-panel-close" data-action="cancel-preset-edit">×</button>
        </div>
        <div class="preset-edit-body">
          <div class="preset-edit-left">
            <div class="preset-edit-field">
              <label>${label}名称</label>
              <input id="preset-edit-name" type="text" value="${escapeHtml(name)}" placeholder="请输入${label}名称" />
            </div>
            <div class="preset-edit-field">
              <label>描述 / 出处</label>
              <textarea id="preset-edit-desc" placeholder="描述该${label}的特征、背景或出处...">${escapeHtml(baseDesc)}</textarea>
            </div>
            ${typeFields[type] || ''}
            <div class="preset-edit-field">
              <label>画风库</label>
              <select id="preset-edit-style">
                <option value="">默认画风</option>
                ${styleOptions}
              </select>
            </div>
            <div class="preset-edit-field">
              <label>宽高比</label>
              <select id="preset-edit-ratio">
                <option value="9:16">9:16 竖版</option>
                <option value="4:3">4:3 横版</option>
                <option value="1:1">1:1 方形</option>
              </select>
            </div>
            <div class="preset-edit-field">
              <label>自定义提示词 <small style="color:#666">(可选，覆盖默认)</small></label>
              <textarea id="preset-edit-prompt" placeholder="输入自定义图片生成提示词..."></textarea>
            </div>
          </div>
          <div class="preset-edit-right">
            <div class="preset-edit-image-preview">
              <div class="preset-edit-image-empty">
                点击「生成图片」预览效果
              </div>
            </div>
            <button class="preset-edit-generate-btn" data-action="generate-preset-image">生成图片</button>
          </div>
        </div>
        <div class="preset-edit-footer">
          <button class="btn" data-action="cancel-preset-edit">取消</button>
          <button class="btn gold" data-action="save-preset-edit">保存</button>
        </div>
      </div>
    </div>`;

  // Store reference to the card being edited and the original name
  const modal = modalRoot.querySelector('.preset-edit-modal');
  if (modal) {
    modal._editingCard = cardEl;
    modal._targetList = targetList;
    modal._presetType = type;
    modal.dataset.originalName = name;

    // 回填类型专属字段（编辑模式）
    (TYPE_FIELD_LABELS[type] || []).forEach(def => {
      const val = parsed.values[def.label];
      if (!val) return;
      const el = modal.querySelector(`#${def.id}`);
      if (!el) return;
      if (def.isSelect) {
        // 反查 select 的 value（如 时段：清晨 → morning）
        const map = type === 'scene' ? TIME_LABELS : ERA_LABELS;
        const key = Object.keys(map).find(k => map[k] === val);
        el.value = key || '';
      } else {
        el.value = val;
      }
    });
  }

  // 新建模式下聚焦到名称输入框
  if (isNew) {
    setTimeout(() => {
      modal?.querySelector('#preset-edit-name')?.focus();
    }, 100);
  }
}

// 类型专属字段的标签定义（用于描述合并/拆分）
const TYPE_FIELD_LABELS = {
  character: [{ id: 'preset-edit-appearance', label: '外貌' }, { id: 'preset-edit-clothing', label: '服饰' }],
  scene: [{ id: 'preset-edit-time', label: '时段', isSelect: true }, { id: 'preset-edit-mood', label: '氛围' }],
  prop: [{ id: 'preset-edit-material', label: '材质' }, { id: 'preset-edit-era', label: '年代风格', isSelect: true }]
};

const TIME_LABELS = { morning: '清晨', noon: '正午', afternoon: '午后', evening: '傍晚', night: '夜晚' };
const ERA_LABELS = { ancient: '古代', medieval: '中世纪', modern: '现代', future: '未来' };

/** 将类型专属字段合并进描述文本，格式：基础描述 | 外貌：xxx | 服饰：xxx */
function mergeTypeSpecificDesc(type, baseDesc, modal) {
  const defs = TYPE_FIELD_LABELS[type] || [];
  const parts = [];
  if (baseDesc) parts.push(baseDesc);
  defs.forEach(def => {
    const el = modal.querySelector(`#${def.id}`);
    if (!el) return;
    let val = (el.value || '').trim();
    if (def.isSelect) {
      if (type === 'scene') val = TIME_LABELS[val] || '';
      else if (type === 'prop') val = ERA_LABELS[val] || '';
    }
    if (val) parts.push(`${def.label}：${val}`);
  });
  return parts.join(' | ');
}

/** 从描述文本中拆出类型专属字段，返回 { base, values: { label: value } } */
function splitTypeSpecificDesc(type, desc) {
  const defs = TYPE_FIELD_LABELS[type] || [];
  const result = { base: desc || '', values: {} };
  if (!desc) return result;
  const segs = desc.split('|').map(s => s.trim());
  const baseParts = [];
  segs.forEach(seg => {
    const m = seg.match(/^([^：]+)：(.*)$/);
    if (m) {
      const label = m[1].trim();
      const val = m[2].trim();
      const matched = defs.find(d => d.label === label);
      if (matched) {
        result.values[label] = val;
        return;
      }
    }
    if (seg) baseParts.push(seg);
  });
  result.base = baseParts.join(' | ');
  return result;
}

let _styleCache = null;
async function loadStyleList() {
  if (_styleCache) return _styleCache;
  try { _styleCache = await api('/api/styles'); } catch (e) { _styleCache = []; }
  return _styleCache;
}

/**
 * 根据预设类型构建图片生成提示词
 * 不同类型的专属字段会拼接成行为主体描述
 */
function buildPresetPrompt(type, name, desc, modal, customPrompt) {
  if (customPrompt) {
    return { typeName: type === 'character' ? '角色' : type === 'scene' ? '场景' : '道具', promptText: customPrompt };
  }

  let subject;
  let details = [];
  let negative = '';

  if (type === 'character') {
    const appearance = modal.querySelector('#preset-edit-appearance')?.value?.trim() || '';
    const clothing = modal.querySelector('#preset-edit-clothing')?.value?.trim() || '';
    subject = `角色形象：${name}`;
    if (desc) details.push(desc);
    if (appearance) details.push(`外貌：${appearance}`);
    if (clothing) details.push(`服饰：${clothing}`);
    negative = '多余手指，扭曲肢体，低分辨率，模糊';
  } else if (type === 'scene') {
    const time = modal.querySelector('#preset-edit-time')?.value || '';
    const mood = modal.querySelector('#preset-edit-mood')?.value?.trim() || '';
    const timeMap = { morning: '清晨', noon: '正午', afternoon: '午后', evening: '傍晚', night: '夜晚' };
    subject = `场景环境：${name}`;
    if (desc) details.push(desc);
    if (timeMap[time]) details.push(`时段：${timeMap[time]}`);
    if (mood) details.push(`氛围：${mood}`);
    negative = '人物，文字，水印，低分辨率，模糊';
  } else {
    const material = modal.querySelector('#preset-edit-material')?.value?.trim() || '';
    const era = modal.querySelector('#preset-edit-era')?.value || '';
    const eraMap = { ancient: '古代', medieval: '中世纪', modern: '现代', future: '未来' };
    subject = `道具特写：${name}`;
    if (desc) details.push(desc);
    if (material) details.push(`材质：${material}`);
    if (eraMap[era]) details.push(`年代风格：${eraMap[era]}`);
    negative = '人物，文字，水印，低分辨率，模糊';
  }

  const promptText = details.length > 0 ? `${subject}。${details.join('。')}` : subject;
  return {
    typeName: type === 'character' ? '角色' : type === 'scene' ? '场景' : '道具',
    promptText,
    negativePrompt: negative
  };
}

function renderPresetItem(item, type) {
  const label = type === 'character' ? '角色' : type === 'scene' ? '场景' : '道具';
  return `<div class="preset-card" data-type="${type}">
    <input type="checkbox" class="preset-check" onchange="updatePresetSelectedCount()" />
    <div class="preset-card-main">
      <div class="preset-card-title">${escapeHtml(item.name || '未命名')}</div>
      <div class="preset-card-desc">${escapeHtml(item.description || '暂无描述')}</div>
    </div>
    <div class="preset-card-actions">
      <button class="preset-act-add">添加</button>
      <button class="preset-act-edit" data-action="edit-preset-item" data-type="${type}" data-name="${escapeHtml(item.name || '')}" data-desc="${escapeHtml(item.description || '')}">编辑</button>
      <button class="preset-act-del" data-action="remove-preset-item" data-type="${type}" data-name="${escapeHtml(item.name || '')}">删除</button>
    </div>
  </div>`;
}

function updatePresetSelectedCount() {
  const el = document.getElementById('preset-selected-count');
  if (!el) return;
  const checked = document.querySelectorAll('#preset-panel-body .preset-check:checked').length;
  el.textContent = checked;
}

// ========== 漫画设置弹窗 ==========
async function openComicSettings() {
  if (!currentNovelId) { notify('请先选择小说'); return; }
  let setting = {};
  try { setting = await api(`/api/comic-settings?novelId=${currentNovelId}`); } catch (e) {}

  let accounts = [];
  try { accounts = await api('/api/accounts'); } catch (e) {}
  let styles = [];
  try { styles = await api('/api/styles'); } catch (e) {}
  let prompts = [];
  try { prompts = await api('/api/prompts'); } catch (e) {}

  const textAccounts = accounts.filter(a => (a.capabilities || []).includes('TEXT'));
  const imageAccounts = accounts.filter(a => (a.capabilities || []).includes('IMAGE'));

  modalRoot.innerHTML = `
    <div class="modal-back">
      <div class="modal comic-settings-modal">
        <h2>漫画设置</h2>
        <div class="form-grid">
          <div class="field full"><label>图片推导词</label>
            <select id="comic-prompt-template">
              <option value="">-- 选择推导词模板 --</option>
              ${prompts.map(p => `<option value="${p.id}" ${setting.promptTemplateId == p.id ? 'selected' : ''}>${escapeHtml(p.title || '')}</option>`).join('')}
            </select>
          </div>
          <div class="field"><label>文本推理模型</label>
            <select id="comic-text-model">
              <option value="">-- 选择文本模型 --</option>
              ${textAccounts.map(a => `<option value="${a.id}" ${setting.textModelId == a.id ? 'selected' : ''}>${escapeHtml(a.displayName || a.name || '')}</option>`).join('')}
            </select>
          </div>
          <div class="field"><label>图片推理模型</label>
            <select id="comic-image-model">
              <option value="">-- 选择图片模型 --</option>
              ${imageAccounts.map(a => `<option value="${a.id}" ${setting.imageModelId == a.id ? 'selected' : ''}>${escapeHtml(a.displayName || a.name || '')}</option>`).join('')}
            </select>
          </div>
          <div class="field"><label>默认画风</label>
            <select id="comic-art-style">
              <option value="">-- 选择画风 --</option>
              ${styles.map(s => `<option value="${s.id}" ${setting.artStyleId == s.id ? 'selected' : ''}>${escapeHtml(s.name || '')}</option>`).join('')}
            </select>
          </div>
          <div class="field"><label>宽高比</label>
            <select id="comic-aspect-ratio">
              <option value="9:16" ${setting.aspectRatio === '9:16' ? 'selected' : ''}>9:16</option>
              <option value="4:3" ${setting.aspectRatio === '4:3' ? 'selected' : ''}>4:3</option>
              <option value="1:1" ${setting.aspectRatio === '1:1' ? 'selected' : ''}>1:1</option>
            </select>
          </div>
          <div class="field full"><label>前缀提示词</label>
            <select id="comic-prefix">
              <option value="">-- 无前缀 --</option>
              ${prompts.filter(p => p.category === '前后缀').map(p => `<option value="${p.id}" ${setting.prefixPrompt == p.id ? 'selected' : ''}>${escapeHtml(p.title || '')}</option>`).join('')}
            </select>
          </div>
          <div class="field full"><label>后缀提示词</label>
            <select id="comic-suffix">
              <option value="">-- 无后缀 --</option>
              ${prompts.filter(p => p.category === '前后缀').map(p => `<option value="${p.id}" ${setting.suffixPrompt == p.id ? 'selected' : ''}>${escapeHtml(p.title || '')}</option>`).join('')}
            </select>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn" data-close>取消</button>
          <button class="btn gold" data-action="save-comic-settings">保存设置</button>
        </div>
      </div>
    </div>`;
}

// ========== 批量操作范围选择弹窗 ==========
function openBatchScopeModal(actionType) {
  if (!currentNovelId) { notify('请先选择小说'); return; }
  modalRoot.innerHTML = `
    <div class="modal-back">
      <div class="modal" style="width:min(400px,90vw)">
        <h2>${actionType === 'infer' ? '批量推理提示词' : '批量生图'}</h2>
        <p style="color:#aaa;margin:10px 0 20px">选择操作范围：</p>
        <div style="display:flex;flex-direction:column;gap:12px">
          <button class="btn teal" style="width:100%;padding:14px" data-action="batch-scope" data-scope="page" data-type="${actionType}">
            <strong>当前页</strong><br/><small style="color:#aaa">仅处理当前页 ${storyPageSize > 0 ? storyPageSize : ''} 个分镜</small>
          </button>
          <button class="btn gold" style="width:100%;padding:14px" data-action="batch-scope" data-scope="all" data-type="${actionType}">
            <strong>全部镜头</strong><br/><small style="color:#aaa">处理全部 ${storyTotal} 个分镜</small>
          </button>
        </div>
        <div class="modal-foot">
          <button class="btn" data-close>取消</button>
        </div>
      </div>
    </div>`;
}

// ========== 初始化 ==========
function collectPresetItems(type) {
  const list = document.querySelector(`#preset-${type}s`);
  if (!list) return [];
  const items = [];
  list.querySelectorAll('.preset-card').forEach(el => {
    // 编辑态：input 元素
    const editNameInput = el.querySelector('.preset-edit-name');
    const editDescInput = el.querySelector('.preset-edit-desc');
    // 展示态：div 元素
    const titleDiv = el.querySelector('.preset-card-title');
    const descDiv = el.querySelector('.preset-card-desc');

    const name = editNameInput?.value || titleDiv?.textContent || '';
    const desc = editDescInput?.value || descDiv?.textContent || '';
    if (name.trim()) items.push({ name: name.trim(), description: desc.trim(), type: type.toUpperCase(), imageUrl: '' });
  });
  return items;
}

function refreshAllPresetList() {
  const allEl = document.querySelector('#preset-all');
  if (!allEl) return;
  const chars = document.querySelectorAll('#preset-characters .preset-card');
  const scenes = document.querySelectorAll('#preset-scenes .preset-card');
  const props = document.querySelectorAll('#preset-props .preset-card');
  let html = '';
  chars.forEach(el => html += el.outerHTML);
  scenes.forEach(el => html += el.outerHTML);
  props.forEach(el => html += el.outerHTML);
  allEl.innerHTML = html || '<div class="sp-empty">暂无预设</div>';
}

loadNovelList();
show('novels');
