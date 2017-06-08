# CompletableFuture

## 参考

[Java CompletableFuture 详解](http://colobu.com/2016/02/29/Java-CompletableFuture/)

## 概述

**CompletableFuture** 是java8提供的一套新的异步操作Api，同类型的还有Guava、Netty的Future类库。它与Future相比最大的特点在于使用``异步回调``的方式返回信息，而不用通过``阻塞循环``的方式访问结果数据。





## API规范

- 使用``Runnable``类型的参数，一般代表不返回参数

- 使用``Consummer``系类型的参数，一般代表纯消费计算结果

- 使用``Function``系类型的参数，一般代表着计算结果的转换

- 使用``Async``结尾的方法方法，会放在线程池内执行，否则用当前线程执行




## API详解

### 创建CompletableFuture对象

我们可以通过一些静态API创建一些可以立刻执行的Future

```java                                           
public static CompletableFuture<Void> 	runAsync(Runnable runnable)
public static CompletableFuture<Void> 	runAsync(Runnable runnable, Executor executor)
public static <U> CompletableFuture<U> 	supplyAsync(Supplier<U> supplier)
public static <U> CompletableFuture<U> 	supplyAsync(Supplier<U> supplier, Executor executor)

```

#### Demo

```java

/**
     * runAsync会立刻触发Future的启动，异步执行Runable,默认使用 ForkJoinPool.commonPool()
     *
     * @throws IOException
     */
    public static void testRunAsync() throws IOException {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            System.out.println("sub thread :" + Thread.currentThread().getId());

            try {
                Thread.sleep(3000);
                System.out.println("testRunAsync success");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
    
    
    
    /**
     * supplyAsync 会立刻触发Future的启动，返回一个计算结果
     *
     * @throws IOException
     */
    public static String testSupplyAsync() throws IOException, ExecutionException, InterruptedException {

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            String result = "result : success";

            System.out.println("testSupplyAsync: " + result);

            return result;
        });

        return future.get();
    }
```

## 计算结果完成时的处理

 以下方法用来对返回结果后进行进一步处理。
 
```java
public CompletableFuture<T> 	whenComplete(BiConsumer<? super T,? super Throwable> action)
public CompletableFuture<T> 	whenCompleteAsync(BiConsumer<? super T,? super Throwable> action)
public CompletableFuture<T> 	whenCompleteAsync(BiConsumer<? super T,? super Throwable> action, Executor executor)
public CompletableFuture<T>     exceptionally(Function<Throwable,? extends T> fn)

```

#### Demo

```java
    /**
     * 主要进行future完成后的操作，可以对结果与异常进行处理，这里需要关注线程的变化，
     * 当future提前完成，whenComplete会正确执行，但是所在线程变成了主线程。
     * @throws InterruptedException the interrupted exception
     */
    public static void testWhenComplete() throws InterruptedException {
        CompletableFuture<Integer> future = getRandomFuture();

        // Thread.sleep(5000); //这里主要是测试future提前完成的时候的情况，当future提前完成，whenComplete会正确执行，但是所在线程变成了主线程。
        future.whenComplete((x, e) -> {
            System.out.println("whenComplete thread :" + Thread.currentThread().getId());
            System.out.println("testWhenComplete result: " + x);
        });
    }


```


## 转换操作
主要用于中介结果的处理，但是不支持对exception的中间结果的处理。如果需要对异常也进行处理，可以使用handle

```java
public <U> CompletableFuture<U> 	thenApply(Function<? super T,? extends U> fn)
public <U> CompletableFuture<U> 	thenApplyAsync(Function<? super T,? extends U> fn)
public <U> CompletableFuture<U> 	thenApplyAsync(Function<? super T,? extends U> fn, Executor executor)

```


#### Demo
```java

/**
     * 进行中间结果处理，同样的是用Async结尾的Api可能会切换到新的线程，是否使用新的线程会取决于executor(这里是ForkJoinPool.commonPool())的分配
     *
     * @throws InterruptedException the interrupted exception
     */
    public static void testThenApply() throws InterruptedException {
        CompletableFuture<Integer> future = getRandomFuture();
        //Thread.sleep(5000); //这里主要是测试future提前完成的时候的情况，当future提前完成，whenComplete会正确执行，但是所在线程变成了主线程。
        future.thenApplyAsync(x -> { //
            System.out.println("thenApply thread :" + Thread.currentThread().getId());
            return x + 1;
        }).thenApply(x -> {
            System.out.println("thenApply thread :" + Thread.currentThread().getId());
            return "x+1=" + x;
        }).whenCompleteAsync((s, e) -> {
            System.out.println("whenComplete thread :" + Thread.currentThread().getId());
            System.out.println(s);
        });
    }
```


### 支持异常处理的handle方法
```java
public <U> CompletableFuture<U> 	handle(BiFunction<? super T,Throwable,? extends U> fn)
public <U> CompletableFuture<U> 	handleAsync(BiFunction<? super T,Throwable,? extends U> fn)
public <U> CompletableFuture<U> 	handleAsync(BiFunction<? super T,Throwable,? extends U> fn, Executor executor)

```

## 纯消费操作

这种操作会对计算结果进行处理，但是不会有新的结果返回( 返回Void )


#### 基本API
```java

public CompletableFuture<Void> 	thenAccept(Consumer<? super T> action)
public CompletableFuture<Void> 	thenAcceptAsync(Consumer<? super T> action)
public CompletableFuture<Void> 	thenAcceptAsync(Consumer<? super T> action, Executor executor)
```

如何区分thenAccpt与whenCompelete？
>当如果有需要对多个future进行操作处理时，thenAccept会比whenComplete更加适合。

--- 

#### 进阶API

```java

public <U> CompletableFuture<Void> 	thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T,? super U> action)
public <U> CompletableFuture<Void> 	thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T,? super U> action)
public <U> CompletableFuture<Void> 	thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T,? super U> action, Executor executor)
public     CompletableFuture<Void> 	runAfterBoth(CompletionStage<?> other,  Runnable action)

public CompletableFuture<Void> 	thenRun(Runnable action)
public CompletableFuture<Void> 	thenRunAsync(Runnable action)
public CompletableFuture<Void> 	thenRunAsync(Runnable action, Executor executor)


```

- **Accept xxx** 会对结果进行处理
- **Run xxx** 不会依赖结果，只是在执行动作


#### DEMO

```java

    /**
     * thenAcceptBoth用来对两个future的结果进行处理，不返回任何消息
     */
    public static void testThenAcceptBoth() {
        CompletableFuture<Integer> future1 = getRandomFuture();
        CompletableFuture<Integer> future2 = getRandomFuture();
        future1.thenAcceptBoth(future2, (x, y) -> {
            System.out.println("x+y=" + (x + y));
        });
    }
```

## 组合

```java
public <U> CompletableFuture<U> 	thenCompose(Function<? super T,? extends CompletionStage<U>> fn)
public <U> CompletableFuture<U> 	thenComposeAsync(Function<? super T,? extends CompletionStage<U>> fn)
public <U> CompletableFuture<U> 	thenComposeAsync(Function<? super T,? extends CompletionStage<U>> fn, Executor executor)
public <U,V> CompletableFuture<V> 	thenCombine(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn)
public <U,V> CompletableFuture<V> 	thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn)
public <U,V> CompletableFuture<V> 	thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn, Executor executor)


```

#### DEMO


```java
 /**
     * thenCompose 有点类似flatMap,讲一个future的计算结果转化成另一个future
     */
    public static void testCompese() {

        CompletableFuture<Integer> future1 = getRandomFuture();
        future1.thenCompose(x -> CompletableFuture.supplyAsync(() -> x * 2)).whenComplete((x, e) -> {
            System.out.println("testCompese result:" + x);
        });
    }
```




## allOf 和 anyOf

用于两个以上的future协同操作
```java
public static CompletableFuture<Void> 	    allOf(CompletableFuture<?>... cfs)
public static CompletableFuture<Object> 	anyOf(CompletableFuture<?>... cfs)
```

#### Demo

```java
/**
     * allOFF 当所有future返回结果后触发操作进行回调。
     */
    public static void testAllOff() {
        CompletableFuture<Integer> future1 = getRandomFuture();
        CompletableFuture<Integer> future2 = getRandomFuture();
        CompletableFuture<Integer> future3 = getRandomFuture();
        CompletableFuture<Integer> future4=CompletableFuture.supplyAsync(()->{
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            throw  new RuntimeException();
        });

        CompletableFuture.allOf(future1, future2, future3,future4).whenComplete((x, e) -> {

            try {
                System.out.println("future1 value: "+future1.get());
                System.out.println("future2 value: "+future2.get());
                System.out.println("future3 value: "+future3.get());
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            } catch (ExecutionException e1) {
                e1.printStackTrace();
            }
        });

```
