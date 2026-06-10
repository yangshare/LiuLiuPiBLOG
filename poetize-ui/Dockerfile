# 使用官方Nginx镜像作为基础镜像
FROM nginx:latest

# 将本地构建好的静态资源复制到容器的Nginx默认发布目录
COPY . /usr/share/nginx/html

# 启动Nginx服务
CMD ["nginx", "-g", "daemon off;"]
