# 使用官方 Nginx 镜像作为基础镜像
FROM nginx:latest

# 将本地构建好的静态资源复制到容器的 Nginx 默认发布目录
# 注意：IDEA 构建时 contextFolderPath = liuliupi-ui/dist，所以 . 就是 dist 的内容
COPY . /usr/share/nginx/html

# 启动 Nginx 服务
CMD ["nginx", "-g", "daemon off;"]
