# rest_face


### JAVA环境安装
#### window（开发）环境安装
JRE： Java Runtime EnvironmentJDK：Java Development Kit JRE顾名思义是java运行时环境，包含了java虚拟机，java基础类库。是使用java语言编写的程序运行所需要的软件环境，是提供给想运行java程序的用户使用的。
JDK顾名思义是java开发工具包，是程序员使用java语言编写java程序所需的开发工具包，是提供给程序员使用的。JDK包含了JRE，同时还包含了编译java源码的编译器javac，还包含了很多java程序调试和分析的工具：jconsole，jvisualvm等工具软件，还包含了java程序编写所需的文档和demo例子程序。
如果你需要运行java程序，只需安装JRE就可以了。如果你需要编写java程序，需要安装JDK。JRE根据不同操作系统（如：windows，linux等）和不同JRE提供商（IBM,ORACLE等）有很多版本，最常用的是Oracle公司收购SUN公司的JRE版本。如果你想查看更官方的解释，可以前往Oracle官网：http://www.oracle.com/cn/technologies/java/overview/index.html

IDE用的是 IntelliJ IDEA

JDK版本选择：1.9的jdk我还是有点虚，所以用的是1.8的，jdk-8u161-windows-x64
[JDK和Tomcat安装教程](http://blog.csdn.net/qq_32519693/article/details/71330930)
设置JDK和JRE的环境变量时并没有完全按照这个给的思路，而是按照基本的想法，把JDK和JRE安装的目录的bin给了环境变量，JRE没有用JDK里的，因为Tomcat在安装的是偶用的是下面这个
C:\Program Files\Java\jdk1.8.0_161\bin
C:\Program Files\Java\jre1.8.0_161\bin

Tomcat版本：apache-tomcat-8.5.29，我直接下载的安装包，安装完成后就可以登陆到本地了
Tomcat8只支持jdk1.7及1.7以上的jdk，否则报java.lang.UnsupportedClassVersionError：Unsupported major.minor version 51.0。

maven是JAVA工程的构建工具，下载二进制文件包，解压，添加bin到环境变量，[教程](http://blog.csdn.net/xyang81/article/details/51487939)教程中的步骤5没有做

Jersey 版本：JAX-RS 2.1 / Jersey 2.26+ （但是事实上我没有用到这个，spring boot自带）

spring boot 感觉是和上面体系完全不同的内容，spring boot 自带了Tomcat的支持
Spring boot：spring-boot-cli-2.0.0.RELEASE-bin.zip，[spring boot的hello world](http://blog.csdn.net/liumiaocn/article/details/53431149)，将zip下载后bin添加到环境变量中
Jersey Spring Boot ~~这个方向还没有走通~~**如果设置系统变量后，记得重启cmd**，安装是安装好了，下一步卡住了，改用书中的方法
>至于jersey和spring-mvc能不能混用，这个你写个小demo测试一下就行了。但我不建议混用，首先这样会导致代码混乱，其次要加拦截器的话只能依赖servlet-api的过滤器了，否则你只能两边各写一套。 (感觉jersey的代码可能比较特殊)

