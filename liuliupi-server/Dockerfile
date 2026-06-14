#指定基础镜像，在其上进行定制
#FROM reverie/armhf-alpine-oracle-jdk:latest
FROM openjdk:21-jdk-slim

# 安装字体支持：登录图形验证码（Hutool LineCaptcha）依赖 Java AWT 字体子系统，
# 而 openjdk:21-jdk-slim 精简镜像未内置 fontconfig 与字体文件，会导致
# NoClassDefFoundError: Could not initialize class sun.font.SunFontManager
RUN apt-get update && apt-get install -y --no-install-recommends \
    fontconfig \
    libfreetype6 \
    fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

#维护者信息
MAINTAINER yangshare <717449318@qq.com>

ADD target/*.jar liuliupi-server.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "liuliupi-server.jar"]