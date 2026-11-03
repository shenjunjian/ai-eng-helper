// 页面定义
const screens = {
  splash: {
    html: `
      <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:40px;text-align:center">
        <div style="width:120px;height:120px;margin-bottom:30px">
          <svg viewBox="0 0 120 120" xmlns="http://www.w3.org/2000/svg">
            <defs>
              <linearGradient id="logoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:#4F46E5"/>
                <stop offset="100%" style="stop-color:#818CF8"/>
              </linearGradient>
            </defs>
            <circle cx="60" cy="60" r="55" fill="url(#logoGrad)"/>
            <path d="M 35 50 Q 60 30 85 50" stroke="#fff" stroke-width="5" fill="none" stroke-linecap="round"/>
            <circle cx="45" cy="55" r="4" fill="#fff"/>
            <circle cx="75" cy="55" r="4" fill="#fff"/>
            <path d="M 50 70 Q 60 80 70 70" stroke="#fff" stroke-width="4" fill="none" stroke-linecap="round"/>
            <path d="M 40 85 Q 60 95 80 85" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" opacity="0.6"/>
          </svg>
        </div>
        <h1 style="margin-bottom:12px">TalkFlow</h1>
        <p style="font-size:16px;margin-bottom:40px">沉浸式英语对话学习<br>让单词在交流中自然积累</p>
        <button class="btn btn-primary" onclick="go('login')" style="width:100%;margin-bottom:12px">开始使用</button>
        <button class="btn btn-secondary" onclick="go('login')" style="width:100%">已有账号登录</button>
      </div>
    `
  },
  login: {
    html: `
      <div style="padding:40px 24px;flex:1;display:flex;flex-direction:column">
        <h1 style="margin-bottom:8px">欢迎回来</h1>
        <p style="margin-bottom:32px">使用手机号登录</p>
        <div class="card" style="margin:0 0 20px 0">
          <div style="display:flex;gap:12px;margin-bottom:16px">
            <select class="input" style="width:100px;margin-bottom:0">
              <option>+86</option>
            </select>
            <input class="input" type="tel" placeholder="请输入手机号" style="flex:1;margin-bottom:0">
          </div>
          <input class="input" type="password" placeholder="请输入密码">
          <button class="btn btn-primary" onclick="go('onboarding')" style="width:100%">登录</button>
        </div>
        <div style="text-align:center;margin-top:auto">
          <p>还没有账号？<span style="color:var(--primary);font-weight:600;cursor:pointer" onclick="go('onboarding')">立即注册</span></p>
        </div>
      </div>
    `
  },
  onboarding: {
    html: `
      <div class="nav-back" onclick="go('login')">← 返回</div>
      <div style="padding:20px 24px;flex:1">
        <h1 style="margin-bottom:8px">完善学习档案</h1>
        <p style="margin-bottom:24px">让AI老师更懂你</p>
        <div class="card" style="margin:0 0 16px 0">
          <h2>基本信息</h2>
          <input class="input" placeholder="昵称（如：小明）" value="林小雨">
          <input class="input" type="number" placeholder="年龄" value="16">
          <select class="input">
            <option>英文水平：初级</option>
            <option selected>英文水平：中级</option>
            <option>英文水平：高级</option>
          </select>
        </div>
        <div class="card" style="margin:0 0 16px 0">
          <h2>学习目标</h2>
          <div style="display:flex;flex-wrap:wrap;gap:10px">
            <span class="badge badge-primary" style="padding:8px 16px">日常交流</span>
            <span class="badge badge-primary" style="padding:8px 16px;background:var(--primary);color:#fff">考试提分</span>
            <span class="badge badge-primary" style="padding:8px 16px">职场英语</span>
            <span class="badge badge-primary" style="padding:8px 16px">出国旅行</span>
          </div>
        </div>
        <button class="btn btn-primary" onclick="go('home')" style="width:100%;margin-top:12px">开始体验</button>
      </div>
    `
  },
  home: {
    html: `
      <div style="padding:20px 24px 100px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
          <div>
            <h1 style="font-size:22px">你好，林小雨</h1>
            <p style="font-size:14px">今天也要坚持学习哦</p>
          </div>
          <div style="width:48px;height:48px;border-radius:50%;background:var(--primary);display:flex;align-items:center;justify-content:center;color:#fff;font-weight:700">林</div>
        </div>
        <div class="card" style="background:linear-gradient(135deg,#4F46E5,#818CF8);color:#fff;margin:0 0 20px 0">
          <p style="color:rgba(255,255,255,.8);font-size:14px;margin-bottom:4px">今日学习时长</p>
          <div style="font-size:36px;font-weight:700">25<span style="font-size:16px">分钟</span></div>
          <p style="color:rgba(255,255,255,.8);font-size:13px;margin-top:8px">已掌握 12 个新词汇</p>
        </div>
        <h2 style="margin:0 16px 12px">快速开始</h2>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin:0 16px 20px">
          <div class="card" style="margin:0;text-align:center;cursor:pointer" onclick="go('quick-add')">
            <div style="font-size:36px;margin-bottom:8px">➕</div>
            <div style="font-weight:600;color:var(--text)">快速录入</div>
          </div>
          <div class="card" style="margin:0;text-align:center;cursor:pointer" onclick="go('learn-setup')">
            <div style="font-size:36px;margin-bottom:8px">🎯</div>
            <div style="font-weight:600;color:var(--text)">英语学习</div>
          </div>
          <div class="card" style="margin:0;text-align:center;cursor:pointer" onclick="go('review')">
            <div style="font-size:36px;margin-bottom:8px">📚</div>
            <div style="font-weight:600;color:var(--text)">复习巩固</div>
          </div>
          <div class="card" style="margin:0;text-align:center;cursor:pointer" onclick="go('analytics')">
            <div style="font-size:36px;margin-bottom:8px">📊</div>
            <div style="font-weight:600;color:var(--text)">学习分析</div>
          </div>
        </div>
        <h2 style="margin:0 16px 12px">稍后学习</h2>
        <div class="card" style="margin:0 16px">
          <div style="display:flex;justify-content:space-between;align-items:center;padding:12px 0;border-bottom:1px solid var(--border)">
            <div>
              <div style="font-weight:600;color:var(--text)">break a leg</div>
              <div style="font-size:13px;color:var(--muted)">祝你好运</div>
            </div>
            <span class="badge badge-orange">习语</span>
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center;padding:12px 0">
            <div>
              <div style="font-weight:600;color:var(--text)">serendipity</div>
              <div style="font-size:13px;color:var(--muted)">意外发现珍宝的运气</div>
            </div>
            <span class="badge badge-primary">词汇</span>
          </div>
        </div>
      </div>
      ${tabBar('home')}
    `
  },
  'quick-add': {
    html: `
      <div class="nav-back" onclick="go('home')">← 返回</div>
      <div style="padding:20px 24px;flex:1">
        <h1 style="margin-bottom:8px">快速录入词条</h1>
        <p style="margin-bottom:24px">通过AI对话或手动录入生词、习语</p>
        <div class="card" style="margin:0 0 20px 0">
          <h2>AI 智能录入</h2>
          <p style="margin-bottom:16px">描述你想学习的场景或单词，AI自动提取词条</p>
          <div style="background:var(--bg);border-radius:12px;padding:16px;margin-bottom:12px">
            <p style="font-size:14px;font-style:italic">"我想学习关于餐厅点餐的常用表达"</p>
          </div>
          <button class="btn btn-primary" onclick="go('quick-add')" style="width:100%">🎤 语音描述</button>
        </div>
        <div class="card" style="margin:0 0 20px 0">
          <h2>手动录入</h2>
          <input class="input" placeholder="英文词条（如：break a leg）">
          <input class="input" placeholder="中文释义（如：祝你好运）">
          <input class="input" placeholder="备注/例句（可选）">
          <button class="btn btn-primary" onclick="go('home')" style="width:100%">添加到稍后学习</button>
        </div>
      </div>
    `
  },
  'learn-setup': {
    html: `
      <div class="nav-back" onclick="go('home')">← 返回</div>
      <div style="padding:20px 24px;flex:1">
        <h1 style="margin-bottom:8px">开始英语学习</h1>
        <p style="margin-bottom:24px">选择学习方式，进入沉浸式对话</p>
        <div class="card" style="margin:0 0 16px 0;cursor:pointer" onclick="go('conversation')">
          <h2>📋 从稍后学习开始</h2>
          <p>选择"稍后学习"列表中的词汇组</p>
          <div style="margin-top:12px">
            <span class="badge badge-primary">break a leg</span>
            <span class="badge badge-primary">serendipity</span>
            <span class="badge badge-primary">+3</span>
          </div>
        </div>
        <div class="card" style="margin:0 0 16px 0;cursor:pointer" onclick="go('conversation')">
          <h2>📝 输入文本学习</h2>
          <p>粘贴一段英文，AI围绕内容展开对话</p>
        </div>
        <div class="card" style="margin:0 0 16px 0;cursor:pointer" onclick="go('conversation')">
          <h2>🖼️ 图片识别学习</h2>
          <p>上传图片，AI识别内容并生成对话</p>
        </div>
        <div class="card" style="margin:0;cursor:pointer;background:linear-gradient(135deg,#4F46E5,#818CF8);color:#fff" onclick="go('conversation')">
          <h2 style="color:#fff">🎤 自由对话</h2>
          <p style="color:rgba(255,255,255,.8)">不限主题，随心开启英语交流</p>
        </div>
      </div>
    `
  },
  conversation: {
    html: `
      <div style="display:flex;flex-direction:column;height:100%">
        <div style="padding:12px 16px;background:var(--card);border-bottom:2px solid var(--border);display:flex;justify-content:space-between;align-items:center">
          <div class="nav-back" style="padding:0" onclick="go('learn-setup')">← 退出</div>
          <div style="text-align:center">
            <div style="font-weight:700;color:var(--text)">AI 老师 · Emma</div>
            <div style="font-size:12px;color:var(--accent)">● 正在对话</div>
          </div>
          <div style="display:flex;gap:8px">
            <button style="background:none;border:none;font-size:20px;cursor:pointer" onclick="toggleSettings()">⚙️</button>
          </div>
        </div>
        <div id="settings-panel" style="display:none;padding:12px 16px;background:var(--card);border-bottom:2px solid var(--border);gap:8px;flex-wrap:wrap">
          <span class="badge badge-primary" style="cursor:pointer">🎭 切换角色</span>
          <span class="badge badge-primary" style="cursor:pointer">🎚️ 对话模式：教学型</span>
          <span class="badge badge-primary" style="cursor:pointer" onclick="toggleImmersive()">👁️ 沉浸式：关</span>
          <span class="badge badge-primary" style="cursor:pointer">🌐 中英字幕</span>
        </div>
        <div id="chat-area" style="flex:1;overflow-y:auto;padding:16px;display:flex;flex-direction:column;gap:12px">
          <div style="align-self:flex-start;max-width:80%;background:var(--card);border-radius:16px;padding:12px 16px;box-shadow:0 2px 8px rgba(0,0,0,.06)">
            <p style="font-size:14px;margin-bottom:4px">Hi! I'm Emma. Let's practice ordering food at a restaurant. What would you like to order?</p>
            <p class="subtitle" style="font-size:12px;color:var(--muted)">嗨！我是Emma。我们来练习在餐厅点餐。你想点什么？</p>
          </div>
          <div style="align-self:flex-end;max-width:80%;background:var(--primary);color:#fff;border-radius:16px;padding:12px 16px">
            <p style="font-size:14px;margin-bottom:4px;color:#fff">I'd like a cheeseburger and some fries, please.</p>
            <p class="subtitle" style="font-size:12px;color:rgba(255,255,255,.7)">我想要一个芝士汉堡和一些薯条。</p>
          </div>
          <div style="align-self:flex-start;max-width:80%;background:var(--card);border-radius:16px;padding:12px 16px;box-shadow:0 2px 8px rgba(0,0,0,.06)">
            <p style="font-size:14px;margin-bottom:4px">Great choice! That's a classic American meal. Would you like anything to drink?</p>
            <p class="subtitle" style="font-size:12px;color:var(--muted)">不错的选择！那是经典的美式餐点。你想喝点什么吗？</p>
            <div style="margin-top:8px;padding:8px;background:#ECFDF5;border-radius:8px;font-size:12px">
              💡 新词汇：<span style="font-weight:600;color:var(--accent)">classic</span> - 经典的
            </div>
          </div>
        </div>
        <div style="padding:16px;background:var(--card);border-top:2px solid var(--border);display:flex;justify-content:center;align-items:center">
          <button class="btn btn-danger" onclick="go('summary')" style="width:200px;height:56px;font-size:18px;border-radius:28px">🔴 挂断对话</button>
        </div>
      </div>
    `
  },
  summary: {
    html: `
      <div style="padding:40px 24px;flex:1;display:flex;flex-direction:column;align-items:center;text-align:center">
        <div style="font-size:64px;margin-bottom:20px">🎉</div>
        <h1 style="margin-bottom:12px">对话完成！</h1>
        <p style="margin-bottom:32px">本次学习成果</p>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;width:100%;margin-bottom:32px">
          <div class="card" style="margin:0;text-align:center">
            <div style="font-size:32px;font-weight:700;color:var(--primary)">8</div>
            <div style="font-size:14px;color:var(--muted)">新学词汇</div>
          </div>
          <div class="card" style="margin:0;text-align:center">
            <div style="font-size:32px;font-weight:700;color:var(--accent)">12</div>
            <div style="font-size:14px;color:var(--muted)">对话轮次</div>
          </div>
          <div class="card" style="margin:0;text-align:center">
            <div style="font-size:32px;font-weight:700;color:var(--primary)">5</div>
            <div style="font-size:14px;color:var(--muted)">分钟时长</div>
          </div>
          <div class="card" style="margin:0;text-align:center">
            <div style="font-size:32px;font-weight:700;color:var(--accent)">A</div>
            <div style="font-size:14px;color:var(--muted)">发音评分</div>
          </div>
        </div>
        <button class="btn btn-primary" onclick="go('home')" style="width:100%;margin-bottom:12px">返回首页</button>
        <button class="btn btn-secondary" onclick="go('review')" style="width:100%">去复习</button>
      </div>
    `
  },
  review: {
    html: `
      <div style="padding:20px 24px 100px">
        <h1 style="margin-bottom:8px">复习巩固</h1>
        <p style="margin-bottom:24px">温故而知新</p>
        <div class="card" style="margin:0 0 16px 0;cursor:pointer" onclick="go('conversation')">
          <h2>💬 回顾历史对话</h2>
          <p>重新进入之前的对话场景，巩固记忆</p>
          <div style="margin-top:12px;padding:12px;background:var(--bg);border-radius:12px">
            <div style="font-weight:600;color:var(--text);font-size:14px">餐厅点餐练习</div>
            <div style="font-size:12px;color:var(--muted)">昨天 · 15分钟 · 12个词汇</div>
          </div>
          <div style="margin-top:8px;padding:12px;background:var(--bg);border-radius:12px">
            <div style="font-weight:600;color:var(--text);font-size:14px">机场值机对话</div>
            <div style="font-size:12px;color:var(--muted)">3天前 · 20分钟 · 18个词汇</div>
          </div>
        </div>
        <div class="card" style="margin:0 0 16px 0;cursor:pointer" onclick="go('conversation')">
          <h2>📖 单词本复习</h2>
          <p>复习生词本中的词汇，掌握后自动移除</p>
          <div style="margin-top:12px;display:flex;flex-wrap:wrap;gap:8px">
            <span class="badge badge-primary">serendipity</span>
            <span class="badge badge-primary">break a leg</span>
            <span class="badge badge-primary">euphoria</span>
            <span class="badge badge-primary">+12</span>
          </div>
        </div>
      </div>
      ${tabBar('review')}
    `
  },
  analytics: {
    html: `
      <div style="padding:20px 24px 100px">
        <h1 style="margin-bottom:8px">学习分析</h1>
        <p style="margin-bottom:24px">数据统计与进度追踪</p>
        <div class="card" style="margin:0 0 16px 0;background:linear-gradient(135deg,#4F46E5,#818CF8);color:#fff">
          <p style="color:rgba(255,255,255,.8);font-size:14px;margin-bottom:8px">本周学习时长</p>
          <div style="font-size:36px;font-weight:700">2.5<span style="font-size:16px">小时</span></div>
          <p style="color:rgba(255,255,255,.8);font-size:13px;margin-top:8px">较上周增长 40%</p>
        </div>
        <div class="card" style="margin:0 0 16px 0">
          <h2>📈 词汇掌握趋势</h2>
          <div style="display:flex;align-items:flex-end;gap:8px;height:120px;margin-top:16px;padding:0 8px">
            <div style="flex:1;background:var(--border);border-radius:4px;height:40%"></div>
            <div style="flex:1;background:var(--border);border-radius:4px;height:60%"></div>
            <div style="flex:1;background:var(--primary-light);border-radius:4px;height:50%"></div>
            <div style="flex:1;background:var(--primary-light);border-radius:4px;height:75%"></div>
            <div style="flex:1;background:var(--primary);border-radius:4px;height:90%"></div>
            <div style="flex:1;background:var(--primary);border-radius:4px;height:100%"></div>
            <div style="flex:1;background:var(--accent);border-radius:4px;height:85%"></div>
          </div>
          <div style="display:flex;justify-content:space-between;font-size:12px;color:var(--muted);margin-top:8px;padding:0 8px">
            <span>一</span><span>二</span><span>三</span><span>四</span><span>五</span><span>六</span><span>日</span>
          </div>
        </div>
        <div class="card" style="margin:0 0 16px 0">
          <h2>🎯 阶段成果</h2>
          <div style="display:flex;justify-content:space-between;padding:12px 0;border-bottom:1px solid var(--border)">
            <span style="color:var(--muted)">累计掌握词汇</span>
            <span style="font-weight:700;color:var(--text)">156 个</span>
          </div>
          <div style="display:flex;justify-content:space-between;padding:12px 0;border-bottom:1px solid var(--border)">
            <span style="color:var(--muted)">完成对话次数</span>
            <span style="font-weight:700;color:var(--text)">23 次</span>
          </div>
          <div style="display:flex;justify-content:space-between;padding:12px 0">
            <span style="color:var(--muted)">平均发音评分</span>
            <span style="font-weight:700;color:var(--accent)">B+</span>
          </div>
        </div>
        <div class="card" style="margin:0">
          <h2>📅 学习日历</h2>
          <p>连续学习 5 天，继续保持！</p>
        </div>
      </div>
      ${tabBar('analytics')}
    `
  },
  profile: {
    html: `
      <div style="padding:20px 24px 100px">
        <h1 style="margin-bottom:24px">个人设置</h1>
        <div class="card" style="margin:0 0 16px 0">
          <div style="display:flex;align-items:center;gap:16px;margin-bottom:16px">
            <div style="width:64px;height:64px;border-radius:50%;background:var(--primary);display:flex;align-items:center;justify-content:center;color:#fff;font-weight:700;font-size:24px">林</div>
            <div>
              <div style="font-weight:700;color:var(--text);font-size:18px">林小雨</div>
              <div style="font-size:14px;color:var(--muted)">中级水平 · 考试提分</div>
            </div>
          </div>
          <button class="btn btn-secondary" style="width:100%">编辑资料</button>
        </div>
        <div class="card" style="margin:0 0 16px 0">
          <h2>账户余额</h2>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <div>
              <div style="font-size:28px;font-weight:700;color:var(--text)">¥ 25.00</div>
              <div style="font-size:13px;color:var(--muted)">剩余对话时长：50分钟</div>
            </div>
            <button class="btn btn-primary" style="padding:10px 20px">充值</button>
          </div>
        </div>
        <div class="card" style="margin:0 0 16px 0">
          <h2>默认AI老师</h2>
          <div style="display:flex;gap:12px;align-items:center;padding:12px;background:var(--bg);border-radius:12px">
            <div style="width:48px;height:48px;border-radius:50%;background:var(--primary-light);display:flex;align-items:center;justify-content:center;font-size:24px">👩</div>
            <div style="flex:1">
              <div style="font-weight:600;color:var(--text)">Emma</div>
              <div style="font-size:13px;color:var(--muted)">教学型 · 耐心细致</div>
            </div>
            <span style="color:var(--primary);font-weight:600;cursor:pointer">切换</span>
          </div>
        </div>
        <div class="card" style="margin:0">
          <h2>设置</h2>
          <div style="display:flex;justify-content:space-between;align-items:center;padding:12px 0;border-bottom:1px solid var(--border)">
            <span style="color:var(--text)">消息通知</span>
            <span style="color:var(--muted)">开启</span>
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center;padding:12px 0;border-bottom:1px solid var(--border)">
            <span style="color:var(--text)">沉浸式模式默认</span>
            <span style="color:var(--muted)">关闭</span>
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center;padding:12px 0">
            <span style="color:var(--danger)">退出登录</span>
          </div>
        </div>
      </div>
      ${tabBar('profile')}
    `
  }
};

// 底部导航
function tabBar(active) {
  const tabs = [
    {id:'home',icon:'🏠',label:'首页'},
    {id:'review',icon:'📚',label:'复习'},
    {id:'analytics',icon:'📊',label:'分析'},
    {id:'profile',icon:'👤',label:'我的'}
  ];
  return `<div class="tab-bar">${tabs.map(t=>`
    <div class="tab-item ${active===t.id?'active':''}" onclick="go('${t.id}')">
      <span style="font-size:24px">${t.icon}</span>
      <span>${t.label}</span>
    </div>
  `).join('')}</div>`;
}

// 渲染函数
function go(id) {
  document.querySelectorAll('.screen').forEach(s=>{
    s.classList.remove('active','has-tabbar');
  });
  const el = document.getElementById('screen-'+id);
  el.classList.add('active');
  // 如果页面包含tab-bar，添加has-tabbar类
  if(el.querySelector('.tab-bar')){
    el.classList.add('has-tabbar');
  }
}

function toggleSettings() {
  const p = document.getElementById('settings-panel');
  p.style.display = p.style.display==='none'?'flex':'none';
}

function toggleImmersive() {
  document.querySelectorAll('.subtitle').forEach(s=>{
    s.style.display = s.style.display==='none'?'block':'none';
  });
}

// 初始化
function init() {
  const container = document.getElementById('screens');
  Object.entries(screens).forEach(([id,data])=>{
    const div = document.createElement('div');
    div.id = 'screen-'+id;
    div.className = 'screen';
    div.innerHTML = data.html;
    container.appendChild(div);
  });
  go('splash');
}

init();
