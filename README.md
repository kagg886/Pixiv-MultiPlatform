# Pixiv-MultiPlatform

这是一个基于`Kotlin MultiPlatform Compose`而发布的，支持`Android`，`Desktop(Windows,Linux)`两端三平台的**第三方**Pixiv客户端。

![630c53942ab78d98e13d0ac9e566211c](./README.assets/630c53942ab78d98e13d0ac9e566211c.png)

## 一. 下载链接

[![release](https://img.shields.io/github/v/release/kagg886/Pixiv-MultiPlatform)](https://github.com/kagg886/Pixiv-MultiPlatform/releases/latest)

## 三. 功能列表

> 打勾的为已实现，未打钩的则在将来的版本进行实现
>
> 欢迎issue以提供好点子，好的点子将会写入下方的**TODO**表格中待实现哦

- [x] 通过内置浏览器进行登录
- [x] 首页
  - [x] 推荐
    - [x] 查看推荐插画
    - [x] 查看推荐小说
  - [x] 插画排行
    - [x] 日榜
    - [x] 周榜
    - [x] 月榜
    - [x] 男性
    - [x] 女性
    - [x] 原创
    - [x] 新人
  - [x] 动态
    - [x] 查看收藏插画
    - [x] 查看最新插画
- [x] 搜索
  - [x] 热门tag
  - [x] 搜索建议
  - [x] 标题匹配模式
  - [x] 排序方式(热度排序，时间降序，时间升序)
  - [x] 搜索结果
    - [x] 插画
    - [x] 小说
  - [x] 根据id猜测搜索结果(插画/小说/作者)
- [ ] 插画详情
  - [x] 基本预览图
  - [ ] 原图展示
  - [x] 下载原图
  - [x] 收藏插画
  - [x] 点赞
  - [x] 查看评论
  - [x] 查看回复
  - [x] 回复插画
  - [ ] 回复评论
- [ ] 小说详情
  - [x] 基本阅读器
    - [x] 页数跳转
    - [x] 链接支持
  - [ ] 导出小说为png格式
  - [ ] 评论
    - [x] 查看评论
    - [x] 查看回复
    - [x] 回复小说
    - [ ] 回复评论
- [x] 个人中心
  - [x] 查看资料
  - [x] 修改资料
  - [x] 历史记录
  - [x] 下载管理
  - [x] 退出登录

- [ ] 其他功能
  - [x] 无效插画过滤(R18，删除插画，无权限查看插画)
  - [x] 使用DoH实现直连
  - [x] 自定义pixiv-image代理
  - [ ] 自定义TAG过滤
  - [ ] 国际化