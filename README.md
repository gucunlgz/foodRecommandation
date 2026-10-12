# 食刻校园：校园美食推荐助手

课程原型，使用60条模拟商家／窗口数据及每家多道模拟菜品。前端为Vue 3，后端为Spring Boot 4.1.1，默认连接本机MySQL 8.0.36。

## 运行项目

环境：Java 21、Maven 3.9、Node.js 24、npm 11。

分别打开两个终端：

```powershell
cd D:\FoodRecommendations\backend
mvn spring-boot:run
```

```powershell
cd D:\FoodRecommendations\frontend
npm install
npm run dev
```

打开 `http://127.0.0.1:5173/`。后端默认运行在 `http://127.0.0.1:8080/`；前端开发服务器会代理 `/api` 请求。首次运行会在 `campus_food` 中初始化五张表、60条模拟商家记录及多道模拟菜品；后端重启不会重复插入。已有V1数据库会保留原商家数据并补建菜单表。

若本机Maven全局配置指向不可写的仓库，请在Maven命令中传入自己的可写仓库目录或本机settings文件。不要提交本地仓库缓存。

## 数据库配置

本机已创建 `campus_food` 数据库，使用 `utf8mb4` 字符集。应用默认使用MySQL，连接项保存在本机 `backend/.env`，该文件不会被Git跟踪。克隆项目到其他电脑后，可复制[配置示例](backend/.env.example)并填写自己的连接信息。请勿把真实密码提交到Git或公开仓库。

建表语句位于 [schema-mysql.sql](backend/src/main/resources/db/schema-mysql.sql)。当前课程原型使用可重复执行的建表语句和模拟数据初始化；表结构后续变更需要编写数据库迁移，不能直接删除已有数据。

`dining_item` 保存商家／窗口信息；`dish_item` 通过 `dining_item_id` 保存每道菜的价格、口味、饮食标签和过敏原。搜索、预算及忌口筛选以菜品字段为准，距离、评分、营业时间以商家字段为准。`tag`、`dining_item_tag` 和 `business_hours` 仍保存原商家数据的结构化映射；当前接口读取 `dining_item` 的商家展示字段，若手动修改这些辅助表，仍需同步更新对应商家字段。

搜索支持“全部结果／找菜品／找商家”：菜品结果逐道展示，商家结果每家展示一条并标出匹配菜品。“面条”可匹配标记为面条类的模拟菜品；少量常见别名由显式规则处理，不会生成数据中不存在的菜品。

如需不连接MySQL、仅临时演示，可使用内存数据库：

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

## 验证

```powershell
cd D:\FoodRecommendations\frontend
npm run build
npm test
```

```powershell
cd D:\FoodRecommendations\backend
mvn test
```

接口入口：`GET /api/v1/dining-items`（可传 `keyword`、`searchScope=all|dish|venue` 及筛选条件）、`GET /api/v1/dining-items/{id}`（含菜单）、`POST /api/v1/recommendations`、`GET /api/v1/meta/data-info`。

## 数据声明

全部名称、价格、距离、评分、营业时间和过敏原均为课程模拟数据，不代表真实校园餐饮信息。过敏原信息不能替代现场确认。
