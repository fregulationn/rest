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


----
[百度人脸服务api](http://ai.baidu.com/docs#/Face-Java-SDK/top)（接口的主要参考）（蓝色字体为引用的链接）


#### 接口明确，接收内容转换，字段更改
更改后的rest服务的接口和路径和原来的有差别
路径： `http://127.0.0.1:5000/rest/2.0/face/v2/faceset/user/add`
curl调用接口格式：`curl -i -k  "http://127.0.0.1:5000/rest/2.0/face/v2/faceset/user/add" --data "uid=test_user_5&user_info=userInfo5&group_id=test_group_2" -H 'Content-Type:application/x-www-form-urlencoded''`

python 调试接口：百度提供的代码是python2的，需要改成python3，[python3中没有urllib2库，被整合了](https://blog.csdn.net/drdairen/article/details/51149498)，同时需要[编码](https://stackoverflow.com/questions/30760728/python-3-urllib-produces-typeerror-post-data-should-be-bytes-or-an-iterable-of)encode("utf-8")

根据：[curl网站开发指南](http://www.ruanyifeng.com/blog/2011/09/curl.html)，data中的内容事实上就是表单上传的内容，[更全的curl命令](http://blog.51cto.com/zhengmingjing/1900718)，[获取url请求参数也就是表单参数的方法](https://blog.csdn.net/yalishadaa/article/details/68937141)。
BUG：[curl:<1> Protocol "'http" not supported or disabled in libcurl](https://blog.csdn.net/whq19890827/article/details/78252086)
参照百度人脸服务api中java的代码对接收到的图片字符串参数进行转码，转码后得到bytes数组，[通过时间戳生成文件名](https://blog.csdn.net/u011403672/article/details/47606901)，[将bytes数组转换为图片并保存到本地](https://blog.csdn.net/psyixiao/article/details/7444792)

返回json，多个可用list替代，单个用map，然后Object可以用来替代任何类型，[示例](https://blog.csdn.net/winfredzen/article/details/53605976)，直接返回就可以，会自动转换，同时，为了处理不同系统之间的关系，将model放到当前文件夹下，并由绝对路径变成相对路径，同时，如果注册的图片的文件夹不存在会创建文件夹另存图片

[可选参数的解决：](https://blog.csdn.net/yizdream/article/details/469654)request中的内容可以被映射为map，但是很奇怪的是如果是int值的话得到的是字符串的名字，于是我利用map来判断存在，再直接通过request得到


#### 录入服务（人脸注册）接口
用于从人脸库中新增用户，可以设定多个用户所在组，及组内用户的人脸图片，
典型应用场景：构建您的人脸库，如会员人脸注册，已有用户补全人脸信息等



层级关系：
```
|- 人脸库(appid)
   |- 用户组一（group_id）
      |- 用户01（uid）
         |- 人脸（faceid）
      |- 用户02（uid）
         |- 人脸（faceid）
         |- 人脸（faceid）
         ....
       ....
   |- 用户组二（group_id）
   |- 用户组三（group_id）
   ....
```
在这里，可以认为用户和他的图片生成的特征是一对多关系，一个用户，可以有多个特征，但是一个特征，只可以对应一用户，就可以通过这个用户，来找到ta的所有特征，也可以通过特征来找到它的用户，这就是关系的定义（特征就是人脸）
appid有点像在group id外面再铺了一层，如果一个uid存在于多个用户组内，将会同时将从各个组中把用户删除，所以可以认为是用户和组是多对多关系，一个用户在多个组中，一个组有多个用户

**数据库表：**
三个表：
**group table**
id 
group_id
users（一个组有多个用户）

**uer table**
id
uid
user_info
group （一个user 可能在多个组）
feture（一个用户对应多个face）


**face table**
id
faceid(好像并没有用)
user（一个face对应一个用户）
image
feature

数据库选择：
[mysql 安装](https://blog.csdn.net/qq_34531925/article/details/78022905)，社区版免费
3306 root，MYSQL 8版本以上提供了新的连接方式，加密性更强，用navicat for mysql 连接就会出问题，error 2059，最好使用低版本的

[float 数组的存储](https://stackoverflow.com/questions/3106548/blob-vs-varchar-for-storing-arrays-in-a-mysql-table)，[blob和java jpa](https://stackoverflow.com/questions/5031585/how-to-write-java-sql-blob-to-jpa-entity)
[数据库图片的存储方式](https://www.cnblogs.com/wangtao_20/p/3440570.html)：比较合理的方式是把图片存到本地，然后存路径到数据库中，并发压力小

Java中对数据库的操作：
[spring boot jpa实现](https://www.cnblogs.com/chenpi/p/6357527.html#_label3)：这个实现是比较完整和合理的，但是它的配置并不合理，[配置参照](https://blog.csdn.net/hguisu/article/details/50977180)，其他方面的对照：[spring boot jersey jpa](https://blog.csdn.net/xiongpei00/article/details/76576420)，[spring boot jpa GitHub](https://github.com/spring-projects/spring-data-jpa)
数据库字段命名的时候还是不要加下划线了，会出问题，找不到
[Spring Boot异常：BeanCreationException:Injection of autowired dependencies failed;](https://blog.csdn.net/qyddle/article/details/69226912)
MySQL的版本对连接有影响：`spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL5Dialect`Mysql8是不行的，必须得5的版本才可以

[JPA(Java Persistence API)是Sun官方提出的Java持久化规范。它为Java开发人员提供了一种对象/关联映射工具来管理Java应用中的关系数据。](https://www.cnblogs.com/ityouknow/p/5891443.html)
[spring boot jpa 多对多实例(如何添加新的数据关系)](https://www.callicoder.com/hibernate-spring-boot-jpa-many-to-many-mapping-example/)
[spring boot jpa 实现(详细参考的这个)](https://blog.csdn.net/lewis_007/article/details/53006602#t4)
[sping boot jpa sqlite(上面都是MySQL)](https://blog.csdn.net/tianyaleixiaowu/article/details/79445561)，[GitHub上的例子](https://github.com/bharat0126/springboot-sqlite-app)
[mybatis水平扩展缓解访问压力](https://my.oschina.net/feinik/blog/879266)

BUG：`org.springframework.dao.DataIntegrityViolationException","message":"could not execute statement; SQL [n/a]; nested exception is org.hibernate.exception.DataException: could not execute statement"`
上面的错误报了两次：（但是在调试的时候真正的错误并不是上面的原因，而是打印的信息会报数据库有关的错误） 
1. 本来打算使用字符串保存特征结果，但是各种方法都没有用，总是存不进去，因为有一万字节的长度，于是更改为blob格式，[blob格式长度](https://stackoverflow.com/questions/21522875/data-truncation-data-too-long-for-column-logo-at-row-1) 
2. 更改后直接插入，但是表是没有更新的，所以有问题，于是删除表后重新插入成功 

BUG：调用`UserRepository`总是报为空的错，[@Autowired是一种函数，可以对成员变量、方法和构造函数进行标注，来完成自动装配的工作](https://www.zhihu.com/question/39356740)  
BUG：数据库连接和调整是在一开始就做了的，如果有问题会在一开始的一长串东西中报错，[Mysql中有些关键字不能用](https://dba.stackexchange.com/questions/123305/mysqlsyntaxerrorexception-you-have-an-error-in-your-sql-syntax)，[Group也包括在里面](http://zhaozhi-1983.iteye.com/blog/159044)，我改名为zu   
BUG：[Solve “failed to lazily initialize a collection of role” exception](https://stackoverflow.com/questions/11746499/solve-failed-to-lazily-initialize-a-collection-of-role-exception)   
BUG：[多对多的关系需要在两边都add才会将关系存储到数据库中](https://www.callicoder.com/hibernate-spring-boot-jpa-many-to-many-mapping-example/)   
BUG：多对多关系，如果已经存在，再重复插入到数据库的话会报错，[解决方法](https://stackoverflow.com/questions/41899628/jpa-hibernate-multiple-representations-of-the-same-entity)，查找关系，然后通过对比组的名字来判断关系是否已经被添加过，直接使用contains函数来判断，因为hashcode不同，程序不会认为即使两个信息一样的组事实上是一个组   
BUG：[detached entity passed to persist](https://stackoverflow.com/questions/13370221/jpa-hibernate-detached-entity-passed-to-persist)   
BUG：[object references an unsaved transient instance](http://aniyo.iteye.com/blog/2388671)，这个错误产生的原因是，级联保存的时候，Zu对象可能并没有在数据库中，这个时候保存User，系统不知道如何处理，解决办法：如果是new的Zu提前保存一下   

java jpa 数据库调用逻辑：定义实体类，定义完之后有的比较简单的就直接DAO，有的会使用service来调用DAO，\***Repository可以将函数语句转换为数据库查询或者设置的语句，也就是jpa的作用，[jpa函数](https://www.cnblogs.com/dreamroute/p/5173896.html)，[jpa 有关命令和函数集合很清晰](https://www.jianshu.com/p/ff4839931c54)   

一对多或者多对多关系的时候，设定的时候，给构造函数中添加对`set`或者`List`结构的初始化，多的那一方以set的形式存在的，被转换到表中 

注册时判断组和用户关系的思路：首先判断用户是否在表中存在，如果存在就取出来，不存在新建，然后判断用户是否和组建立关系，如果有，取出对应的组，结束，如果没有，找对应组名的组是否存在，如果存在就取出来，不存在新建，然后添加关系，结束 


#### 人脸检测接口
faster_rcnn我用来测试的图片结果不是很好，于是换成mobilenet 
返回的结果是浮点百分数，ymin, xmin, ymax, xmax as relative to the image，就是这个顺序，调试代码确认了这个结果，是以左上角为基准计算的 

人脸的图片是直接int值扔到网络里跑的同时不用注册，接口实现按理来说直接给int值而不是给图片会更快，所以这一步可以直接尝试用二进制数组，其实就少了一步二进制存为图片，再取图片，而是直接将二进制读为图片，[byte array  to BufferedImage ](https://blog.csdn.net/feiyangchengjian/article/details/7082130)，BUG：[coordinates out of bound:bufferedimage](https://stackoverflow.com/questions/25610291/coordinates-out-of-boundbufferedimage) 
[一次返回多个结果处理](https://blog.csdn.net/liuyan20062010/article/details/79670479) 

java中byte类型是有符号的，所以二进制值会为负的，[转换为正的int](https://blog.csdn.net/zdy10326621/article/details/50236529)，Java中的一个byte，其范围是-128~127的，之前写mtcnn的时候没有注意到这个问题是因为所有的数据本来都比较小，看不出来 


#### M:N 识别batch模式接口 
与百度api不同的地方在于，**为提高GPU的使用率，一次处理多张图片** 

参数理解：detect_top\_num，每张图片检测的人脸数量，默认为一（检测返回的结果是我觉得是排序好的，所以取设定的前几个做特征处理即可） 
user_top_num，对比结果，每个人脸与注册中特征对比得到分数最高的几个 
最终返回的个数是小于等于 “实际检测到的人脸数” * “每个人脸匹配的top人数”，可以理解为可能人脸检测的数量没有那么多 

BUG：[in Python 3, there's no implicit conversion between unicode (str) objects and bytes objects](https://stackoverflow.com/questions/21916888/cant-concat-bytes-to-str)，所以img1+b','+img2 
BUG：[python 字符串不能以\结尾](https://stackoverflow.com/questions/36519159/python-string-concatenation-append-variable-to-path-what-is-the-correct-syntax-m)，如果是路径的话正确的打开方式应该是用join 

**仿射变换：**
给定一个眼睛和嘴巴三个点的百分数，利用mtcnn的最后一层网络找出人脸的眼睛鼻子嘴角五个点，通过opencv将对应的点移动到那三个点上，可能会放大缩小旋转等，比单纯的用opencv resize的效果更好 

确定五个点和仿射变换代码，使用的时候可以不用固定大小，直接的将byte多维数组转换为mat数据是可以的，但是可能由于检测处理的结果不怎么样的的原因，效果不算太好 
将返回的bytes数组，如果小于0的话转换为正数，再开始预白化 

为提高响应速度，可以先加载数据库中的数据，比如将数据预存到全局中，实现的过程中发现[Spring Boot中有缓存支持](http://blog.didispace.com/springbootcache1/)，第一次查询完数据后就会被存储到缓存中，不会重新从数据库中获取数据，而是从缓存中直接拿去数据（体现在控制台中查询语句不会输出第二次） 

[List自定义排序](https://blog.csdn.net/jadyer/article/details/12784021) 

多张图片的情况下，分别扔到网络里取得框框，然后返回List<object\>，包含框框的位置和整个图片的像素的byte多维数组，将all list扔到处理函数里面，先遍历图片，再遍历框框，一个一个框框扔到老师写的仿射变换中处理，然后得到结果，预白化，扔到一个batch_size大小的预白化四维数组，再把这个四维数组扔到FaceNet中embedding，得到feature 

对比思路：将所有的人脸和其对应的uId存到两个数组中（考虑在注册步骤后添加更新函数） 
与得到的对比，求出最大的k个值，和对应的uId,groupId，boxes不用动，顺序不会边，改变了的只有人脸的特征值（变为得分和顺序），返回每个对应的k个，然后返回Json 

