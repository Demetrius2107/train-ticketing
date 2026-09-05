# web

购票前端（Vue 3 + Ant Design Vue），包管理统一使用 **npm**（`package-lock.json` 为准，勿引入 yarn.lock）。

## Project setup
```
npm install
```

### Compiles and hot-reloads for development
```
npm run dev
```

### Compiles and minifies for production
```
npm run build
```

### Lints and fixes files
```
npm run lint
```

### 本地联调
`vue.config.js` 已把 `/member`、`/business` 代理到网关 `http://localhost:8000`，
先启动后端（docker compose 或本地 mvn），登录后即可走购票流程。
演示数据：仓库根目录执行 `python script/seed-demo.py`。

### Customize configuration
See [Configuration Reference](https://cli.vuejs.org/config/).
