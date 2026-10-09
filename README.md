# 食刻校园：校园美食推荐助手

课程原型，使用60条模拟餐饮数据。前端为Vue 3，后端为Spring Boot 4.1.1，默认连接本机MySQL 8.0.36。

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

打开 `http://127.0.0.1:5173/`。后端默认运行在 `http://127.0.0.1:8080/`；前端开发服务器会代理 `/api` 请求。首次运行会在 `campus_food` 中初始化四张表和60条模拟餐饮数据；后端重启不会重复插入。

若本机Maven全局配置指向不可写的仓库，请在Maven命令中传入自己的可写仓库目录或本机settings文件。不要提交本地仓库缓存。

## 数据库配置

本机已创建 `campus_food` 数据库，使用 `utf8mb4` 字符集。应用默认使用MySQL，连接项保存在本机 `backend/.env`，该文件不会被Git跟踪。克隆项目到其他电脑后，可复制[配置示例](backend/.env.example)并填写自己的连接信息。请勿把真实密码提交到Git或公开仓库。

建表语句位于 [schema-mysql.sql](backend/src/main/resources/db/schema-mysql.sql)。当前课程原型使用可重复执行的建表语句和模拟数据初始化；表结构后续变更需要编写数据库迁移，不能直接删除已有数据。

目前接口以 `dining_item` 中的展示字段进行查询。`tag`、`dining_item_tag` 和 `business_hours` 保存同一批模拟数据的结构化映射；若手动修改标签或营业时间，需要同步更新 `dining_item` 中相应的展示字段，避免推荐结果与详情不一致。

如需不连接MySQL、仅临时演示，可使用内存数据库：

```powershell
mvn spring-boot:run '-Dspring-boot.run.profiles=demo'
```

## 验证

```powershell
cd D:\FoodRecommendations\frontend
npm run build
```

```powershell
cd D:\FoodRecommendations\backend
mvn test
```

接口入口：`GET /api/v1/dining-items`、`GET /api/v1/dining-items/{id}`、`POST /api/v1/recommendations`、`GET /api/v1/meta/data-info`。

## 数据声明

全部名称、价格、距离、评分、营业时间和过敏原均为课程模拟数据，不代表真实校园餐饮信息。过敏原信息不能替代现场确认。
