#Setting Java 1.6 as default Java



```

macbook:~ jgarcia$ java -version
java version "1.5.0_16"
Java(TM) 2 Runtime Environment, Standard Edition (build 1.5.0_16-b06-284)
Java HotSpot(TM) Client VM (build 1.5.0_16-133, mixed mode, sharing)

macbook:~ jgarcia$ ls -la /usr/bin/java
lrwxr-xr-x  1 root  wheel  74 Oct 29 14:54 /usr/bin/java -> /System/Library/Frameworks/JavaVM.framework/Versions/Current/Commands/java

macbook:~ jgarcia$ sudo ln -fs /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Commands/java /usr/bin/java
Password:

macbook:~ jgarcia$ java -version
java version "1.6.0_07"
Java(TM) SE Runtime Environment (build 1.6.0_07-b06-153)
Java HotSpot(TM) 64-Bit Server VM (build 1.6.0_07-b06-57, mixed mode)
macbook:~ jgarcia$ 

```