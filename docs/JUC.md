# JUC核心知识点

- [JMM](#jmm)
- [volatile关键字](#volatile关键字)
  - [可见性](#可见性)
  - [原子性](#原子性)
  - [有序性](#有序性)
  - [哪些地方用到过volatile？](#哪些地方用到过volatile？)
    - [单例模式的安全问题](#单例模式的安全问题)
- [CAS](#cas)
  - [CAS底层原理](#cas底层原理)
  - [CAS缺点](#cas缺点)
- [ABA问题](#aba问题)
  - [AtomicReference](#atomicreference)
  - [AtomicStampedReference和ABA问题的解决](#atomicstampedreference和aba问题的解决)
- [集合类不安全问题](#集合类不安全问题)
  - [List](#list)
    - [CopyOnWriteArrayList](#copyonwritearraylist)
  - [Set](#set)
    - [HashSet和HashMap](#hashset和hashmap)
  - [Map](#map)
- [Java锁](#java锁)
  - [公平锁/非公平锁](#公平锁非公平锁)
  - [可重入锁/递归锁](#可重入锁递归锁)
    - [锁的配对](#锁的配对)
  - [自旋锁](#自旋锁)
  - [读写锁/独占/共享锁](#读写锁独占共享锁)
  - [Synchronized和Lock的区别](#synchronized和lock的区别)
- [CountDownLatch/CyclicBarrier/Semaphore](#countdownlatchcyclicbarriersemaphore)
  - [CountDownLatch](#countdownlatch)
    - [枚举类的使用](#枚举类的使用)
  - [CyclicBarrier](#cyclicbarrier)
  - [Semaphore](#semaphore)
- [阻塞队列](#阻塞队列)
  - [SynchronousQueue](#synchronousqueue)
- [Callable接口](#callable接口)
- [阻塞队列的应用——生产者消费者](#阻塞队列的应用生产者消费者)
  - [传统模式](#传统模式)
  - [阻塞队列模式](#阻塞队列模式)
- [阻塞队列的应用——线程池](#阻塞队列的应用线程池)
  - [线程池基本概念](#线程池基本概念)
  - [线程池三种常用创建方式](#线程池三种常用创建方式)
  - [线程池创建的七个参数](#线程池创建的七个参数)
  - [线程池底层原理](#线程池底层原理)
  - [线程池的拒绝策略](#线程池的拒绝策略)
  - [实际生产使用哪一个线程池？](#实际生产使用哪一个线程池？)
    - [自定义线程池参数选择](#自定义线程池参数选择)
- [死锁编码和定位](#死锁编码和定位)

# JMM

JMM是指Java**内存模型**，不是Java**内存布局**，不是所谓的栈、堆、方法区。

每个Java线程都有自己的**工作内存**。操作数据，首先从主内存中读，得到一份拷贝，操作完毕后再写回到主内存。

![](D:\Java\java-interview2\docs\images\JMM-17444487679902.png)

JMM可能带来**可见性**、**原子性**和**有序性**问题。所谓可见性，就是某个线程对主内存内容的更改，应该立刻通知到其它线程。原子性是指一个操作是不可分割的，不能执行到一半，就不执行了。所谓有序性，就是指令是有序的，不会被重排。

# volatile关键字

`volatile`关键字是Java提供的一种**轻量级**同步机制。它能够保证**可见性**和**有序性**，但是不能保证**原子性**。

## 可见性

```java
package com.ljt.juc;

import java.util.concurrent.TimeUnit;

class MyData {
    int number = 0;

    //此时number前面没有volatile 其他线程不能访问到最新的number值
    public void addPlusPlus() {
        number++;
    }

    public void setTo60() {
        this.number = 60;
    }

}

//volatile可以保证可见性，及时通知其它线程主物理内存的值已被修改
private static void volatileDemo() {
        System.out.println("可见性测试");
        MyData myData = new MyData();//资源类
        //启动一个线程操作共享数据
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + "\t come in");
            try {
                TimeUnit.SECONDS.sleep(3);
                //改变number的值 此处不加volatile main获取不到number的最新值
                myData.setTo60();
                System.out.println(Thread.currentThread().getName() + "\t update number value: " + myData.number);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "AAA").start();
        while (myData.number == 0) {
            //main线程持有共享数据的拷贝，一直为0
        }
        System.out.println(Thread.currentThread().getName() + "\t mission is over. main get number value: " + myData.number);
    }
}

```

`MyData`类是资源类，一开始number变量没有用volatile修饰，所以程序运行的结果是：

```java
可见性测试
AAA	 come in
AAA	 update number value: 60
```

虽然一个线程把number修改成了60，但是main线程持有的仍然是最开始的0，所以一直循环，程序不会结束。

如果对number添加了volatile修饰

```java
volatile int number = 0;
```

运行结果是：

```java
可见性测试
AAA	 come in
AAA	 update number value: 60
main	 mission is over. main get number value: 60
```

可见某个线程对number的修改，会立刻反映到主内存上。

## 原子性

volatile并**不能保证操作的原子性**。这是因为，比如一条number++的操作，会形成4条指令。

```assembly
getfield     //读
iconst_1	//++常量1
iadd		//加操作
putfield	//写操作
```

假设有3个线程，分别执行number++，都先从主内存中拿到最开始的值，number=0，然后三个线程分别进行操作。假设线程0执行完毕，number=1，也立刻通知到了其它线程，但是此时线程1、2已经拿到了number=0，所以结果就是写覆盖，线程1、2将number变成1。

```java
private static void atomicDemo() {
    System.out.println("原子性测试");
    MyData myData=new MyData();
    for (int i = 1; i <= 20; i++) {
        new Thread(()->{
            for (int j = 0; j <1000 ; j++) {
                myData.addPlusPlus();
                myData.addAtomic();
            }
        },String.valueOf(i)).start();
    }
    while (Thread.activeCount()>2){
        Thread.yield();
    }
    System.out.println(Thread.currentThread().getName()+"\t int type finally number value: "+myData.number);
    System.out.println(Thread.currentThread().getName()+"\t AtomicInteger type finally number value: "+myData.atomicInteger);
}
```

结果：可见，由于`volatile`不能保证原子性，出现了线程重复写的问题，最终结果比20000小。而`AtomicInteger`可以保证原子性。

```java
原子性测试
main	 int type finally number value: 17542
main	 AtomicInteger type finally number value: 20000
```

解决的方式就是：

1. 对`addPlusPlus()`方法加锁。
2. 使用`java.util.concurrent.AtomicInteger`类。

```java
  public synchronized void addPlusPlus() {
        number++;
    }

 AtomicInteger atomicInteger = new AtomicInteger();
```

## 有序性

```java
package com.ljt.juc;

public class ResortSeqDemo {

    int a = 0;
    boolean flag = false;

    /*
    多线程下flag=true可能先执行，还没走到a=1就被挂起。
    其它线程进入method02的判断，修改a的值=5，而不是6。
     */
    public void method01() {
        a = 1;
        flag = true;
    }

    public void method02() {
        if (flag) {
            a += 5;
            System.out.println("*****retValue: " + a);
        }
    }
}

```

volatile可以保证**有序性**，也就是防止**指令重排序**。所谓指令重排序，就是出于优化考虑，CPU执行指令的顺序跟程序员自己编写的顺序不一致。就好比一份试卷，题号是老师规定的，是程序员规定的，但是考生（CPU）可以先做选择，也可以先做填空。

```java
int x = 11; //语句1
int y = 12; //语句2
x = x + 5;  //语句3
y = x * x;  //语句4
```

以上例子，可能出现的执行顺序有1234、2134、1342，这三个都没有问题，最终结果都是x = 16，y=256。但是如果是4开头，就有问题了，y=0。这个时候就**不需要**指令重排序。

volatile底层是用CPU的**内存屏障**（Memory Barrier）指令来实现的，有两个作用，一个是保证特定操作的顺序性，二是保证变量的可见性。在指令之间插入一条Memory Barrier指令，告诉编译器和CPU，在Memory Barrier指令之间的指令不能被重排序。

## 哪些地方用到过volatile？

### 单例模式的安全问题

常见的DCL（Double Check Lock）模式虽然加了同步，但是在多线程下依然会有线程安全问题。

```java
package com.ljt.juc;

public class SingletonDemo {
    private SingletonDemo() {
        System.out.println(Thread.currentThread().getName() + "\t 我是构造方法");
    }

    //private static SingletonDemo instance;
    private volatile static SingletonDemo instance;
    //DCL模式 Double Check Lock 双端检索机制：在加锁前后都进行判断
    public static SingletonDemo getInstance() {
        if (instance == null) {
            synchronized (SingletonDemo.class) {
                if (instance == null) {
                    instance = new SingletonDemo();
                }
            }
        }
        return instance;
    }

    public static void main(String[] args) {
        for (int i = 1; i <= 1000; i++) {
            new Thread(() -> SingletonDemo.getInstance(), String.valueOf(i)).start();
        }
    }
}

```

这个漏洞比较tricky，很难捕捉，但是是存在的。`instance=new SingletonDemo();`可以大致分为三步

```java
memory = allocate();     //1.分配内存
instance(memory);	 //2.初始化对象
instance = memory;	 //3.设置引用地址
```

其中2、3没有数据依赖关系，**可能发生重排**。如果发生，此时内存已经分配，那么`instance=memory`不为null。如果此时线程挂起，`instance(memory)`还未执行，对象还未初始化。由于`instance!=null`，所以两次判断都跳过，最后返回的`instance`没有任何内容，还没初始化。

解决的方法就是对`singletondemo`对象添加上`volatile`关键字，禁止指令重排。

# CAS

CAS是指**Compare And Swap**，**比较并交换**，是一种很重要的同步思想。如果主内存的值跟期望值一样，那么就进行修改，否则一直重试，直到一致为止。

```java
package com.ljt.juc;

import java.util.concurrent.atomic.AtomicInteger;

public class CASDemo {
    public static void main(String[] args) {
        AtomicInteger atomicInteger = new AtomicInteger(5);
        System.out.println(atomicInteger.compareAndSet(5, 2019) + "\t current data : " + atomicInteger.get());
        //修改失败
        System.out.println(atomicInteger.compareAndSet(5, 1024) + "\t current data : " + atomicInteger.get());
    }
}
```

运行结果

```java
true	 current data : 2019
false	 current data : 2019
```

第一次修改，期望值为5，主内存也为5，修改成功，为2019。第二次修改，期望值为5，主内存为2019，修改失败。

查看`AtomicInteger.getAndIncrement()`方法，发现其没有加`synchronized`**也实现了同步**。这是为什么？

## CAS底层原理

`AtomicInteger`内部维护了`volatile int value`和`private  static final Unsafe unsafe`两个比较重要的参数。

```java
public final int getAndIncrement(){
    return unsafe.getAndAddInt(this,valueOffset,1);
}
```

`AtomicInteger.getAndIncrement()`调用了`Unsafe.getAndAddInt()`方法。`Unsafe`类的大部分方法都是`native`的，用来像C语言一样从底层操作内存。

```java
public final int getAnddAddInt(Object var1,long var2,int var4){
    int var5;
    do{
        var5 = this.getIntVolatile(var1, var2);
    } while(!this.compareAndSwapInt(var1, var2, var5, var5 + var4));
    return var5;
}
```

这个方法的var1和var2，就是根据**对象**和**偏移量**得到在**主内存的快照值**var5。然后`compareAndSwapInt`方法通过var1和var2得到当前**主内存的实际值**。如果这个**实际值**跟**快照值**相等，那么就更新主内存的值为var5+var4。如果不等，那么就一直循环，一直获取快照，一直对比，直到实际值和快照值相等为止。

比如有A、B两个线程，一开始都从主内存中拷贝了原值为3，A线程执行到`var5=this.getIntVolatile`，即var5=3。此时A线程挂起，B修改原值为4，B线程执行完毕，由于加了volatile，所以这个修改是立即可见的。A线程被唤醒，执行`this.compareAndSwapInt()`方法，发现这个时候主内存的值不等于快照值3，所以继续循环，**重新**从主内存获取。

## CAS缺点

CAS实际上是一种自旋锁，

1. 一直循环，开销比较大。
2. 只能保证一个变量的原子操作，多个变量依然要加锁。
3. 引出了**ABA问题**。

# ABA问题

所谓ABA问题，就是比较并交换的循环，存在一个**时间差**，而这个时间差可能带来意想不到的问题。比如线程T1将值从A改为B，然后又从B改为A。线程T2看到的就是A，但是**却不知道这个A发生了更改**。尽管线程T2 CAS操作成功，但不代表就没有问题。
有的需求，比如CAS，**只注重头和尾**，只要首尾一致就接受。但是有的需求，还看重过程，中间不能发生任何修改，这就引出了`AtomicReference`原子引用。

## AtomicReference

`AtomicInteger`对整数进行原子操作，如果是一个POJO呢？可以用`AtomicReference`来包装这个POJO，使其操作原子化。

```java
 public static void AtomicReferenceDemo() {
        User user1 = new User("Jack", 25);
        User user2 = new User("Lucy", 21);
        AtomicReference<User> atomicReference = new AtomicReference<>();
        atomicReference.set(user1);
        System.out.println(atomicReference.compareAndSet(user1, user2)); //true
        System.out.println(atomicReference.compareAndSet(user1, user2)); //false
    }
class User {

    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

## AtomicStampedReference和ABA问题的解决

使用`AtomicStampedReference`类可以解决ABA问题。这个类维护了一个“**版本号**”Stamp，在进行CAS操作的时候，不仅要比较当前值，还要比较**版本号**。只有两者都相等，才执行更新操作。

```java
AtomicStampedReference.compareAndSet(expectedReference,newReference,oldStamp,newStamp);`
```

```java
package com.ljt.juc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class ABADemo {
    static AtomicReference<Integer> atomicReference = new AtomicReference<>(100);
    static AtomicStampedReference<Integer> atomicStampedReference = new AtomicStampedReference<>(100, 1);

    public static void main(String[] args) {
        System.out.println("======ABA问题的产生======");

        new Thread(() -> {
            atomicReference.compareAndSet(100, 101);
            atomicReference.compareAndSet(101, 100);
        }, "t1").start();

        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();

            }
            System.out.println(atomicReference.compareAndSet(100, 2019) + "\t" + atomicReference.get().toString());
        }, "t2").start();

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("======ABA问题的解决======");
        new Thread(() -> {
            int stamp = atomicStampedReference.getStamp();
            System.out.println(Thread.currentThread().getName() + "\t第一次版本号： " + stamp);
            atomicStampedReference.compareAndSet(100, 101, atomicStampedReference.getStamp(), atomicStampedReference.getStamp() + 1);
            System.out.println(Thread.currentThread().getName() + "\t第二次版本号： " + atomicStampedReference.getStamp());
            atomicStampedReference.compareAndSet(101, 100, atomicStampedReference.getStamp(), atomicStampedReference.getStamp() + 1);
            System.out.println(Thread.currentThread().getName() + "\t第三次版本号： " + atomicStampedReference.getStamp());
        }, "t3").start();

        new Thread(() -> {
            int stamp = atomicStampedReference.getStamp();
            System.out.println(Thread.currentThread().getName() + "\t第一次版本号： " + stamp);
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            boolean result = atomicStampedReference.compareAndSet(100, 2019,
                    stamp, stamp + 1);
            System.out.println(Thread.currentThread().getName() + "\t修改成功与否：" + result + "  当前最新版本号" + atomicStampedReference.getStamp());
            System.out.println(Thread.currentThread().getName() + "\t当前实际值：" + atomicStampedReference.getReference());
        }, "t4").start();
    }
}

```



# 集合类不安全问题

## List

`ArrayList`不是线程安全类，在多线程同时写的情况下，会抛出`java.util.ConcurrentModificationException`异常。

```java
package com.ljt.juc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CollectionsNotSafeDemo {

    public static void main(String[] args) {
        listNotSafe();
    }

    private static void listNotSafe() {
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            new Thread(() -> {
                list.add(UUID.randomUUID().toString().substring(0, 8));
                System.out.println(Thread.currentThread().getName() + "\t" + list);
            }, String.valueOf(i)).start();
        }
    }
}

```

**解决方法**：

1. 使用`Vector`（`ArrayList`所有方法加`synchronized`，太重）。
2. 使用`Collections.synchronizedList()`转换成线程安全类。
3. 使用`java.concurrent.CopyOnWriteArrayList`（推荐）。

### CopyOnWriteArrayList

这是JUC的类，通过**写时复制**来实现**读写分离**。比如其`add()`方法，就是先**复制**一个新数组，长度为原数组长度+1，然后将新数组最后一个元素设为添加的元素。

```java
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        //得到旧数组
        Object[] elements = getArray();
        int len = elements.length;
        //复制新数组
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        //设置新元素
        newElements[len] = e;
        //设置新数组
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}
```

```java
private static void listSafe() {
//        List<String> list = Collections.synchronizedList(new ArrayList<>());
        List<String> list = new CopyOnWriteArrayList<>(); // 写时复制，读写分离（底层使用了ReentrantLock锁分段技术
        for (int i = 1; i <= 30; i++) {
            new Thread(() -> {
                list.add(UUID.randomUUID().toString().substring(0, 8));
                System.out.println(Thread.currentThread().getName() + "\t" + list);
            }, String.valueOf(i)).start();
        }
    }
```

## Set

跟List类似，`HashSet`和`TreeSet`都不是线程安全的，与之对应的有`CopyOnWriteSet`这个线程安全类。这个类底层维护了一个`CopyOnWriteArrayList`数组。

```java
private final CopyOnWriteArrayList<E> al;
public CopyOnWriteArraySet() {
    al = new CopyOnWriteArrayList<E>();
}
```

### HashSet和HashMap

`HashSet`底层是用`HashMap`实现的。既然是用`HashMap`实现的，那`HashMap.put()`需要传**两个参数**，而`HashSet.add()`只**传一个参数**，这是为什么？实际上`HashSet.add()`就是调用的`HashMap.put()`，只不过**Value**被写死了，是一个`private static final Object`对象。

```java
private transient HashMap<E,Object> map;

public HashSet() {
    map = new HashMap<>();
}
private static final Object PRESENT = new Object();
public boolean add(E e) {
    return map.put(e, PRESENT)==null;
}
```

## Map

`HashMap`不是线程安全的，`Hashtable`是线程安全的，但是跟`Vector`类似，太重量级。所以也有类似CopyOnWriteMap，只不过叫`ConcurrentHashMap`。

# Java锁

## 公平锁/非公平锁

**概念**：所谓**公平锁**，就是多个线程按照**申请锁的顺序**来获取锁，类似排队，先到先得。而**非公平锁**，则是多个线程抢夺锁，会导致**优先级反转**或**饥饿现象**。

**区别**：公平锁在获取锁时先查看此锁维护的**等待队列**，**为空**或者当前线程是等待队列的**队首**，则直接占有锁，否则插入到等待队列，FIFO原则。非公平锁比较粗鲁，上来直接**先尝试占有锁**，失败则采用公平锁方式。非公平锁的优点是**吞吐量**比公平锁更大。

`synchronized`和`juc.ReentrantLock`默认都是**非公平锁**。`ReentrantLock`在构造的时候传入`true`则是**公平锁**。

## 可重入锁/递归锁

可重入锁又叫递归锁，指的同一个线程在**外层方法**获得锁时，进入**内层方法**会自动获取锁。也就是说，线程可以进入任何一个它已经拥有锁的代码块。比如`get`方法里面有`set`方法，两个方法都有同一把锁，得到了`get`的锁，就自动得到了`set`的锁。

就像有了家门的锁，厕所、书房、厨房就为你敞开了一样。可重入锁可以**避免死锁**的问题。

```java
package com.ljt.juc;

import java.util.concurrent.locks.ReentrantLock;

import static com.ljt.juc.Phone.syncTest;

public class ReentrantLockDemo {
    public static void main(String[] args) throws InterruptedException {
        Phone phone = new Phone();
        syncTest(phone);
        Thread.sleep(1000);
        
        new Thread(phone, "A").start();
        new Thread(phone, "B").start();
    }
}

class Phone implements Runnable {
    //Synchronized TEST

    public static void syncTest(Phone phone) {

        new Thread(() -> {
            try {
                phone.sendSMS();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "t1").start();

        new Thread(() -> {
            try {
                phone.sendSMS();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "t2").start();
    }

    public synchronized void sendSMS() {
        System.out.println(Thread.currentThread().getName() + "\t" + "sendSMS()");
        sendEmail();
    }

    public synchronized void sendEmail() {
        System.out.println(Thread.currentThread().getName() + "\t" + "sendEmail()");
    }

    @Override
    public void run() {
        get();
    }

    ReentrantLock lock = new ReentrantLock();

    public void get() {
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "\t" + "get()");
            set();
        } finally {
            lock.unlock();
        }
    }

    public void set() {
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "\t" + "set()");
        } finally {
            lock.unlock();
        }
    }
}
```

```java
t1	sendSMS()
t1	sendEmail()
t2	sendSMS()
t2	sendEmail()
A	get()
A	set()
B	get()
B	set()
```

### 锁的配对

锁之间要配对，加了几把锁，最后就得解开几把锁，下面的代码编译和运行都没有任何问题。但锁的数量不匹配会导致死循环。

```java
private static void deadLockDemo() {
        ReentrantLock lock = new ReentrantLock();
        new Thread(()->{
            lock.lock();
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + "\t" + "lock()");
            }finally {
                lock.unlock();
            }
            System.out.println(Thread.currentThread().getName() + "\t" + "unlock()");
        }).start();
    }
```

注：该代码是否死锁存疑，`ReentrantLock` 是可重入的，允许同一线程多次获取同一个锁。

## 自旋锁

所谓自旋锁，就是尝试获取锁的线程不会**立即阻塞**，而是采用**循环的方式去尝试获取**。自己在那儿一直循环获取，就像“**自旋**”一样。这样的好处是减少**线程切换的上下文开销**，缺点是会**消耗CPU**。CAS底层的`getAndAddInt`就是**自旋锁**思想。

```java
//跟CAS类似，一直循环比较。
while (!atomicReference.compareAndSet(null, thread)) { }
```

```java
package com.ljt.juc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SpinLockDemo {

    AtomicReference<Thread> lock = new AtomicReference<>();

    public static void main(String[] args) {
        SpinLockDemo spinLock = new SpinLockDemo();
        new Thread(() -> {
            spinLock.lock();
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            spinLock.unlock();
        }, "t1").start();

        new Thread(() -> {
            spinLock.lock();
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            spinLock.unlock();
        }, "t2").start();
    }


    public void lock() {
        Thread thread = Thread.currentThread();
        System.out.println(Thread.currentThread().getName() + "\t" + "lock()");
        while (!lock.compareAndSet(null, thread)) {
            // 自旋
        }
    }

    public void unlock() {
        Thread thread = Thread.currentThread();
        System.out.println(Thread.currentThread().getName() + "\t" + "unlock()");
        lock.compareAndSet(thread, null);
    }

}

```

运算结果

```java
t1	lock()
t2	lock()
 //虽然t1 等待了5s，但是锁还是在t1手上，必须等到t1释放锁，t2才可以释放锁
t1	unlock()
t2	unlock()
```

## 读写锁/独占/共享锁

**读锁**是**共享的**，**写锁**是**独占的**。`juc.ReentrantLock`和`synchronized`都是**独占锁**，独占锁就是**一个锁**只能被**一个线程**所持有。有的时候，需要**读写分离**，那么就要引入读写锁，即`juc.ReentrantReadWriteLock`。

比如缓存，就需要读写锁来控制。缓存就是一个键值对，以下Demo模拟了缓存的读写操作，读的`get`方法使用了`ReentrantReadWriteLock.ReadLock()`，写的`put`方法使用了`ReentrantReadWriteLock.WriteLock()`。这样避免了写被打断，实现了多个线程同时读。

```java
package com.ljt.juc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockDemo {
    public static void main(String[] args) {
        MyCache myCache = new MyCache();
        // 写操作
        for (int i = 1; i <= 5; i++) {
            final int tempInt = i;
            new Thread(() -> {
                myCache.put(tempInt + "", tempInt + "");
            }, String.valueOf(i)).start();

        }
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 读操作
        for (int i = 1; i <= 5; i++) {
            final int tempInt = i;
            new Thread(() -> {
                myCache.get(tempInt + "");
            }, String.valueOf(i)).start();
        }
    }
}

class MyCache {
    volatile Map<String, Object> map = new HashMap<>();
    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void put(String key, Object value) {
        rwLock.writeLock().lock();
        try {
            System.out.println(Thread.currentThread().getName() + "\t" + "正在写入： " + key);
            //模拟网络传输
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } finally {
            rwLock.writeLock().unlock();
        }

        map.put(key, value);
        System.out.println(Thread.currentThread().getName() + "\t" + "写入完成");
    }

    public void get(String key) {
        rwLock.readLock().lock();
        try {
            System.out.println(Thread.currentThread().getName() + "\t" + "正在读取： " + key);
            //模拟网络传输
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } finally {
            rwLock.readLock().unlock();
        }
        Object result = map.get(key);
        System.out.println(Thread.currentThread().getName() + "\t" + "读取完成： " + result);
    }
}

```

输出结果如下

```java
1	正在写入： 1
1	写入完成
2	正在写入： 2
2	写入完成
3	正在写入： 3
3	写入完成
4	正在写入： 4
4	写入完成
5	正在写入： 5
5	写入完成
5	正在读取： 5
4	正在读取： 4
3	正在读取： 3
2	正在读取： 2
1	正在读取： 1
3	读取完成： 3
2	读取完成： 2
5	读取完成： 5
4	读取完成： 4
1	读取完成： 1
```

## 锁绑定多个条件Condition

多线程之问按顺序调用，实现A->B->C三个线程启动，要求如下:AA打印5次，BB打印10次，cc打印15次，紧接着，AA打印5次，BB打印10次，cc打印15次，来10轮

```java
package com.ljt.juc;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 多线程之问按顺序调用，实现A->B->C三个线程启动，
 * 要求如下:AA打印5次，BB打印10次，cc打印15次，
 * 紧接着，AA打印5次，BB打印10次，cc打印15次，
 * ...
 * 来10轮
 */
public class SyncAndReenLockDemo {
    public static void main(String[] args) {
        ShareResource shareResource = new ShareResource();
    
        // 启动线程AA，循环调用print5方法10次
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                shareResource.print5();
            }
        }, "AA").start();
    
        // 启动线程BB，循环调用print10方法10次
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                shareResource.print10();
            }
        }, "BB").start();
    
        // 启动线程CC，循环调用print15方法10次
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                shareResource.print15();
            }
        }, "CC").start();
    
    }

}

class ShareResource {
    private int number = 1;
    private final ReentrantLock lock = new ReentrantLock();
    Condition condition1 = lock.newCondition();
    Condition condition2 = lock.newCondition();
    Condition condition3 = lock.newCondition();

    public void print5() {
        // 加锁
        lock.lock();
        try {
            // 循环等待，直到number等于1
            while (number != 1) {
                condition1.await();
            }
            // 打印当前线程名称和1到5的数字
            for (int i = 0; i < 5; i++) {
                System.out.println(Thread.currentThread().getName() + "\t" + (i + 1));
            }
            // 将number设置为2
            number = 2;
            // 通知等待在condition2上的线程
            condition2.signal();
        } catch (InterruptedException e) {
            // 捕获中断异常，并抛出运行时异常
            throw new RuntimeException(e);
        } finally {
            // 解锁
            lock.unlock();
        }
    }


    public void print10() {
        lock.lock();
        try {
            while (number != 2) {
                condition2.await();
            }
            for (int i = 0; i < 10; i++) {
                System.out.println(Thread.currentThread().getName() + "\t" + (i + 1));
            }
            number = 3;
            condition3.signal();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    public void print15() {
        lock.lock();
        try {
            while (number != 3) {
                condition3.await();
            }
            for (int i = 0; i < 10; i++) {
                System.out.println(Thread.currentThread().getName() + "\t" + (i + 1));
            }
            number = 1;
            condition1.signal();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
```



## Synchronized和Lock的区别

`synchronized`关键字和`java.util.concurrent.locks.Lock`都能加锁，两者有什么区别呢？

1. **原始构成**：`sync`是JVM层面的，底层通过`monitorenter`和`monitorexit`来实现的（`sync`一个enter会有两个exit，一个是正常退出，一个是异常退出）。`Lock`是JDK API层面的，java.util.concurrent包的类。
2. **使用方法**：`sync`不需要手动释放锁，而`Lock`需要手动释放。
3. **是否可中断**：`sync`不可中断，除非抛出异常或者正常运行完成。`Lock`是可中断的，通过调用`interrupt()`方法。
4. **是否为公平锁**：`sync`只能是非公平锁，而`Lock`既能是公平锁，又能是非公平锁。
5. **绑定多个条件**：`sync`不能，只能随机唤醒。而`Lock`可以通过`Condition`来绑定多个条件，精确唤醒。

# CountDownLatch/CyclicBarrier/Semaphore

## CountDownLatch

`CountDownLatch`内部维护了一个**计数器**，只有当**计数器==0**时，某些线程才会停止阻塞，开始执行。

`CountDownLatch`主要有两个方法，`countDown()`来让计数器-1，`await()`来让线程阻塞。当`count==0`时，阻塞线程自动唤醒。

**案例一班长关门**：main线程是班长，6个线程是学生。只有6个线程运行完毕，都离开教室后，main线程班长才会关教室门。

**案例二秦灭六国**：只有6国都被灭亡后（执行完毕），main线程才会显示“秦国一统天下”。

### 枚举类的使用

在**案例二**中会使用到枚举类，因为灭六国，循环6次，想根据`i`的值来确定输出什么国，比如1代表楚国，2代表赵国。如果用判断则十分繁杂，而枚举类可以简化操作。

枚举类就像一个**简化的数据库**，枚举类名就像数据库名，枚举的项目就像数据表，枚举的属性就像表的字段。

```java
package com.ljt.juc;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class CountDownLatchDemo {
    public static void main(String[] args) throws InterruptedException {
        country();
    }

    private static void country() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(6);
        for (int i = 1; i <= 6; i++) {
            new Thread(() -> {
                System.out.println(Thread.currentThread().getName() + "\t 国被灭");
                // 计数器减1 表示一个国家被灭
                countDownLatch.countDown();
            }, Objects.requireNonNull(CountryEnum.list(i)).getRetMsg()).start();
        }
        // 主线程等待6个国家被灭，才能继续执行
        countDownLatch.await();
        System.out.println(Thread.currentThread().getName() + "\t ******秦国一统华夏");
    }
}

enum CountryEnum {

    ONE(1, "齐"),

    TWO(2, "楚"),

    THREE(3, "燕"),

    FOUR(4, "韩"),

    FIVE(5, "赵"),

    SIX(6, "魏");

    private final Integer retCode;
    private final String retMsg;

    CountryEnum(Integer retCode, String retMsg) {
        this.retCode = retCode;
        this.retMsg = retMsg;
    }

    public Integer getRetCode() {
        return retCode;
    }
    public String getRetMsg() {
        return retMsg;
    }

    public static CountryEnum list(int index) {
        CountryEnum[] values = CountryEnum.values();
        for (CountryEnum value : values) {
            if (value.getRetCode() == index) {
                return value;
            }
        }
        return null;
    }
}
```

输出结果如下

```java
齐	 国被灭
燕	 国被灭
楚	 国被灭
韩	 国被灭
魏	 国被灭
赵	 国被灭
main	 ******秦国一统华夏
```

## CyclicBarrier

`CyclicBarrier` 是 Java 中的一个并发工具类，用于协调多个线程在某个点上调用`await()` 方法等待彼此，,直到所有线程都到达该点后才继续执行。它类似于 `CountDownLatch`，但 `CyclicBarrier` 是可重用的，可以在多个屏障点之间循环使用。

```java
package com.ljt.juc;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierDemo {
    public static void main(String[] args) throws BrokenBarrierException, InterruptedException {

        CyclicBarrier cyclicBarrier = new CyclicBarrier(7, () -> System.out.println("*****召唤神龙"));
        // 七个参与者都到达屏障后才继续执行
        for (int i = 1; i <= 7; i++) {
            final int tempInt = i;
            new Thread(
                    () -> {
                        System.out.println(Thread.currentThread().getName() +
                                "\t 收集到第" + tempInt + "颗龙珠");
                        try {
                            // 让线程等待
                            cyclicBarrier.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            throw new RuntimeException(e);
                        }

                    }
                    , String.valueOf(i)).start();
        }
    }
}

```

输出结果如下

```java
1	 收集到第1颗龙珠
4	 收集到第4颗龙珠
3	 收集到第3颗龙珠
2	 收集到第2颗龙珠
6	 收集到第6颗龙珠
5	 收集到第5颗龙珠
7	 收集到第7颗龙珠
收集到7颗龙珠
```

`CyclicBarrier` 可重用性测试代码

```java
package com.ljt.juc;

import java.util.concurrent.CyclicBarrier;

public class ReusableCyclicBarrierDemo {
    public static void main(String[] args) {
        // 创建一个 CyclicBarrier，指定 3 个线程参与，屏障动作为打印所有线程到达的信息
        CyclicBarrier barrier = new CyclicBarrier(3, () -> {
            System.out.println("All threads have arrived at the barrier. Proceeding to next phase.");
        });

        // 创建并启动线程
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    // 第一个屏障点
                    System.out.println(Thread.currentThread().getName() + " is waiting at the first barrier");
                    barrier.await(); // 等待其他线程到达
                    System.out.println(Thread.currentThread().getName() + " has passed the first barrier");

                    // 第二个屏障点
                    System.out.println(Thread.currentThread().getName() + " is waiting at the second barrier");
                    barrier.await(); // 等待其他线程到达
                    System.out.println(Thread.currentThread().getName() + " has passed the second barrier");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
```

输出结果如下

```java
Thread-0 is waiting at the first barrier
Thread-2 is waiting at the first barrier
Thread-1 is waiting at the first barrier
All threads have arrived at the barrier. Proceeding to next phase.
Thread-1 has passed the first barrier
Thread-1 is waiting at the second barrier
Thread-0 has passed the first barrier
Thread-0 is waiting at the second barrier
Thread-2 has passed the first barrier
Thread-2 is waiting at the second barrier
All threads have arrived at the barrier. Proceeding to next phase.
Thread-2 has passed the second barrier
Thread-1 has passed the second barrier
Thread-0 has passed the second barrier
```

## Semaphore 

Semaphore /ˈseməfɔː(r)/

`CountDownLatch`的问题是**不能复用**。比如`count=3`，那么加到3，就不能继续操作了。而`Semaphore`可以解决这个问题，比如6辆车3个停车位，对于`CountDownLatch`**只能停3辆车**，而`Semaphore`可以停6辆车，车位空出来后，其它车可以占有，这就涉及到了`Semaphore.accquire()`和`Semaphore.release()`方法。

```java
package com.ljt.juc;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SemaphoreDemo {
    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore(3);

        for (int i = 1; i <= 6; i++) {
            new Thread(() -> {

                try {
                    semaphore.acquire();
                    System.out.println(Thread.currentThread().getName() +
                            "\t抢到车位");
                    try {
                        TimeUnit.SECONDS.sleep(3);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread().getName() +
                            "\t停车3秒后离开车位");
                } catch (Exception ignored) {

                } finally {
                    semaphore.release();
                }

            }).start();
        }
    }
}

```

运行结果如下

```java
Thread-0	抢到车位
Thread-2	抢到车位
Thread-1	抢到车位
Thread-0	停车3秒后离开车位
Thread-1	停车3秒后离开车位
Thread-2	停车3秒后离开车位
Thread-5	抢到车位
Thread-4	抢到车位
Thread-3	抢到车位
Thread-3	停车3秒后离开车位
Thread-4	停车3秒后离开车位
Thread-5	停车3秒后离开车位
```

# 阻塞队列

**概念**：当阻塞队列为空时，获取（take）操作是阻塞的；当阻塞队列为满时，添加（put）操作是阻塞的。

![](D:\Java\java-interview2\docs\images\BlockingQueue.png)

**好处**：阻塞队列不用手动控制什么时候该被阻塞，什么时候该被唤醒，简化了操作。

**体系**：`Collection`→`Queue`→`BlockingQueue`→七个阻塞队列实现类。

| 类名                    | 作用                             |
| ----------------------- | -------------------------------- |
| **ArrayBlockingQueue**  | 由**数组**构成的**有界**阻塞队列 |
| **LinkedBlockingQueue** | 由**链表**构成的**有界**阻塞队列 |
| PriorityBlockingQueue   | 支持优先级排序的无界阻塞队列     |
| DelayQueue              | 支持优先级的延迟无界阻塞队列     |
| **SynchronousQueue**    | 单个元素的阻塞队列               |
| LinkedTransferQueue     | 由链表构成的无界阻塞队列         |
| LinkedBlockingDeque     | 由链表构成的双向阻塞队列         |

粗体标记的三个用得比较多，许多消息中间件底层就是用它们实现的。

需要注意的是`LinkedBlockingQueue`虽然是有界的，但有个巨坑，其默认大小是`Integer.MAX_VALUE`，高达21亿，一般情况下内存早爆了（在线程池的`ThreadPoolExecutor`有体现）。

**API**：抛出异常是指当队列满时，再次插入会抛出异常；返回布尔是指当队列满时，再次插入会返回false；阻塞是指当队列满时，再次插入会被阻塞，直到队列取出一个元素，才能插入。超时是指当一个时限过后，才会插入或者取出。

```java
package com.ljt.juc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockingQueueDemo {
    public static void main(String[] args) throws InterruptedException {
        ArrayBlockingQueue<String> arrayBlockingQueue = new ArrayBlockingQueue<>(3);
        addAndRemove(arrayBlockingQueue);
        offerAndPoll(arrayBlockingQueue);
        putAndTake(arrayBlockingQueue);
        offerOutOfTime(arrayBlockingQueue);
    }


    public static void addAndRemove(BlockingQueue<String> blockingQueue) {
        System.out.println("----------------addAndRemove()----------------");
        System.out.println(blockingQueue.add("a"));
        System.out.println(blockingQueue.add("b"));
        System.out.println(blockingQueue.add("c"));
        // Queue full
        //System.out.println(blockingQueue.add("d"));

        System.out.println(blockingQueue.remove());
        System.out.println(blockingQueue.remove());
        System.out.println(blockingQueue.remove());
        //error
        //System.out.println(blockingQueue.remove());
    }

    public static void offerAndPoll(BlockingQueue<String> blockingQueue) {
        System.out.println("----------------offerAndPoll()----------------");
        System.out.println(blockingQueue.offer("a"));
        System.out.println(blockingQueue.offer("b"));
        System.out.println(blockingQueue.offer("c"));
        System.out.println(blockingQueue.offer("d"));
        // peek 查看队首
        System.out.println(blockingQueue.peek());
        System.out.println(blockingQueue.poll());
        System.out.println(blockingQueue.poll());
        System.out.println(blockingQueue.poll());
        System.out.println(blockingQueue.poll());
    }

    public static void putAndTake(BlockingQueue<String> blockingQueue) throws InterruptedException {
        System.out.println("----------------putAndTake()----------------");
        blockingQueue.put("a");
        blockingQueue.put("b");
        blockingQueue.put("c");
        // 由于超过队列的长度 添加d将会阻塞 在这里阻塞
        // blockingQueue.put("d");
        System.out.println(blockingQueue.take());
        System.out.println(blockingQueue.take());
        System.out.println(blockingQueue.take());
        // 由于队列为空 这里将会阻塞
        // System.out.println(blockingQueue.take());
    }


    public static void offerOutOfTime(BlockingQueue<String> blockingQueue) throws InterruptedException {
        System.out.println("----------------offerOutOfTime()----------------");
        System.out.println(blockingQueue.offer("a", 3L, TimeUnit.SECONDS));
        System.out.println(blockingQueue.offer("b", 3L, TimeUnit.SECONDS));
        System.out.println(blockingQueue.offer("c", 3L, TimeUnit.SECONDS));
        // 由于队列满了 这里将会阻塞3秒后返回false
        System.out.println(blockingQueue.offer("d", 3L, TimeUnit.SECONDS));
    }

}

```

运行结果如下

```java
----------------addAndRemove()----------------
true
true
true
a
b
c
----------------offerAndPoll()----------------
true
true
true
false
a
a
b
c
null
----------------putAndTake()----------------
a
b
c
----------------offerOutOfTime()----------------
true
true
true
false
```



| 方法类型 | 抛出异常  | 返回布尔   | 阻塞     | 超时                     |
| -------- | --------- | ---------- | -------- | ------------------------ |
| 插入     | add(E e)  | offer(E e) | put(E e) | offer(E e,Time,TimeUnit) |
| 取出     | remove()  | poll()     | take()   | poll(Time,TimeUnit)      |
| 队首     | element() | peek()     | 无       | 无                       |

## SynchronousQueue

队列只有一个元素，如果想插入多个，必须等队列元素取出后，才能插入，只能有一个“坑位”，用一个插一个，

```java
package com.ljt.juc;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class SynchronousQueueDemo {
    public static void main(String[] args) {
        SynchronousQueue<String> synchronousQueue = new SynchronousQueue<>();

        new Thread(() -> {
            try {
                System.out.println(Thread.currentThread().getName()+"\t put 1");
                synchronousQueue.put("1");
                System.out.println(Thread.currentThread().getName()+"\t put 2");
                synchronousQueue.put("2");
                System.out.println(Thread.currentThread().getName()+"\t put 3");
                synchronousQueue.put("3");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }).start();

        new Thread(() -> {
            try {
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "\t take " + synchronousQueue.take());
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "\t take " + synchronousQueue.take());
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "\t take" + synchronousQueue.take());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();

    }
}

```

运行结果如下

```java
Thread-0	 put 1
Thread-1	 take 1
Thread-0	 put 2
Thread-1	 take 2
Thread-0	 put 3
Thread-1	 take3
```



# Callable接口

**与Runnable的区别**：

1. Callable带返回值。
2. 会抛出异常。
3. 覆写`call()`方法，而不是`run()`方法。

**Callable接口的使用**：

```java
package com.ljt.juc;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class CallableDemo {
    //实现Callable接口
    static class MyThread implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("callable come in ...");
            return 1024;
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //创建FutureTask类，接受MyThread。    
        FutureTask<Integer> futureTask = new FutureTask<>(new MyThread());
        //将FutureTask对象放到Thread类的构造器里面。
        new Thread(futureTask, "AA").start();
        int result01 = 100;
        //用FutureTask的get方法得到返回值。
        int result02 = futureTask.get();
        System.out.println("result=" + (result01 + result02));
    }
}
```

运行结果如下

```java
callable come in ...
result=1124
```

# 阻塞队列的应用——生产者消费者

## 传统模式

传统模式使用`Lock`来进行操作，需要手动加锁、解锁。

```java
package com.ljt.juc;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 初始值为0的变量，两个线程交替操作，一个+1，一个-1，执行五轮
 * 1 线程  操作  资源类
 * 2 判断  干活  通知
 * 3 防止虚假唤醒机制
 */
public class ProdConsTradiDemo {
    public static void main(String[] args) {
        ShareData shareData = new ShareData();

        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    shareData.increment();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Producer").start();

        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    shareData.decrement();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Consumer").start();

    }
}

class ShareData {
    private int number = 0;
    private Lock lock = new ReentrantLock();
    
    private Condition condition = lock.newCondition();

    public void increment() throws InterruptedException {
        lock.lock();
        try {
            //1 判断
            while (number != 0) {
                //等待，不能生产
                condition.await();
            }
            //2 干活
            number++;
            System.out.println(Thread.currentThread().getName() + "\t" + number);
            //3 通知唤醒
            condition.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void decrement() throws InterruptedException {
        lock.lock();
        try {
            //1 判断
            while (number == 0) {
                //等待，不能生产
                condition.await();
            }
            //2 干活
            number--;
            System.out.println(Thread.currentThread().getName() + "\t" + number);
            //3 通知唤醒
            condition.signalAll();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}

```

运行结果如下

```java
Producer	1
Consumer	0
Producer	1
Consumer	0
Producer	1
Consumer	0
Producer	1
Consumer	0
Producer	1
Consumer	0
```

## 阻塞队列模式

使用阻塞队列就不需要手动加锁了。

```java
package com.ljt.juc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ProdConsBlockQueueDemo {
    public static void main(String[] args) {
        MyResource myResource = new MyResource(new ArrayBlockingQueue<>(10));
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + "\t生产线程启动");
            try {
                myResource.myProd();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "prod").start();

        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + "\t消费线程启动");
            try {
                myResource.myCons();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "cons").start();

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("5秒钟后，叫停");
        myResource.stop();
    }
}

class MyResource {
    private volatile boolean FLAG = true; //默认开启，进行生产+消费
    private AtomicInteger atomicInteger = new AtomicInteger();

    private BlockingQueue<String> blockingQueue;

    public MyResource(BlockingQueue<String> blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    public void myProd() throws Exception {
        String data;
        boolean retValue;
        // 循环条件：当FLAG为true时继续循环
        while (FLAG) {
            // 原子性地增加atomicInteger的值，并将其转换为字符串
            data = atomicInteger.incrementAndGet() + "";//++i
            // 尝试在阻塞队列中插入数据，等待时间为2秒
            retValue = blockingQueue.offer(data, 2L, TimeUnit.SECONDS);
            // 如果插入成功
            if (retValue) {
                // 打印当前线程名及插入成功的信息
                System.out.println(Thread.currentThread().getName() + "\t" + "插入队列" + data + "成功");
            } else {
                // 打印当前线程名及插入失败的信息
                System.out.println(Thread.currentThread().getName() + "\t" + "插入队列" + data + "失败");
            }
            // 线程休眠1秒
            TimeUnit.SECONDS.sleep(1);
        }
        // 打印当前线程名及停止生产的信息
        System.out.println(Thread.currentThread().getName() + "\tFLAG==false，停止生产");
    }


    public void myCons() throws Exception {
        // 定义一个字符串变量res
        String res;
        // 当FLAG为true时，进入循环
        while (FLAG) {
            // 从阻塞队列中尝试获取元素，超时时间为2秒
            res = blockingQueue.poll(2L, TimeUnit.SECONDS);
            // 如果res为null或者res为空字符串
            if (null == res || res.equalsIgnoreCase("")) {
                // 将FLAG设置为false
                FLAG = false;
                // 输出当前线程名称和提示信息
                System.out.println(Thread.currentThread().getName() + "\t超过2秒钟没有消费，退出消费");
                // 退出方法
                return;
            }
            // 输出当前线程名称和消费成功的信息
            System.out.println(Thread.currentThread().getName() + "\t消费队列" + res + "成功");
        }
    }


    public void stop() {
        this.FLAG = false;
    }
}

```

运行结果如下

```java
prod	生产线程启动
cons	消费线程启动
prod	插入队列1成功
cons	消费队列1成功
prod	插入队列2成功
cons	消费队列2成功
cons	消费队列3成功
prod	插入队列3成功
prod	插入队列4成功
cons	消费队列4成功
prod	插入队列5成功
cons	消费队列5成功
5秒钟后，叫停
prod	FLAG==false，停止生产
cons	超过2秒钟没有消费，退出消费
```

# 阻塞队列的应用——线程池

## 线程池基本概念

**概念**：线程池主要是控制运行线程的数量，将待处理任务放到等待队列，然后创建线程执行这些任务。如果超过了最大线程数，则等待。

**优点**：

1. 线程复用：不用一直new新线程，重复利用已经创建的线程来降低线程的创建和销毁开销，节省系统资源。
2. 提高响应速度：当任务达到时，不用创建新的线程，直接利用线程池的线程。
3. 管理线程：可以控制最大并发数，控制线程的创建等。

**体系**：`Executor`→`ExecutorService`→`AbstractExecutorService`→`ThreadPoolExecutor`。`ThreadPoolExecutor`是线程池创建的核心类。类似`Arrays`、`Collections`工具类，`Executor`也有自己的工具类`Executors`。

## 线程池三种常用创建方式

**newFixedThreadPool**：使用`LinkedBlockingQueue`实现，定长线程池。

```java
public static ExecutorService newFixedThreadPool(int nThreads) {
    return new ThreadPoolExecutor(nThreads, nThreads,
                                  0L, TimeUnit.MILLISECONDS,
                                  new LinkedBlockingQueue<Runnable>());
}
```

**newSingleThreadExecutor**：使用`LinkedBlockingQueue`实现，一池只有一个线程。

```java
public static ExecutorService newSingleThreadExecutor() {
    return new FinalizableDelegatedExecutorService(new ThreadPoolExecutor(1, 1,
                                    0L, TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<Runnable>()));
}
```

**newCachedThreadPool**：使用`SynchronousQueue`实现，变长线程池。

```java
public static ExecutorService newCachedThreadPool() {
    return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                 60L, TimeUnit.SECONDS,
                                 new SynchronousQueue<Runnable>());
}
```

## 线程池创建的七个参数

| 参数            | 意义                       |
| --------------- | -------------------------- |
| corePoolSize    | 线程池常驻核心线程数       |
| maximumPoolSize | 能够容纳的最大线程数       |
| keepAliveTime   | 多余线程存活时间           |
| unit            | 存活时间单位               |
| workQueue       | 存放提交但未执行任务的队列 |
| threadFactory   | 创建线程的工厂类           |
| handler         | 等待队列满后的拒绝策略     |

**理解**：线程池的创建参数，就像一个**银行**。

`corePoolSize`就像银行的“**当值窗口**“，比如今天有**2位柜员**在受理**客户请求**（任务）。如果超过2个客户，那么新的客户就会在**等候区**（等待队列`workQueue`）等待。当**等候区**也满了，这个时候就要开启“**加班窗口**”，让其它3位柜员来加班，此时达到**最大窗口**`maximumPoolSize`，为5个。如果开启了所有窗口，等候区依然满员，此时就应该启动”**拒绝策略**“`handler`，告诉不断涌入的客户，叫他们不要进入，已经爆满了。由于不再涌入新客户，办完事的客户增多，窗口开始空闲，这个时候就通过`keepAlivetTime`将多余的3个”加班窗口“取消，恢复到2个”当值窗口“。



## 线程池底层原理

**原理图**：上面银行的例子，实际上就是线程池的工作原理。

![](D:\Java\java-interview2\docs\images\threadPool.png)

**流程图**：

![](D:\Java\java-interview2\docs\images\threadPoolProcedure.png)

新任务到达→

如果正在运行的线程数小于`corePoolSize`，创建核心线程；大于等于`corePoolSize`，放入等待队列。

如果等待队列已满，但正在运行的线程数小于`maximumPoolSize`，创建非核心线程；大于等于`maximumPoolSize`，启动拒绝策略。

当一个线程无事可做一段时间`keepAliveTime`后，如果正在运行的线程数大于`corePoolSize`，则关闭非核心线程。

```java
package com.ljt.juc;

import java.util.concurrent.*;

/**
 * 第四种使用Java多线程的方式，线程池
 */
public class MyThreadPoolDemo {
    public static void main(String[] args) {
        System.out.println("Fixed Thread Pool");
        fixedThreadPool();
        System.out.println("Single Thread Pool");
        singleThreadPool();
        System.out.println("Cached Thread Pool");
        cachedThreadPool();
        System.out.println("Custom Thread Pool");
        customThreadPool();
    }

    private static void customThreadPool() {
        ExecutorService threadPool =
                new ThreadPoolExecutor(2,
                        5,
                        1L,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(3),
                        Executors.defaultThreadFactory(),
                        new ThreadPoolExecutor.AbortPolicy()
                );
        try {
            for (int i = 0; i < 9; i++) {
                threadPool.execute(() -> System.out.println(Thread.currentThread().getName() + "\t办理业务"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private static void cachedThreadPool() {
        //不定量线程
        ExecutorService threadPool = Executors.newCachedThreadPool();
        try {
            for (int i = 0; i < 9; i++) {
                threadPool.execute(() -> System.out.println(Thread.currentThread().getName() + "\t办理业务"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private static void singleThreadPool() {
        //一池1个线程
        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        try {
            for (int i = 0; i < 9; i++) {
                threadPool.execute(() -> System.out.println(Thread.currentThread().getName() + "\t办理业务"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private static void fixedThreadPool() {
        //一池5个线程
        ExecutorService threadPool = Executors.newFixedThreadPool(5);
        //一般常用try-catch-finally
        //模拟10个用户来办理业务，每个用户就是一个线程
        try {
            for (int i = 0; i < 9; i++) {
                threadPool.execute(() -> System.out.println(Thread.currentThread().getName() + "\t办理业务"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }
}

```

## 线程池的拒绝策略

当等待队列满时，且达到最大线程数，再有新任务到来，就需要启动拒绝策略。JDK提供了四种拒绝策略，分别是。

1. **AbortPolicy**：默认的策略，直接抛出`RejectedExecutionException`异常，阻止系统正常运行。
2. **CallerRunsPolicy**：既不会抛出异常，也不会终止任务，而是将任务返回给调用者。
3. **DiscardOldestPolicy**：抛弃队列中等待最久的任务，然后把当前任务加入队列中尝试再次提交任务。
4. **DiscardPolicy**：直接丢弃任务，不做任何处理。

## 实际生产使用哪一个线程池？

**单一、可变、定长都不用**！原因就是`FixedThreadPool`和`SingleThreadExecutor`底层都是用`LinkedBlockingQueue`实现的，这个队列最大长度为`Integer.MAX_VALUE`，显然会导致OOM。所以实际生产一般自己通过`ThreadPoolExecutor`的7个参数，自定义线程池。

```java
ExecutorService threadPool=new ThreadPoolExecutor(2,5,
                        1L,TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(3),
                        Executors.defaultThreadFactory(),
                        new ThreadPoolExecutor.AbortPolicy());
```

### 自定义线程池参数选择

对于CPU密集型任务，最大线程数是CPU线程数+1。对于IO密集型任务，尽量多配点，可以是CPU线程数*2，或者CPU线程数/(1-阻塞系数)。

# 死锁编码和定位

```java
package com.ljt.juc;

import java.util.concurrent.TimeUnit;

public class DeadLockDemo {
    public static void main(String[] args) {
      String lockA = "lockA";
      String lockB = "lockB";
      new Thread( new HoldLockThread(lockA,lockB)).start();
      new Thread( new HoldLockThread(lockB,lockA)).start();

    }
}

class HoldLockThread implements Runnable {

    private String lockA;
    private String lockB;

    public HoldLockThread(String lockA, String lockB) {
        this.lockA = lockA;
        this.lockB = lockB;
    }

    @Override
    public void run() {
        synchronized (lockA) {
            System.out.println(Thread.currentThread().getName() + "\t自己持有：" + lockA + "\t尝试获取：" + lockB);
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (lockB) {
                System.out.println(Thread.currentThread().getName() + "\t自己持有：" + lockB + "\t尝试获取：" + lockA);
            }
        }
    }
}

```

运行结果如下

```java
Thread-1	自己持有：lockB	尝试获取：lockA
Thread-0	自己持有：lockA	尝试获取：lockB
```



主要是两个命令配合起来使用，定位死锁。

**jps**指令：`jps -l`可以查看运行的Java进程。

```java
jps -l
5952 Launcher
12932 DeadLockDemo
6740 
22716 Jps
```

**jstack**指令：`jstack pid`可以查看某个Java进程的堆栈信息，同时分析出死锁。

```java
Java stack information for the threads listed above:
===================================================
"Thread-1":
        at com.ljt.juc.HoldLockThread.run(DeadLockDemo.java:35)
        - waiting to lock <0x000000076f0e15a0> (a java.lang.String)
        - locked <0x000000076f0e15d8> (a java.lang.String)
        at java.lang.Thread.run(Thread.java:750)
"Thread-0":
        at com.ljt.juc.HoldLockThread.run(DeadLockDemo.java:35)
        - waiting to lock <0x000000076f0e15d8> (a java.lang.String)
        - locked <0x000000076f0e15a0> (a java.lang.String)
        at java.lang.Thread.run(Thread.java:750)

Found 1 deadlock.
```

