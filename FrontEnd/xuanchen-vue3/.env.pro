NODE_ENV = "production"
# 生产环境只允许同源相对地址，禁止写死任何主机名（原先写死 localhost 必然部署失败）。
# 部署时由 Nginx/网关把 /api 反代到后端并去掉前缀，参考：
#   location /api/ {
#       proxy_pass http://后端地址:8080/;
#       proxy_set_header Host $host;
#       proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
#   }
#   location /api/ws {
#       proxy_pass http://后端地址:8080/ws;
#       proxy_http_version 1.1;
#       proxy_set_header Upgrade $http_upgrade;
#       proxy_set_header Connection "upgrade";
#   }
# 如采用独立 API 域名部署，构建时覆盖为该域名即可（如 https://api.example.com）
APP_BASE_URL = "/api"
# 后端相对路径（语义同 .env.dev）
APP_FILE_UPLOAD_PATH = "/filemanage/upload"
APP_FILE_VIEW_PATH = "/filemanage/static/"
