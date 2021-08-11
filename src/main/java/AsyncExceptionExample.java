import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncExceptionExample {

  /**
   * To test completion exception without exception handling, uncomment commented out line
   *
   * @param args
   */
  public static void main(String[] args) {
    AsyncExceptionExample example = new AsyncExceptionExample();
    //example.testCompletionException();
    example.testExceptionally();
    example.testWhenComplete();
    example.testHandle();
    example.testHandleWithExecutor();
    example.testHandleWithExecutorAsync();
  }

  public void testCompletionException() {
    System.out.println("=====testCompletionException=====");
    CompletableFuture<List<String>> cf = CompletableFuture.supplyAsync(() -> List.of("a", "b", "c"))
            .thenApply((l) -> {
              System.out.println("failure logged");
              throw new RuntimeException("exception thrown!!");
            });
    List<String> list = cf.join();
    System.out.println(list);
  }


  /**
   * Testing exceptionally implementation
   * If no exception happen, then this exceptionally will be transparent
   */
  public void testExceptionally() {
    System.out.println("=====testExceptionally=====");
    CompletableFuture.supplyAsync(() -> List.of("a", "b", "c"))
            .thenApply((l) -> {
              System.out.println("failure logged");
              throw new RuntimeException("exception thrown!!");
            })
            .exceptionally(exception -> (List.of(exception.getMessage())))
            .thenAccept(l -> System.out.println(l));
  }

  /**
   * Test when complete which take a biconsumer (probable result and probable exception)
   */
  public void testWhenComplete() {
    System.out.println("=====testWhenComplete=====");
    CompletableFuture.supplyAsync(() -> List.of("a", "b", "c"))
            .thenApply((l) -> {
              //System.out.println("failure logged");
              throw new RuntimeException("exception thrown!!");
              //return l;
            })
            .whenComplete((result, exception) -> {
              if (result != null) {
                System.out.println("result ok!!");
              } else {
                System.out.println("Failed result");
              }
            })
            .thenAccept(l -> System.out.println(l));
  }

  /**
   * Test handle, which take a biconsumer (probable result and probable exception)
   * This is different than whenComplete in that it can swallow the exception
   */
  public void testHandle() {
    System.out.println("=====testHandle=====");
    CompletableFuture.supplyAsync(() -> List.of("a", "b", "c"))
            .thenApply((l) -> {
              //System.out.println("failure logged");
              throw new RuntimeException("exception thrown within " + Thread.currentThread().getName());
              //return l;
            })
            .handle((result, exception) -> {
              if (result != null) {
                System.out.println("result ok from within " + Thread.currentThread().getName());
                return result;
              } else {
                System.out.println("Failed result from within " + Thread.currentThread().getName());
                return List.of(exception.getMessage());
              }
            })
            .thenAccept(l -> System.out.println(l + Thread.currentThread().getName()));
  }

  /**
   * Alternate flavour of Handle exception running using executor pool (instead of FJP)
   */
  public void testHandleWithExecutor() {
    System.out.println("=====testHandleWithExecutor=====");
    ExecutorService executor = Executors.newFixedThreadPool(4);

    CompletableFuture.supplyAsync(() -> List.of("a", "b", "c"), executor)
            .thenApply((l) -> {
              //System.out.println("failure logged");
              throw new RuntimeException("exception thrown within " + Thread.currentThread().getName());
              //return l;
            })
            .handle((result, exception) -> {
              if (result != null) {
                System.out.println("result ok from within " + Thread.currentThread().getName());
                return result;
              } else {
                System.out.println("Failed result from within " + Thread.currentThread().getName());
                return List.of(exception.getMessage());
              }
            })
            .thenAccept(l -> System.out.println(l + Thread.currentThread().getName()));

    executor.shutdown();
  }

  /**
   * Another flavour of Handle exception using executor and piped via asynchronous calls
   */
  public void testHandleWithExecutorAsync() {
    System.out.println("=====testHandleWithExecutorAsync=====");
    ExecutorService executor = Executors.newFixedThreadPool(4);

    CompletableFuture<Void> cf = CompletableFuture.supplyAsync(() -> List.of("a", "b", "c"), executor)
            .thenApply((l) -> {
              //System.out.println("failure logged");
              throw new RuntimeException("exception thrown within " + Thread.currentThread().getName());
              //return l;
            })
            .handleAsync((result, exception) -> {
              if (result != null) {
                System.out.println("result ok from within " + Thread.currentThread().getName());
                return result;
              } else {
                System.out.println("Failed result from within " + Thread.currentThread().getName());
                return List.of(exception.getMessage());
              }
            }, executor)
            .thenAcceptAsync(l -> System.out.println(l + Thread.currentThread().getName()), executor);

    cf.join();

    executor.shutdown();
  }
}
