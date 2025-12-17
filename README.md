# 数据库部分

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

# 后端部分

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