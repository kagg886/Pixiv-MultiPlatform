> English Version is [→HERE](./README_EN.md)

---

<div align="center">
    <img src=".idea/icon.png"/>
    <h1>Pixiv-MultiPlatform</h1>
    <p>基于Kotlin技术栈的全平台的第三方pixiv客户端</p>
    <table>
        <thead align="center">
            <tr>
                <th>License</th>
                <th>下载数</th>
                <th>issue</th>
                <th>最后提交</th>
            </tr>
        </thead>
        <tbody align="center">
            <tr>
                <td>
                    <img src="https://img.shields.io/github/license/kagg886/Pixiv-MultiPlatform" alt="" srcset="">
                </td>
                <td>
                    <img src="https://img.shields.io/github/downloads/kagg886/Pixiv-MultiPlatform/total" alt=""
                        srcset="">
                </td>
                <td>
                    <img src="https://img.shields.io/github/issues/kagg886/Pixiv-MultiPlatform" alt="" srcset="">
                    <img src="https://img.shields.io/github/issues-closed/kagg886/Pixiv-MultiPlatform" alt="">
                </td>
                <th>
                    <img src="https://img.shields.io/github/last-commit/kagg886/Pixiv-MultiPlatform" alt="" srcset="">
                </th>
            </tr>
        </tbody>
    </table>
    <img src="./README.assets/630c53942ab78d98e13d0ac9e566211c.png" />
</div>

> 在新版本发布前，iOS的ipa文件请前往 workflow 处下载。

## 一. 下载链接

**戳此处进行下载-->** [![release](https://img.shields.io/github/v/release/kagg886/Pixiv-MultiPlatform)](https://github.com/kagg886/Pixiv-MultiPlatform/releases/latest) [![F-Droid](https://img.shields.io/f-droid/v/top.kagg886.pmf.svg?logo=F-Droid)](https://f-droid.org/packages/top.kagg886.pmf/) 

## 二. 功能截图(宽窄屏配色不同的原因是主题不同)

| 功能    | Windows Linux macOS UI                                                  | Android, iOS UI                                                         |
|-------|-------------------------------------------------------------------------|-------------------------------------------------------------------------|
| 插画流展示 | ![image-20250404104056471](./README.assets/image-20250404104056471.png) | ![image-20250114224028350](./README.assets/image-20250114224028350.png) |
| 搜索栏   | ![image-20250114224115785](./README.assets/image-20250114224115785.png) | ![image-20250114224144449](./README.assets/image-20250114224144449.png) |
| 图片详情  | ![image-20250114224256063](./README.assets/image-20250114224256063.png) | ![image-20250114224309471](./README.assets/image-20250114224309471.png) |
| 小说阅读  | ![image-20250114224427836](./README.assets/image-20250114224427836.png) | ![image-20250114224459952](./README.assets/image-20250114224459952.png) |



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
    - [x] 查看关注者的最新插画
    - [x] 查看全站的最新插画
- [x] 搜索
  - [x] 热门tag
  - [x] 搜索建议
  - [x] 标题匹配模式
  - [x] 排序方式(热度排序，时间降序，时间升序)
  - [x] 搜索结果
    - [x] 插画
    - [x] 小说
    - [ ] 小说系列
    - [x] 作者
  - [x] 根据id猜测搜索结果(插画/小说/作者)
- [ ] 插画详情
  - [x] 基本预览图
  - [ ] 原图展示
  - [x] 下载原图
  - [x] 收藏插画
    - [x] 按TAG分类收藏
    - [x] 指定私有收藏
  - [x] 点赞
  - [x] 查看评论
  - [x] 查看回复
  - [x] 回复插画
  - [x] 回复评论
- [x] 小说详情
  - [x] 基本阅读器
    - [x] 页数跳转
    - [x] 链接支持
    - [x] 内联图片
  - [x] 导出小说为epub格式
  - [x] 收藏小说
    - [x] 按TAG分类收藏
    - [x] 指定私有收藏
  - [x] 查看小说系列
  - [x] 评论
    - [x] 查看评论
    - [x] 查看回复
    - [x] 回复小说
    - [x] 回复评论
- [ ] 个人中心
  - [x] 查看资料
  - [ ] 修改资料
  - [x] 查看收藏
    - [x] 查看公开收藏
    - [x] 查看私有收藏
    - [x] 按TAG筛选收藏
  - [ ] 查看关注
  - [x] 历史记录
  - [x] 下载管理
  - [x] 退出登录
- [x] 其他功能
  
  - [x] PC端快捷键支持(自V1.6.0)
  
    | 快捷键 | 作用                                           |
    | ------ | ---------------------------------------------- |
    | ↑      | 向上滚动                                       |
    | ↓      | 向下滚动                                       |
    | PgDn   | 向下滚动一页                                   |
    | R      | 在页面顶部时刷新页面，非页面顶部则回到页面顶部 |
    | ←      | 含Tab页面用于向左切换Tab(例如推荐页面)         |
    | →      | 含Tab页面用于向右切换Tab(例如推荐页面)         |
  - [x] 无效插画/小说过滤(删除插画，无权限查看插画)
  - [x] 手动插画/小说过滤(R18，R18G，AI)
  - [x] 屏蔽TAG过长小说
  - [x] 屏蔽正文过短小说
  - [x] 使用DoH实现直连
  - [ ] 自定义TAG过滤
  - [ ] 国际化
