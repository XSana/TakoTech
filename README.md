<div align="center">
  <img alt="logo" src="docs/image/logo.png">
  <h1 align="center" style="margin-top: 0">TakoTech</h1>
</div>

### 介绍

TakoTech(塔可科技)，主要用于改善在与我的小伙伴游玩[GTNH](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack)
时锁遇到的一些问题，随缘更新各种奇怪的东西。

### 目标

- [X] 中文输入修正 ([InputFix](https://github.com/zlainsama/InputFix))
- [X] **矿物存储元件**（灵感来源于[AE2Things](https://github.com/asdflj/AE2Things)，qz-miner太强了得有东西装下他挖的矿）
  - 目标是简化矿物处理以及减少矿典卡使用
  - 近乎无限的矿物存储容量
  - 类型转换系统（可转换为单矿物类型存储且保留原有内容，不匹配的矿物仍可取出）
  - 可配使用单类型原件配合反向卡在原件工作台标记过滤（什么你要写矿典？也不是不行，塞个矿典卡就行）
- [X] 高级工具箱（IC2工具箱的增强版本，在IC2工具箱功能的基础上，增加切换工具箱内的物品到手中，需要设置按键）
- [ ] AE网页下单
- [ ] 加入吉祥物

### 版本支持

> 需要GTNHLib作为前置
>
> 只作为开发使用依赖版本参考，部分版本可能可以跨版本兼容（未测试）

- 0.2.7+ (GTNH274)
- 0.2.1 - 0.2.6 (GTNH273)
- 0.1.0 - 0.2.0 (GTNH272)

### Q&A

> Q: 为什么是Tako是塔可不是章鱼
>
> A: 因为这个名字来自我定制的虚拟角色（女儿），名字Hanami Tako中文名字是塔可，设定上喜欢吃Taco，所以以发音来命名，而不是日语中的タコ。
>
> [详细信息关注我的B站空间](https://space.bilibili.com/7742198?spm_id_from=333.1007.0.0)

### 矿物存储元件使用手册

**这是什么？**
一个能装下qz-miner挖出所有矿物的AE存储原件！

**使用指南：**

- 将元件放入工作台合成即可转换存储类型
- 转换后原有存储内容和升级组件完全保留
- 类型不匹配的矿物：
  - ✓ 可正常取出
  - ✗ 禁止新增存入

**高级技巧：**

- 搭配反向卡实现单物品过滤（无需写矿典）
- 插入矿典卡解锁矿典支持
