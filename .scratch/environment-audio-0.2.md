# Gate Rush 0.2 / design revision 13

用户要求：周围看起来不要空，加入简单天空盒或环境；补充音效，音效不能尖锐。

- Full stage；全天球浅蓝与奶油色云层贴图，低饱和地面、18 个圆润远山和 10 组玩具花园。环境位于 placementOwner 下，使用米制，保持原 fittedSceneRoot 与赛道尺寸。
- 天球材质使用 UnlitMaterial、TextureResource 和 FRONT culling，沿用 Wasteland 已验证实现。简单环境由几何与程序贴图组成，无新增生成模型。
- 九段本地 mono PCM WAV：射击、命中、破门、敌人死亡、爆炸、拾取、护盾、受伤、通关。约 1.15 kHz 低通、12 ms 渐入、40 ms 渐出，无金属尖响；素材峰值最高 0.65。
- 射击增益 0.20；主要反馈 0.32–0.40。SoundPool 最多八路，按素材峰值控制叠加余量至 0.80，命中间隔 140 ms，重要反馈短时压低射击声；多发子弹一轮只触发一次射击音效，同次拾取与奖励去重。
- 音效只随现有游戏状态触发。暂停、规则页、离开前台、Stage 失焦和释放场景立即停止所有活动声音；继续游戏不恢复旧的短音效。Stage 通过公开 SpatialContainerStateObserver 监听焦点；PICO Home 菜单只改变容器焦点而不触发 Android ON_PAUSE，因此失焦也暂停游戏，返回后需手动继续。
- 运行证明与频谱分析保存在本轮临时目录，实际佩戴观感与听感由用户验证。

验证：最终源码完成 Gradle 编译和 22 项 JVM 测试；天空、场景物件和九种 WAV 全部在真机加载。2026-11-06 17:17:07.359 的 Stage 失焦事件同时记录 AUDIO_ACTIVE=false，17:17:07.825 重新聚焦后没有重新激活声音，游戏保持暂停。

稳定性限制：本轮出现系统 Runtime GPU_NO_RETIRE，既有游戏会话 2D 线程事件，也有已通过 Android events 追溯到 com.picoxr.systemui 的 2D 线程事件；中间相同 APK 64 秒运行未复现。保留全部失败证据，不将过程记为全量稳定性通过，不声称根因或已修复系统故障。
