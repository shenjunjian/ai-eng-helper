/* ============================================================
   TalkFlow · 移动端交互原型
   结构：会话（首页/微信式列表） · 单词本 · 我的
   页签栏固定渲染在手机壳（应用窗口）底部
   ============================================================ */

const MIN = 60000, HOUR = 3600000, DAY = 86400000;
const NOW = Date.now();

/* ---------- 图标 ---------- */
const svg = (p, w = 1.7) => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="${w}" stroke-linecap="round" stroke-linejoin="round">${p}</svg>`;
const I = {
  chats: svg('<path d="M20 11.6a7.6 7.6 0 0 1-11.2 6.7L4 20l1.7-4.6A7.6 7.6 0 1 1 20 11.6z"/>'),
  book: svg('<path d="M12 6.6C10.4 5.2 8 4.6 5 5.2v13.4c3-.6 5.4 0 7 1.4 1.6-1.4 4-2 7-1.4V5.2c-3-.6-5.4 0-7 1.4z"/><path d="M12 6.6v13.4"/>'),
  user: svg('<circle cx="12" cy="8.2" r="3.6"/><path d="M5.2 20c1.2-3.6 3.9-5.2 6.8-5.2s5.6 1.6 6.8 5.2"/>'),
  mic: svg('<rect x="9.2" y="3.2" width="5.6" height="9.6" rx="2.8"/><path d="M5.6 11.4a6.4 6.4 0 0 0 12.8 0M12 17.8v2.8"/>'),
  chev: svg('<path d="M9.4 5.6l6.4 6.4-6.4 6.4"/>', 2),
  back: svg('<path d="M14.6 5.6l-6.4 6.4 6.4 6.4"/>', 2),
  search: svg('<circle cx="11" cy="11" r="6.2"/><path d="M15.6 15.6L20 20"/>'),
  captions: svg('<rect x="3" y="5.6" width="18" height="12.8" rx="3.4"/><path d="M8.2 11.6a2.4 2.4 0 1 0 0 2.8M15 11.6a2.4 2.4 0 1 0 0 2.8"/>'),
  moon: svg('<path d="M20 14.6A8.4 8.4 0 1 1 9.6 4.2a7 7 0 0 0 10.4 10.4z"/>'),
  mask: svg('<path d="M4 12a8 8 0 0 1 16 0v3.4a4.6 4.6 0 0 1-4.6 4.6h-6.8A4.6 4.6 0 0 1 4 15.4z"/><path d="M8.6 13.6h1.8M13.6 13.6h1.8"/>'),
  x: svg('<path d="M6.4 6.4l11.2 11.2M17.6 6.4L6.4 17.6"/>', 2),
  chart: svg('<path d="M4.6 19.4V11M10 19.4V5.6M15.4 19.4v-6M20.8 19.4V8.6"/>', 2),
  bell: svg('<path d="M6.6 10.4a5.4 5.4 0 1 1 10.8 0c0 4 1.4 5.4 1.4 5.4H5.2s1.4-1.4 1.4-5.4z"/><path d="M10.2 18.6a1.9 1.9 0 0 0 3.6 0"/>'),
  check: svg('<path d="M5 12.6l4.6 4.6L19 7.4"/>', 2.2),
  spark: svg('<path d="M12 4.2l1.9 5.2 5.2 1.9-5.2 1.9L12 18.4l-1.9-5.2-5.2-1.9 5.2-1.9z"/>'),
};
const hangupIcon = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" style="transform:rotate(133deg)"><path d="M6.6 4.4h3.1l1.5 3.8-2 1.4a11 11 0 0 0 5.2 5.2l1.4-2 3.8 1.5v3.1c0 1-.8 1.7-1.8 1.6C10.4 18.4 5.6 13.6 5 6.2c-.1-1 .6-1.8 1.6-1.8z"/></svg>`;

/* ---------- 数据 ---------- */
const TEACHERS = [
  { id: 'emma', name: 'Emma', glyph: '👩🏻‍🏫', style: '教学型', desc: '耐心细致 · 美音', c: ['#FF9A68', '#FF6B4A'] },
  { id: 'leo', name: 'Leo', glyph: '🧑🏽‍🎓', style: '纠正型', desc: '逐句纠错 · 英音', c: ['#8AA7FF', '#4F63F2'] },
  { id: 'mia', name: 'Mia', glyph: '👩🏼‍🦰', style: '温柔型', desc: '鼓励为主 · 慢速', c: ['#FFB3D1', '#F2649B'] },
  { id: 'kai', name: 'Kai', glyph: '🧔🏻', style: '闲聊型', desc: '话题广 · 语速快', c: ['#7DE2C0', '#18A97F'] },
];

const state = {
  user: { name: '林小雨', age: 16, level: '中级', goals: ['考试提分'], balance: 25.00, teacher: 'emma' },
  sessions: [
    { id: 's1', title: '餐厅点餐：汉堡与饮品', t: 'emma', ts: NOW - 2 * HOUR, preview: 'Would you like anything to drink?', mode: '教学型', words: 4, mins: 12 },
    { id: 's2', title: '周末出行计划', t: 'kai', ts: NOW - 1 * DAY - 3 * HOUR, preview: 'So where are you headed this weekend?', mode: '闲聊型', words: 6, mins: 18 },
    { id: 's3', title: '机场值机与安检', t: 'leo', ts: NOW - 2 * DAY, preview: 'Please place your bag on the scale.', mode: '纠正型', words: 9, mins: 22 },
    { id: 's4', title: '电影偏好闲聊', t: 'mia', ts: NOW - 4 * DAY, preview: 'I am totally into sci-fi lately.', mode: '温柔型', words: 3, mins: 9 },
    { id: 's5', title: '英文自我介绍 · 面试', t: 'emma', ts: NOW - 9 * DAY, preview: 'Tell me a little about yourself.', mode: '教学型', words: 7, mins: 26 },
  ],
  words: [
    { id: 'w1', en: 'board something', zh: '登机', type: 'phrase', src: 's3', ts: NOW - 2 * DAY, done: false },
    { id: 'w2', en: 'overweight', zh: '超重的', type: 'word', src: 's3', ts: NOW - 2 * DAY + 8 * MIN, done: false },
    { id: 'w3', en: 'carry-on', zh: '随身行李', type: 'word', src: 's3', ts: NOW - 2 * DAY + 15 * MIN, done: false },
    { id: 'w4', en: 'sci-fi', zh: '科幻（题材）', type: 'word', src: 's4', ts: NOW - 4 * DAY, done: false },
    { id: 'w5', en: 'into something', zh: '迷上某事', type: 'phrase', src: 's4', ts: NOW - 4 * DAY + 6 * MIN, done: false },
    { id: 'w6', en: 'serendipity', zh: '意外发现美好事物的运气', type: 'word', src: 's2', ts: NOW - 1 * DAY - 3 * HOUR, done: false },
    { id: 'w7', en: 'itinerary', zh: '行程表', type: 'word', src: 's2', ts: NOW - 1 * DAY - 2 * HOUR, done: false },
    { id: 'w8', en: 'classic', zh: '经典的；老派的', type: 'word', src: 's1', ts: NOW - 2 * HOUR - 20 * MIN, done: false },
    { id: 'w9', en: 'appetizer', zh: '开胃菜', type: 'word', src: 's1', ts: NOW - 2 * HOUR - 12 * MIN, done: true },
    { id: 'w10', en: 'on the house', zh: '店家请客、免费赠送', type: 'phrase', src: 's1', ts: NOW - 2 * HOUR - 4 * MIN, done: true },
  ],
  filters: 'all',
  call: null, // {sid, teacher, review, seconds}
  immersive: false,
  bilingual: true,
};

/* ---------- 工具 ---------- */
const T = id => TEACHERS.find(t => t.id === id) || TEACHERS[0];
const sess = id => state.sessions.find(s => s.id === id);
const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

function ago(ts) {
  const d = new Date(ts), n = new Date();
  const diff = n - ts;
  if (diff < MIN) return '刚刚';
  if (d.toDateString() === n.toDateString()) return `${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`;
  const y = new Date(n - DAY);
  if (d.toDateString() === y.toDateString()) return '昨天';
  if (diff < 7 * DAY) return `${Math.floor(diff / DAY)}天前`;
  return `${d.getMonth() + 1}月${d.getDate()}日`;
}
function ava(t, size = 46, glyphSize) {
  return `<div class="avatar" style="width:${size}px;height:${size}px;background:linear-gradient(140deg,${t.c[0]},${t.c[1]});box-shadow:0 4px 14px ${t.c[1]}40">
    <span class="glyph" style="font-size:${glyphSize || Math.round(size * .58)}px;line-height:1">${t.glyph}</span></div>`;
}
function toast(msg, icon = I.check) {
  const el = document.getElementById('toast');
  el.innerHTML = `<span style="color:#7DE2C0;display:flex;width:16px;height:16px">${icon}</span><span>${msg}</span>`;
  el.classList.add('show');
  clearTimeout(el._t);
  el._t = setTimeout(() => el.classList.remove('show'), 2200);
}
function openSheet(html) {
  document.getElementById('sheet').innerHTML = html;
  document.getElementById('sheet').classList.add('show');
  document.getElementById('scrim').classList.add('show');
}
function closeSheet() {
  document.getElementById('sheet').classList.remove('show');
  document.getElementById('scrim').classList.remove('show');
}
/* 挂断时的“AI 正在总结”遮罩 */
function veil(on, text) {
  const el = document.getElementById('veil');
  el.querySelector('.veil-txt').textContent = text || '';
  el.classList.toggle('show', on);
}
function navBar(title, back = 'chats') {
  return `<div class="pt-safe" style="display:flex;align-items:center;gap:10px;padding-left:12px;padding-right:16px;height:98px">
    <div class="icon-btn" onclick="go('${back}')">${I.back}</div>
    <h2 style="font-size:17px">${title}</h2>
  </div>`;
}

/* ---------- 屏幕 ---------- */
const SCREENS = {

  splash: {
    render: () => `
    <div class="pt-status" style="flex:1;display:flex;flex-direction:column;padding:0 30px">
      <div class="hero">
        <div class="mark">${`<svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round"><path d="M4 12.6V11a8 8 0 0 1 16 0v1.4"/><rect x="2.4" y="12.2" width="4" height="6" rx="2"/><rect x="17.6" y="12.2" width="4" height="6" rx="2"/><path d="M20 20.6a4.4 4.4 0 0 1-4 2.4h-2"/></svg>`}</div>
        <h1 style="font-size:38px;margin-bottom:12px">Talk<span style="color:var(--spark)">Flow</span></h1>
        <p style="font-size:15.5px;line-height:1.6">开口即学<br>生词在对话里自己长出来</p>
      </div>
      <div style="padding-bottom:34px">
        <button class="btn btn-dark" onclick="go('login')" style="margin-bottom:10px">开始使用</button>
        <button class="btn btn-ghost" onclick="go('login')">已有账号 · 登录</button>
      </div>
    </div>`
  },

  login: {
    render: () => `
    <div class="pt-status" style="flex:1;display:flex;flex-direction:column;padding:0 26px">
      <div style="padding-top:44px">
        <h1 style="margin-bottom:8px">登录 TalkFlow</h1>
        <p style="margin-bottom:30px">中国手机号即可注册，无需密码</p>
        <div style="display:flex;align-items:center;gap:10px;margin-bottom:14px">
          <span class="chip on" style="cursor:default">+86</span>
          <input class="field" style="flex:1;margin:0" type="tel" inputmode="numeric" placeholder="手机号" value="138 8888 6666">
        </div>
        <div style="display:flex;gap:10px;align-items:flex-start">
          <input class="field" style="flex:1" inputmode="numeric" placeholder="验证码" value="2 4 8 1">
          <button class="btn btn-ghost btn-sm" style="margin-bottom:10px;white-space:nowrap" onclick="toast('验证码已发送')">获取验证码</button>
        </div>
        <button class="btn btn-dark" onclick="go('onboarding')" style="margin-top:18px">进入</button>
        <p class="tiny" style="text-align:center;margin-top:22px">登录即代表同意《用户协议》与《隐私政策》</p>
      </div>
    </div>`
  },

  onboarding: {
    render: () => `
    <div style="flex:1;display:flex;flex-direction:column">
      ${navBar('学习档案', 'login')}
      <div class="scroll"><div style="padding:6px 20px 26px">
        <p style="margin-bottom:20px">这几步让 AI 老师知道该用什么词、什么语速跟你聊。</p>
        <div class="group" style="margin:0 0 14px">
          <div class="item"><span class="lbl">昵称</span><input class="field" style="width:150px;margin:0;padding:9px 12px;font-size:15px;text-align:right" value="${state.user.name}"></div>
          <div class="item"><span class="lbl">年龄</span><input class="field" style="width:90px;margin:0;padding:9px 12px;font-size:15px;text-align:right" type="number" value="${state.user.age}"></div>
          <div class="item"><span class="lbl">英文水平</span>
            <select class="field" style="width:130px;margin:0;padding:9px 30px 9px 12px;font-size:14px;text-align:right" onclick="event.stopPropagation()">
              <option>初级</option><option selected>中级</option><option>高级</option>
            </select></div>
        </div>
        <div class="sec-label" style="padding-left:4px">学习目标（可多选）</div>
        <div style="display:flex;flex-wrap:wrap;gap:9px;padding:2px 4px 22px">
          ${['日常交流', '考试提分', '职场英语', '出国旅行', '美剧听力'].map(g =>
            `<span class="chip ${state.user.goals.includes(g) ? 'on' : ''}" onclick="this.classList.toggle('on')">${g}</span>`).join('')}
        </div>
        <div class="sec-label" style="padding-left:4px">默认 AI 老师</div>
        <div style="display:flex;gap:10px;padding:2px 4px 26px">
          ${TEACHERS.map(t => `<div onclick="pickTeacher('${t.id}')" style="text-align:center;cursor:pointer;flex:1">
            <div style="position:relative;display:inline-block">${ava(t, 54, 26)}
              ${state.user.teacher === t.id ? `<span style="position:absolute;right:-3px;bottom:-3px;width:20px;height:20px;border-radius:50%;background:var(--spark);border:2.5px solid var(--bg);display:flex;align-items:center;justify-content:center;color:#fff">${`<svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="#fff" stroke-width="3.4" stroke-linecap="round"><path d="M5 12.6l4.6 4.6L19 7.4"/></svg>`}</span>` : ''}
            </div>
            <div style="font-size:12px;font-weight:600;margin-top:7px">${t.name}</div>
            <div class="tiny" style="font-size:10px">${t.style}</div>
          </div>`).join('')}
        </div>
        <button class="btn btn-dark" onclick="go('chats')">开始开口</button>
      </div></div>
    </div>`
  },

  /* ===== 首页：会话 ===== */
  chats: {
    tab: 'chats',
    render: () => {
      const list = [...state.sessions].sort((a, b) => b.ts - a.ts);
      const saved = state.words.filter(w => !w.done).length;
      return `
      <div class="scroll pt-safe pb-tabs">
        <div style="display:flex;align-items:center;justify-content:space-between;padding:8px 20px 4px">
          <h1>会话</h1>
          <div style="display:flex;gap:9px">
            <div class="icon-btn" onclick="toast('原型未实现搜索，正式版按标题与词条检索',I.search)">${I.search}</div>
            <div class="icon-btn" style="position:relative" onclick="go('words')">
              ${I.book}
              ${saved ? `<span style="position:absolute;top:-3px;right:-3px;min-width:17px;height:17px;padding:0 4px;border-radius:9px;background:var(--spark);color:#fff;font-size:10px;font-weight:700;display:flex;align-items:center;justify-content:center">${saved}</span>` : ''}
            </div>
          </div>
        </div>
        <p class="tiny" style="padding:0 20px 14px">Hi ${state.user.name}，${state.sessions.length} 段对话在这里</p>

        <!-- 自由对话入口 -->
        <div style="margin:0 20px 6px;border-radius:26px;background:var(--ink);color:#fff;padding:18px 18px 16px;position:relative;overflow:hidden;cursor:pointer" onclick="startFree()">
          <div style="position:absolute;right:-24px;top:-30px;width:130px;height:130px;border-radius:50%;background:radial-gradient(circle,var(--spark),transparent 68%);opacity:.5"></div>
          <div style="display:flex;align-items:center;gap:14px;position:relative">
            <div class="big-ic" style="width:50px;height:50px;border-radius:50%;background:rgba(255,255,255,.14);display:flex;align-items:center;justify-content:center;color:#fff;flex:none">${I.mic}</div>
            <div style="flex:1;min-width:0">
              <div style="font-size:17px;font-weight:700;letter-spacing:-.3px">自由对话</div>
              <div style="font-size:12.5px;color:rgba(255,255,255,.6);margin-top:3px">随时开口，AI 老师陪你聊任何话题</div>
            </div>
            <span style="color:rgba(255,255,255,.5);width:20px;height:20px;display:block">${I.chev}</span>
          </div>
          <div style="display:flex;gap:8px;margin-top:15px;position:relative" onclick="event.stopPropagation()">
            ${TEACHERS.map(t => `<div onclick="startFree('${t.id}')" title="${t.name} · ${t.style}" style="cursor:pointer;position:relative;opacity:${state.user.teacher === t.id ? 1 : .62};transition:opacity .2s">
              ${ava(t, 36, 18)}
              ${state.user.teacher === t.id ? `<span style="position:absolute;inset:-3px;border-radius:50%;border:2px solid var(--spark)"></span>` : ''}
            </div>`).join('')}
            <div style="flex:1;text-align:right;align-self:center;font-size:11.5px;color:rgba(255,255,255,.45)">点头像指定老师</div>
          </div>
        </div>

        <div class="sec-label">最近对话</div>
        <div class="list" style="margin:0 20px 20px;background:transparent;border:none">
          ${list.map(s => {
            const t = T(s.t);
            return `<div class="cell" onclick="openSession('${s.id}')" style="background:var(--surface)">
              ${ava(t, 46, 22)}
              <div class="cell-body">
                <div class="cell-title">${esc(s.title)}</div>
                <div class="cell-sub">${esc(s.preview)} · ${s.mins}分钟 · ${s.words} 词</div>
              </div>
              <div class="cell-time">${ago(s.ts)}</div>
            </div>`;
          }).join('')}
        </div>
      </div>`;
    }
  },

  /* ===== 对话中 ===== */
  call: {
    render: () => {
      const c = state.call;
      if (!c) return `<div class="pt-status" style="flex:1;padding:30px"><p>没有进行中的对话。</p><button class="btn btn-ghost" onclick="go('chats')" style="margin-top:14px">返回会话</button></div>`;
      const t = T(c.teacher);
      const L = (en, zhMeaning, type) => `<b class="wlink ${saved(en) ? 'saved' : ''}" onclick="saveWord('${en}','${zhMeaning}','${type || 'word'}')">${en}</b>`;
      const hideZh = state.immersive || !state.bilingual;
      const m = (who, en, zh) => `<div class="msg ${who}${hideZh ? ' hide-zh' : ''}">${en}${zh ? `<span class="zh">${zh}</span>` : ''}</div>`;

      return `
      <div class="call-head">
        <div class="icon-btn" onclick="hangup()">${I.x}</div>
        <div style="flex:1;text-align:center;min-width:0">
          <div class="call-name">${t.name} · ${c.review ? '复习模式' : '对话中'}</div>
          <div class="call-status" style="justify-content:center"><i class="live"></i>${c.mode} · <span id="timer">${fmtClock(c.seconds)}</span></div>
        </div>
        <div class="icon-btn" onclick="teacherSheet()">${I.mask}</div>
      </div>

      <div class="ctrl-strip">
        <span class="chip" onclick="modeSheet()">${I.spark} ${c.mode}</span>
        <span class="chip ${state.bilingual ? 'on' : ''}" onclick="toggleBilingual()">${I.captions} 中英字幕</span>
        <span class="chip ${state.immersive ? 'spark on' : ''}" onclick="toggleImmersive()">${I.moon} 沉浸式</span>
        ${c.review ? `<span class="chip ${c.reviewWord.done ? 'spark on' : ''}" onclick="confirmMastered('${c.review}')">${I.check} ${c.reviewWord.done ? '已掌握' : '确认掌握 ' + c.reviewWord.en}</span>` : ''}
      </div>

      ${state.immersive ? `<div class="toast show" style="bottom:auto;top:46%;transform:translate(-50%,-50%)">沉浸式 · 字幕已隐藏</div>` : ''}

      <div class="chat-scroll" id="chat">
        ${m('ai', `Hi! Shall we practice ordering at a café? Tell me what you would like.`, `嗨，我们练一下在咖啡店点单？告诉我想点什么。`)}
        ${m('me', `I would like a flat white and something to snack on.`, `我想要一杯馥芮白，再来点小食。`)}
        ${m('ai', `Good — <b>snack on</b> sounds natural here. This place gets ${`packed`} at noon, so grab a seat.`, `很好，snack on 说得很地道。这家店中午很挤，先找个座位。`)
          .replace('<b>snack on</b>', L('snack on', '随便吃点小食', 'phrase')).replace('packed', L('packed', '拥挤的、满座的'))}
        ${m('me', `No worries. Is the croissant still fresh today?`, `没关系。可颂今天还新鲜吗？`)}
        ${m('ai', `Baked this morning — they are ${`flaky`} and absolutely ${`worth it`}.`, `今早刚烤的，一层层酥，绝对值得。`)
          .replace('flaky', L('flaky', '酥的、一层层松脆的'))
          .replace('worth it', L('worth it', '值得（这么花时间/花钱）'))}
        ${m('me', `Then I will take two. My friend is paying — lucky me.`, `那我拿两个，朋友买单，我真幸运。`)}
        ${m('ai', `Ha! When the café gives you something free, we say it is ${`on the house`}. Try that one.`, `哈！店家送的东西我们说 on the house，试试这句。`)
          .replace('on the house', L('on the house', '店家请客、免费赠送', 'phrase'))}
      </div>

      <div class="call-foot">
        <p class="tiny" style="text-align:center">点对话里带下划线的词，收进单词本并记下来源会话</p>
        <button class="hangup" onclick="hangup()" aria-label="挂断">${hangupIcon}</button>
      </div>`;
    }
  },

  /* ===== 单词本 ===== */
  words: {
    tab: 'words',
    render: () => {
      const all = [...state.words].sort((a, b) => b.ts - a.ts);
      const list = all.filter(x => state.filters === 'all' ? !x.done : state.filters === 'done' ? x.done : x.type === state.filters);
      const learning = all.filter(x => !x.done).length, mastered = all.filter(x => x.done).length;
      return `
      <div class="scroll pt-safe pb-tabs">
        <div style="padding:8px 20px 4px"><h1>单词本</h1></div>
        <div style="padding:16px 20px 6px">
          <div class="row" style="gap:0;background:var(--surface);border:1px solid var(--line);border-radius:20px;padding:16px 6px">
            <div class="stat-mini"><div class="k">${all.length}</div><div class="v">收录总数</div></div>
            <div style="width:1px;height:30px;background:var(--line)"></div>
            <div class="stat-mini"><div class="k" style="color:var(--spark)">${learning}</div><div class="v">待复习</div></div>
            <div style="width:1px;height:30px;background:var(--line)"></div>
            <div class="stat-mini"><div class="k" style="color:var(--ok)">${mastered}</div><div class="v">已掌握</div></div>
          </div>
        </div>
        <div style="display:flex;gap:8px;padding:10px 20px 12px;overflow-x:auto;scrollbar-width:none">
          ${[['all', '待复习'], ['word', '单词'], ['phrase', '习语'], ['done', '已掌握']].map(([k, l]) =>
            `<span class="chip ${state.filters === k ? 'on' : ''}" onclick="setFilter('${k}')">${l}</span>`).join('')}
        </div>

        <div class="sec-label">按收录顺序</div>
        <div class="list" style="margin:0 20px 18px">
          ${list.length ? list.map(x => {
            const s = sess(x.src);
            return `<div class="word-cell" onclick="wordSheet('${x.id}')">
              <div style="flex:1;min-width:0">
                <div class="row" style="gap:8px"><span class="word-en">${esc(x.en)}</span><span class="tag ${x.type === 'phrase' ? 'phrase' : ''} ${x.done ? 'done' : ''}">${x.done ? '已掌握' : x.type === 'phrase' ? '习语' : '单词'}</span></div>
                <div class="word-zh">${esc(x.zh)}</div>
                <div class="word-src">${I.chats}<span onclick="event.stopPropagation();openSession('${x.src}')" style="text-decoration:underline dotted">${s ? esc(s.title) : '已删除的会话'}</span> · ${ago(x.ts)}</div>
              </div>
              <span style="color:#C6CBD2;width:18px;height:18px;display:block">${I.chev}</span>
            </div>`;
          }).join('') : `<div style="padding:34px 20px;text-align:center"><p class="tiny">这里还空着<br>在对话中点一下单词就会记进来</p></div>`}
        </div>
        <p class="tiny" style="text-align:center;padding:0 30px 24px;line-height:1.7">点词条可与老师单独复习它，<br>确认掌握后自动移出待复习列表</p>
      </div>`;
    }
  },

  /* ===== 我的 ===== */
  me: {
    tab: 'me',
    render: () => {
      const t = T(state.user.teacher);
      const learning = state.words.filter(w => !w.done).length;
      const mastered = state.words.filter(w => w.done).length;
      const mins = state.sessions.reduce((a, s) => a + s.mins, 0);
      return `
      <div class="scroll pt-safe pb-tabs">
        <div style="padding:8px 20px 4px"><h1>我的</h1></div>

        <div style="margin:14px 20px 14px;display:flex;align-items:center;gap:14px;cursor:pointer" onclick="go('profile')">
          <div style="width:60px;height:60px;border-radius:50%;background:linear-gradient(140deg,#FFD9C4,#FF9A68);display:flex;align-items:center;justify-content:center;font-size:24px;font-weight:700;color:#B4441E">${state.user.name[0]}</div>
          <div style="flex:1;min-width:0">
            <div style="font-size:19px;font-weight:700;letter-spacing:-.3px">${state.user.name}</div>
            <div class="tiny" style="margin-top:3px">${state.user.age} 岁 · ${state.user.level} · ${state.user.goals.join(' / ')}</div>
          </div>
          <span style="color:#C6CBD2;width:18px;height:18px;display:block">${I.chev}</span>
        </div>

        <div style="margin:0 20px 16px;background:var(--ink);border-radius:22px;padding:18px;color:#fff;position:relative;overflow:hidden">
          <div style="position:absolute;right:-30px;bottom:-40px;width:140px;height:140px;border-radius:50%;background:radial-gradient(circle,var(--spark),transparent 70%);opacity:.45"></div>
          <div style="position:relative;display:flex;align-items:flex-end;justify-content:space-between">
            <div>
              <div style="font-size:11.5px;color:rgba(255,255,255,.55);font-weight:600;letter-spacing:.4px">账户余额</div>
              <div class="big-num" style="color:#fff;margin-top:6px">¥ ${state.user.balance.toFixed(2)}</div>
              <div style="font-size:12px;color:rgba(255,255,255,.55);margin-top:4px">约可对话 ${Math.round(state.user.balance * 2)} 分钟</div>
            </div>
            <button class="btn btn-sm" style="background:var(--spark);color:#fff" onclick="go('recharge')">充值</button>
          </div>
        </div>

        <div class="group">
          <div class="item" onclick="go('profile')">
            <span class="ic" style="background:${t.c[1]}1A;color:${t.c[1]}">${t.glyph}</span>
            <span class="lbl">默认 AI 老师</span><span class="val">${t.name} · ${t.style}</span><span class="chev" style="width:16px;height:16px;display:block">${I.chev}</span>
          </div>
        </div>

        <div class="group">
          <div class="item" onclick="go('analytics')"><span class="ic" style="background:#EDE9FE;color:#6D28D9">${I.chart}</span><span class="lbl">学习分析</span><span class="val">${mins} 分钟 · ${mastered} 词</span><span class="chev" style="width:16px;height:16px;display:block">${I.chev}</span></div>
          <div class="item" onclick="go('words')"><span class="ic" style="background:var(--spark-soft);color:#C7502A">${I.book}</span><span class="lbl">单词本</span><span class="val">${learning} 待复习</span><span class="chev" style="width:16px;height:16px;display:block">${I.chev}</span></div>
        </div>

        <div class="group">
          <div class="item"><span class="ic" style="background:#E8F7EF;color:var(--ok)">${I.bell}</span><span class="lbl">练习提醒</span><span class="val">每天 20:00</span></div>
          <div class="item" onclick="go('profile')"><span class="ic" style="background:#EEF2FF;color:#4F63F2">${svg('<circle cx="12" cy="12" r="3.2"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5v.2a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.6-1.1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3H9.3a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9v.1a1.7 1.7 0 0 0 1.5 1h.2a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.6 1z"/>')}</span><span class="lbl">更多设置</span><span class="val">昵称 / 年龄 / 目标</span><span class="chev" style="width:16px;height:16px;display:block">${I.chev}</span></div>
        </div>

        <div class="group">
          <div class="item" onclick="go('splash')"><span class="lbl" style="color:var(--hot);text-align:center">退出登录</span></div>
        </div>
      </div>`;
    }
  },

  /* ===== 学习分析（从「我的」进入） ===== */
  analytics: {
    render: () => {
      const days = ['一', '二', '三', '四', '五', '六', '日'];
      const vals = [35, 62, 48, 80, 68, 92, 100];
      const mastered = state.words.filter(w => w.done).length + 146;
      const mins = state.sessions.reduce((a, s) => a + s.mins, 0);
      return `
      <div style="flex:1;display:flex;flex-direction:column">
        ${navBar('学习分析', 'me')}
        <div class="scroll"><div style="padding:0 20px 26px">
          <div class="card" style="margin:0 0 14px;padding:20px">
            <div style="display:flex;gap:0;margin-bottom:18px">
              <div class="stat-mini"><div class="k">${mastered}</div><div class="v">累计掌握</div></div>
              <div style="width:1px;height:32px;background:var(--line)"></div>
              <div class="stat-mini"><div class="k">${Math.round(mins / 6 * 10) / 10}h</div><div class="v">本月时长</div></div>
              <div style="width:1px;height:32px;background:var(--line)"></div>
              <div class="stat-mini"><div class="k" style="color:var(--spark)">6</div><div class="v">连续天数</div></div>
            </div>
            <h2 style="margin-bottom:12px">本周开口时长</h2>
            <div class="bars">${vals.map((v, i) => `<i class="${i === 6 ? 'now' : v > 70 ? 'hi' : ''}" style="height:${v}%"></i>`).join('')}</div>
            <div style="display:flex;justify-content:space-between;font-size:11px;color:var(--muted);margin-top:9px">${days.map(d => `<span style="flex:1;text-align:center">${d}</span>`).join('')}</div>
          </div>
          <div class="card" style="margin:0 0 14px">
            <h2 style="margin-bottom:14px">词汇来源</h2>
            ${[...state.sessions].sort((a, b) => b.words - a.words).slice(0, 4).map(s => {
              const pct = Math.round(s.words / 9 * 100);
              const t = T(s.t);
              return `<div style="margin-bottom:13px">
                <div style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:6px"><span style="font-weight:600">${esc(s.title)}</span><span class="tiny">${s.words} 词</span></div>
                <div style="height:6px;border-radius:3px;background:var(--line)"><div style="height:100%;width:${pct}%;border-radius:3px;background:linear-gradient(90deg,${t.c[0]},${t.c[1]})"></div></div>
              </div>`;
            }).join('')}
          </div>
          <div class="card" style="display:flex;align-items:center;gap:13px">
            <div style="flex:1"><h2 style="margin-bottom:4px">学习 vs 复习</h2><p class="tiny">本月新学 62%，复习巩固 38%</p></div>
            <div style="width:62px;height:62px;border-radius:50%;background:conic-gradient(var(--spark) 0 62%,var(--line) 62% 100%);display:flex;align-items:center;justify-content:center">
              <div style="width:44px;height:44px;border-radius:50%;background:var(--surface);display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:700">62%</div>
            </div>
          </div>
        </div></div>
      </div>`;
    }
  },

  /* ===== 个人设置 ===== */
  profile: {
    render: () => `
    <div style="flex:1;display:flex;flex-direction:column">
      ${navBar('个人设置', 'me')}
      <div class="scroll"><div style="padding:0 20px 26px">
        <div class="group">
          <div class="item"><span class="lbl">昵称</span><span class="val">${state.user.name}</span></div>
          <div class="item"><span class="lbl">年龄</span><span class="val">${state.user.age}</span></div>
          <div class="item"><span class="lbl">英文水平</span><span class="val">${state.user.level}</span></div>
          <div class="item"><span class="lbl">学习目标</span><span class="val">${state.user.goals.join(' · ')}</span></div>
        </div>
        <div class="group">
          <div class="item" onclick="teacherSheet()"><span class="lbl">默认 AI 老师</span><span class="val">${T(state.user.teacher).name} · ${T(state.user.teacher).style}</span></div>
          <div class="item"><span class="lbl">对话模式</span><span class="val">教学型</span></div>
          <div class="item"><span class="lbl">默认沉浸式</span><span class="val">关闭</span></div>
        </div>
        <button class="btn btn-ghost" onclick="go('chats')" style="margin-top:8px">保存</button>
      </div></div>
    </div>`
  },

  /* ===== 充值 ===== */
  recharge: {
    render: () => `
    <div style="flex:1;display:flex;flex-direction:column">
      ${navBar('充值', 'me')}
      <div class="scroll"><div style="padding:0 20px 26px">
        <p style="margin-bottom:16px">按对话时长计费，1 元 ≈ 2 分钟实时语音。</p>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:18px">
          ${[['¥30', '约 60 分钟'], ['¥68', '约 136 分钟', '热销'], ['¥128', '约 256 分钟'], ['¥198', '约 396 分钟']].map(([p, d, hot], i) =>
            `<div class="card tap" style="margin:0;position:relative;border:1.5px solid ${i === 1 ? 'var(--spark)' : 'var(--line)'}" onclick="toast('已选择 ${p}')">
              ${hot ? `<span style="position:absolute;top:-9px;right:12px;background:var(--spark);color:#fff;font-size:10px;font-weight:700;padding:2px 8px;border-radius:8px">${hot}</span>` : ''}
              <div style="font-size:22px;font-weight:700;letter-spacing:-.5px">${p}</div>
              <div class="tiny" style="margin-top:3px">${d}</div>
            </div>`).join('')}
        </div>
        <div class="group" style="margin:0 0 18px">
          <div class="item"><span class="lbl">当前余额</span><span class="val" style="font-weight:700;color:var(--ink)">¥ ${state.user.balance.toFixed(2)}</span></div>
        </div>
        <button class="btn btn-dark" onclick="state.user.balance+=68;go('me');toast('充值成功 · 余额 ¥ 93.00')">微信支付</button>
      </div></div>
    </div>`
  },
};

/* ---------- 页签栏 ---------- */
const TABS = [
  { id: 'chats', label: '会话', icon: I.chats },
  { id: 'words', label: '单词本', icon: I.book },
  { id: 'me', label: '我的', icon: I.user },
];
function renderTabs(active) {
  const bar = document.getElementById('tabbar');
  bar.innerHTML = TABS.map(t => `
    <div class="tab ${active === t.id ? 'on' : ''}" onclick="go('${t.id}')">
      ${t.icon}<span>${t.label}</span>
    </div>`).join('');
  bar.classList.toggle('show', !!active);
}

/* ---------- 路由 ---------- */
let current = 'splash';
function go(id) {
  current = id;
  const scr = SCREENS[id];
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  const el = document.getElementById('screen-' + id);
  el.innerHTML = scr.render();
  el.classList.add('active');
  renderTabs(scr.tab);
  closeSheet();
  if (id === 'call') {
    // 计时跨重绘连续：只在同一次通话内递增
    if (!state.call) return;
    const box = document.getElementById('chat');
    if (box) setTimeout(() => box.scrollTop = box.scrollHeight, 60);
    clearInterval(callTimer);
    callTimer = setInterval(() => {
      state.call.seconds++;
      const el = document.getElementById('timer');
      if (el) {
        const s = state.call.seconds;
        el.textContent = `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
      }
    }, 1000);
  } else {
    clearInterval(callTimer);
  }
}
let callTimer = null;

/* ---------- 会话相关 ---------- */
function saved(en) { return state.words.some(w => w.en === en); }

const fmtClock = s => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

function newCall(sid, teacherId, reviewWordId) {
  const w = reviewWordId ? state.words.find(x => x.id === reviewWordId) : null;
  state.call = {
    sid, teacher: teacherId || state.user.teacher,
    review: w ? w.id : null, reviewWord: w,
    mode: T(teacherId || state.user.teacher).style,
    seconds: 0
  };
  state.immersive = false;
  state.bilingual = true;
  go('call');
}
function startFree(teacherId) {
  const t = teacherId || state.user.teacher;
  const s = { id: 'n' + Date.now(), title: '新的自由对话', t, ts: Date.now(), preview: '随时开口 · 话题随你', mode: T(t).style, words: 0, mins: 0, fresh: true };
  state.sessions.unshift(s);
  newCall(s.id, t);
}
function openSession(id) {
  const s = sess(id);
  if (!s) return;
  s.ts = Date.now();           // 进入即回到列表顶部
  newCall(id, s.t);
}
function reviewWord(wordId) {
  const w = state.words.find(x => x.id === wordId);
  if (!w) return;
  closeSheet();
  const s = sess(w.src);
  newCall(w.src, s ? s.t : state.user.teacher, w.id);
}
function confirmMastered(wordId) {
  const w = state.words.find(x => x.id === wordId);
  if (!w || w.done) return;
  w.done = true;
  go('call');
  toast('「' + w.en + '」标记为已掌握，已移出待复习', I.check);
}

function saveWord(en, zh, type) {
  if (saved(en)) { toast('「' + en + '」已在单词本', I.book); return; }
  const c = state.call;
  state.words.push({ id: 'w' + Date.now() + Math.floor(Math.random() * 99), en, zh, type: type || 'word', src: c ? c.sid : null, ts: Date.now(), done: false });
  const s = sess(c && c.sid);
  if (s) s.words += 1;
  toast('已收录「' + en + '」到单词本', I.check);
  // 只重绘对话区里该词的链接状态，避免整屏重渲染打断节奏
  document.querySelectorAll('.wlink').forEach(el => { if (el.textContent === en) el.classList.add('saved'); });
}

/* 挂断：AI 总结标题 → 保存 → 直接回首页，不展示学习成果 */
function hangup() {
  const c = state.call;
  if (!c) { go('chats'); return; }
  const s = sess(c.sid);
  const t = T(c.teacher);
  const added = Math.round(c.seconds / 60) || 1;
  veil(true, t.name + ' 正在总结这段对话…');
  setTimeout(() => {
    veil(false);
    if (s) {
      if (s.fresh) s.title = '咖啡店点单：馥芮白与可颂';   // AI 总结出的标题
      s.fresh = false;
      s.ts = Date.now();                                  // 置顶
      s.mins += added;
      s.preview = 'Want to try that one?';
    }
    state.call = null;
    go('chats');
    setTimeout(() => toast(s ? '已保存 · ' + s.title : '已保存', I.check), 280);
  }, 1400);
}

function toggleImmersive() {
  state.immersive = !state.immersive;
  state.bilingual = state.immersive ? state.bilingual : true;
  go('call');
}
function toggleBilingual() {
  state.bilingual = !state.bilingual;
  if (!state.bilingual) state.immersive = false;
  go('call');
  toast(state.bilingual ? '字幕：中英双语' : '字幕：仅英文', I.captions);
}
function setFilter(f) { state.filters = f; go('words'); }
function pickTeacher(id) { state.user.teacher = id; go('onboarding'); }

/* ---------- 抽屉 ---------- */
function teacherSheet() {
  openSheet(`<div class="grab"></div><h3>切换 AI 老师</h3><p class="tiny" style="margin-bottom:10px">不同老师的语速、纠错密度不一样</p>
    ${TEACHERS.map(t => `<div class="opt" onclick="chooseTeacher('${t.id}')">
      ${ava(t, 40, 20)}
      <div style="flex:1"><div style="font-weight:600;font-size:15px">${t.name} · ${t.style}</div><div class="tiny">${t.desc}</div></div>
      ${t.id === (state.call ? state.call.teacher : state.user.teacher) ? `<span style="color:var(--spark);width:20px;height:20px;display:block">${I.check}</span>` : ''}
    </div>`).join('')}`);
}
function chooseTeacher(id) {
  state.user.teacher = id;
  if (state.call) {
    state.call.teacher = id;
    state.call.mode = T(id).style;
    const s = sess(state.call.sid);
    if (s) s.t = id;                        // 会话头像跟随当前老师
    closeSheet(); go('call'); toast('已切换为 ' + T(id).name, I.check);
  }
  else { closeSheet(); go(current); toast('默认老师：' + T(id).name, I.check); }
}
function modeSheet() {
  const modes = ['温柔型', '教学型', '纠正型', '闲聊型'];
  openSheet(`<div class="grab"></div><h3>对话模式</h3><p class="tiny" style="margin-bottom:6px">决定老师怎么回应你、纠错的密度</p>
    ${modes.map(m => `<div class="opt" onclick="setMode('${m}')"><div style="flex:1;font-weight:600;font-size:15px">${m}</div>
      ${state.call && state.call.mode === m ? `<span style="color:var(--spark);width:20px;height:20px;display:block">${I.check}</span>` : ''}</div>`).join('')}`);
}
function setMode(m) { if (state.call) state.call.mode = m; closeSheet(); go('call'); toast('对话模式 · ' + m, I.check); }

function wordSheet(id) {
  const w = state.words.find(x => x.id === id);
  if (!w) return;
  const s = sess(w.src);
  const t = T(s ? s.t : state.user.teacher);
  openSheet(`<div class="grab"></div>
    <div style="font-size:26px;font-weight:700;letter-spacing:-.6px">${esc(w.en)}</div>
    <p style="margin:4px 0 14px">${esc(w.zh)}</p>
    <div style="display:flex;gap:8px;margin-bottom:16px">
      <span class="chip ${w.type === 'phrase' ? '' : 'on'}">${w.type === 'phrase' ? '习语' : '单词'}</span>
      <span class="chip">${ago(w.ts)}收录</span>
      ${w.done ? '<span class="chip" style="background:#E8F7EF;color:var(--ok);border-color:#D6F0E2">已掌握</span>' : ''}
    </div>
    <div class="opt" onclick="closeSheet();openSession('${w.src}')" style="${s ? '' : 'opacity:.4;pointer-events:none'}">
      <div style="width:38px;height:38px;border-radius:12px;background:var(--bg);display:flex;align-items:center;justify-content:center;color:var(--ink-2)">${I.chats}</div>
      <div style="flex:1"><div style="font-weight:600;font-size:14.5px">来源会话</div><div class="tiny">${s ? esc(s.title) + ' · ' + ago(s.ts) : '会话已删除'}</div></div>
      <span class="chev" style="width:16px;height:16px;display:block">${I.chev}</span>
    </div>
    <div style="display:flex;gap:10px;margin-top:18px">
      <button class="btn btn-ghost" style="flex:1" onclick="toggleDone('${w.id}')">${w.done ? '放回待复习' : '标记已掌握'}</button>
      <button class="btn btn-dark" style="flex:1" onclick="reviewWord('${w.id}')">和 ${t.name} 复习</button>
    </div>`);
}
function toggleDone(id) {
  const w = state.words.find(x => x.id === id);
  w.done = !w.done;
  closeSheet();
  go('words');
  toast(w.done ? '「' + w.en + '」已掌握，移出待复习' : '「' + w.en + '」已放回待复习', I.check);
}

/* ---------- 初始化 ---------- */
function init() {
  const box = document.getElementById('screens');
  const ids = Object.keys(SCREENS);
  ids.forEach(id => {
    const d = document.createElement('div');
    d.id = 'screen-' + id;
    d.className = 'screen';
    box.appendChild(d);
  });
  go('splash');
}
init();
