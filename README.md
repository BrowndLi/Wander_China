# 游迹中国 Wander China

按热度、著名度、好玩度推荐中国城市景点的旅行应用。

- 手机版网站：https://browndli.github.io/Wander_China/ ，文件在 `gh-pages` 分支
- 安卓安装包：https://github.com/BrowndLi/Wander_China/releases/latest/download/WanderChina.apk

## main 分支

这里是安卓应用的外壳工程。应用用系统网页组件打开手机版网站，并在安装包里带一份离线备份，第一次打开就算没有网络也能使用。

每次推送到 main 分支，GitHub Actions 会自动：

1. 构建安装包，并放入最新的离线备份页面
2. 在安卓模拟器里安装运行，分别在有网络和断网时截图，检查是否崩溃
3. 测试通过后发布到 Releases

也可以在仓库的 Actions 页面手动运行“构建安卓安装包”。

网站内容更新时不需要重新构建安装包，应用联网后会自动获取最新版本。
