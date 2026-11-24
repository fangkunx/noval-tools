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
    view.innerHTML = shell('小说分镜台', '请先选择一个小说');
    return;
  }

  let shots = [];
  try { shots = await api(`/api/shots?novelId=${currentNovelId}`); } catch (e) {}

  view.innerHTML = `
    <div>
      ${shell('小说分镜台', `当前小说 ID: ${currentNovelId} · 将每行文本推理为漫画画面。`,
        `<button class="btn gold" data-action="batch-infer">批量推理</button>
         <button class="btn teal" data-action="batch-generate">批量生图</button>
         <button class="btn" data-action="comic-settings">漫画设置</button>
         <button class="btn teal" data-action="export">导出漫画</button>`)}
      <div class="toolbar">
        <button class="chip active">序号 跳转</button>
        <span>画面内容</span>
        <button class="chip active">排序</button>
        <button class="chip">连续</button>
        <span>预设</span>
        <button class="chip">推理</button>
        <button class="chip active" data-action="preset">设置</button>
        <span>图片描述词</span>
        <button class="chip active">操作</button>
        <button class="chip">缺失</button>
      </div>
      <div class="split-layout">
        ${shots.map(s => `
          <div class="shot" data-shot-id="${s.id}">
            <div class="shot-num">${s.sequence}</div>
            <div class="shot-text">${s.originalText || ''}</div>
            <div class="tags">
              ${(s.tags || []).map(t => `<span class="tag teal">${t}</span>`).join('')}
            </div>
            <button class="btn teal" style="margin-top:18px" data-action="rewrite">改写</button>
          </div>
          <div class="shot">
            <div class="shot-desc">${s.sceneDescription || '等待推理...'}</div>
            <div class="tags">
              <span class="tag teal">画面构图</span>
              <span class="tag">镜头</span>
            </div>
          </div>
          <div class="shot">
            <div class="shot-images">
              ${(s.imageUrls || []).map(url => `<div class="thumb"><img src="${url}" style="width:100%;height:100%;object-fit:cover"/></div>`).join('')}
              <div class="empty-thumb">拖拽导入<br/><strong>＋</strong></div>
            </div>
            <div class="bottom-bar">
              <button class="btn" data-action="image">智能推导</button>
              <button class="btn teal" data-action="image">推导图片</button>
            </div>
          </div>
        `).join('')}
      </div>
      <div class="bottom-bar">
        <span style="margin-right:auto;color:#aaa">分镜数 <b style="color:var(--teal)">${shots.length}</b></span>
        <button class="btn">＋ 导入剧本(txt)</button>
        <button class="btn">＋ 添加新行</button>
        <button class="btn">上一页</button>
        <button class="btn teal">下一页</button>
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

// ========== 事件处理 ==========
document.addEventListener('click', async (e) => {
  const target = e.target.closest('[data-view], [data-action], [data-novel-id], [data-filter-type], [data-close], [data-save]');
  if (!target) return;

  const viewName = target.dataset.view;
  const action = target.dataset.action;
  const novelId = target.dataset.novelId;
  const filterType = target.dataset.filterType;

  // 导航切换
  if (viewName) {
    show(viewName);
    return;
  }

  // 动作处理（优先于 novelId，因为小说卡片同时有 data-action 和 data-novel-id）
  if (action === 'open-novel') {
    currentView = 'story';
    show('story');
    return;
  }

  // 小说选择（侧边栏剧本）
  if (novelId) {
    currentNovelId = parseInt(novelId);
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

// ========== 初始化 ==========
loadNovelList();
show('novels');
