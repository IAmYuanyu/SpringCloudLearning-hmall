# 本地部署

## 数据库部分

启动Linux虚拟机

将`README`中的`mysql`文件夹复制到虚拟机的`/root`目录。如果`/root`下已经存在`mysql`目录则删除旧的，如果不存在则直接复制本地的：

![img](./README/assets/1765639136799-2.png)

然后创建一个通用网络：

```Bash
docker network create hm-net
```

使用下面的命令来安装MySQL：

```Bash
docker run -d \
  --name mysql \
  -p 3307:3306 \
  -e TZ=Asia/Shanghai \
  -e MYSQL_ROOT_PASSWORD=123 \
  -v /root/mysql/data:/var/lib/mysql \
  -v /root/mysql/conf:/etc/mysql/conf.d \
  -v /root/mysql/init:/docker-entrypoint-initdb.d \
  --network hm-net\
  --restart=always \
  mysql
```

此时，通过命令查看mysql容器：

```Bash
docker ps
```

如图：

![img](./README/assets/1765639136795-1.png)

发现mysql容器正常运行

若没有运行，手动输入`docker start mysql`启动

## 后端部分

修改`\hm-service\src\main\resources\application-local.yaml`中的`host`为自己虚拟机的IP地址

修改`\hm-service\src\main\resources\application.yaml`中的`spring.datasource.url`为自己数据库端口（刚刚创建的是3307端口）

按下`ALT` + `8`键打开services窗口，新增一个启动项：

![img](./README/assets/1765638926569-5.png)

在弹出窗口中鼠标向下滚动，找到`Spring Boot`:

![img](./README/assets/1765638926562-1.png)

点击后应该会在services中出现hmall的启动项：

![img](./README/assets/1765638926562-2.png)

点击对应按钮，即可实现运行或DEBUG运行。

**不过别着急！！**

我们还需要对这个启动项做简单配置，在`HMallApplication`上点击鼠标右键，会弹出窗口，然后选择`Edit Configuration`：

![img](./README/assets/1765638926562-3.png)

在弹出窗口中配置SpringBoot的启动环境为local：

![img](./README/assets/1765638926562-4.png)

点击OK配置完成。接下来就可以运行了！

启动完成后，可以访问 http://localhost:8080/hi 测试

> 项目JDK版本为11



# 部署到云服务器

后续内容中的`106.14.144.133`是我的服务器公网ip，部署时改成自己的

把这些东西部署到云服务器的**前提是实现了内网穿透**，否则使用过程中会出现互相访问不到的情况，我的博客有说明如何使用frp实现内网穿透，嫌麻烦直接部署本地即可

**建议服务器内存至少4G**，我是分别部署在2G+4G的两台服务器中，前端、Sentinel和内网穿透使用2G服务器，其余中间件部署到4G服务器

我的博客：[缘鱼](https://iamyuanyu.github.io/)

## 部署前端

把`conf/nginx.conf`如下编辑：

~~~nginx
worker_processes  1;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/json;

    sendfile        on;
    
    keepalive_timeout  65;

     server {
        listen       80;
        server_name  106.14.144.133;
        # 指定前端项目所在的位置
        location / {
            root /usr/share/nginx/html;
        }

        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   /usr/share/nginx/html;
        }
    }
    server {
        listen       18080;
        server_name  106.14.144.133;
        # 指定前端项目所在的位置
        location / {
            root /usr/share/nginx/html/hmall-portal;
        }

        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   /usr/share/nginx/html;
        }
        location /api {
            rewrite /api/(.*)  /$1 break;
            proxy_pass http://106.14.144.133:8080;
        }
    }
    server {
        listen       18081;
        server_name  106.14.144.133;
        # 指定前端项目所在的位置
        location / {
            root /usr/share/nginx/html/hmall-admin;
        }

        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   /usr/share/nginx/html;
        }
        location /api {
            rewrite /api/(.*)  /$1 break;
            proxy_pass http://106.14.144.133:8080;
        }
    }
    server {
        listen       18082;
        server_name  106.14.144.133;
        # 指定前端项目所在的位置
        location / {
            root /usr/share/nginx/html/hm-refresh-admin;
        }

        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   /usr/share/nginx/html;
        }
        location /api {
             proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Real-PORT $remote_port;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            rewrite /api/(.*)  /$1 break;
            proxy_pass http://106.14.144.133:8080;
        }
    }
}

~~~

然后将`conf/nginx.conf`文件和`html`文件夹放到Linux中的`/root/hmall-nginx`目录（没有就创建）

然后在Linux中执行下方命令：

~~~shell
docker run -d \
  --name hmall-nginx \
  -p 18080:18080 \
  -v /root/hmall-nginx/html:/usr/share/nginx/html \
  -v /root/hmall-nginx/nginx.conf:/etc/nginx/nginx.conf \
  nginx:latest
~~~

> 如果使用frp进行内网穿透，18080端口可能被占用，可以写成`-p 18081:18080`

## 部署Sentinel

将Sentinel的jar包发送到服务器的`/root/sentinel/`目录中，确保jar包名字为`sentinel-dashboard.jar`

~~~shell
docker run -d \
  --name sentinel-dashboard \
  -p 8090:8090 \
  -v /root/sentinel/sentinel-dashboard.jar:/app/sentinel-dashboard.jar \
  openjdk:8-jre \
  java -Dserver.port=8090 \
       -Dcsp.sentinel.dashboard.server=106.14.144.133:8090 \
       -Dcsp.sentinel.client.ip=106.14.144.133  # 指定客户端ip
       -Dproject.name=sentinel-dashboard \
       -jar /app/sentinel-dashboard.jar
~~~



## 后续操作前提

在docker中创建一个网络

```shell
docker network create hm-net
```

## 部署mysql

将资料的mysql目录复制到服务器的/root中

部署mysql

~~~shell
docker run -d \
  --name mysql \
  -p 3307:3306 \
  -e TZ=Asia/Shanghai \
  -e MYSQL_ROOT_PASSWORD=123 \
  -v /root/mysql/data:/var/lib/mysql \
  -v /root/mysql/conf:/etc/mysql/conf.d \
  -v /root/mysql/init:/docker-entrypoint-initdb.d \
  --network hm-net \
  --restart=always \
  --memory=512m \
  --memory-swap=512m \
  mysql
~~~

## 部署nacos

在服务器的/root/nacos下创建一个custom.env文件，内容为

MYSQL_SERVICE_HOST改为mysql所在地址，其他看情况改

~~~properties
PREFER_HOST_MODE=hostname
MODE=standalone
SPRING_DATASOURCE_PLATFORM=mysql
MYSQL_SERVICE_HOST=mysql的地址
MYSQL_SERVICE_DB_NAME=nacos
MYSQL_SERVICE_PORT=3307
MYSQL_SERVICE_USER=root
MYSQL_SERVICE_PASSWORD=123
MYSQL_SERVICE_DB_PARAM=characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
~~~

部署nacos

~~~shell
docker run -d \
  --name nacos \
  --env-file ./nacos/custom.env \
  -p 8848:8848 \
  -p 9848:9848 \
  -p 9849:9849 \
  -e TZ=Asia/Shanghai \
  -e MODE=standalone \
  -e JVM_XMS=256m \
  -e JVM_XMX=512m \
  -e JVM_XMN=128m \
  -e JVM_MS=64m \
  -e JVM_MMS=128m \
  -v /root/nacos/logs:/home/nacos/logs \
  --network hm-net \
  --memory=768m \
  --memory-swap=768m \
  --restart=always \
  nacos/nacos-server:v2.1.0-slim
~~~









