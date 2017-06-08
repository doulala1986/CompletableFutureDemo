import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Created by doulala on 2017/6/8.
 */
public class CompletableFutureTest1 {


    public static void main(String[] args) throws Exception {

//        System.out.println("main thread :" + Thread.currentThread().getId());
//
//        testRunAsync();//runAsync会立刻触发Future的启动，异步执行Runable
//        String result = testSupplyAsync();//supplyAsync 会立刻触发Future的启动，返回一个计算结果
//        testWhenComplete();//主要进行future完成后的操作，可以对结果与异常进行处理，这里需要关注线程的变化，当future提前完成，whenComplete会正确执行，但是所在线程变成了主线程。
//        testThenApply();//进行中间结果处理，同样的是用Async结尾的Api可能会切换到新的线程，是否使用新的线程会取决于executor(这里是ForkJoinPool.commonPool())的分配
//        testThenAcceptBoth();
//        testCompese();
        testAllOff();
        System.in.read();
    }

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

    public static CompletableFuture<Integer> getRandomFuture() {

        Random r = new Random(System.currentTimeMillis());

        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {

            System.out.println("original future thread :" + Thread.currentThread().getId());

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int result = r.nextInt(100);
            System.out.println("original integer :" + result);

            return result;
        });
        return future;
    }


    /**
     * 主要进行future完成后的操作，可以对结果与异常进行处理，这里需要关注线程的变化，
     * 当future提前完成，whenComplete会正确执行，但是所在线程变成了主线程。
     *
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

    /**
     * thenCompose 有点类似flatMap,讲一个future的计算结果转化成另一个future
     */
    public static void testCompese() {

        CompletableFuture<Integer> future1 = getRandomFuture();
        future1.thenCompose(x -> CompletableFuture.supplyAsync(() -> x * 2)).whenComplete((x, e) -> {
            System.out.println("testCompese result:" + x);
        });
    }

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


    }
}
