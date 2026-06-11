#指定基础镜像，在其上进行定制
#FROM reverie/armhf-alpine-oracle-jdk:latest
FROM openjdk:21-jdk-slim

#维护者信息
MAINTAINER yangshare <717449318@qq.com>

ADD target/*.jar liuliupi-server.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "liuliupi-server.jar"]