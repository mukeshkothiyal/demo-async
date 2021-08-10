import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
  public static void main(String[] args) {
    Main main = new Main();
    ExecutorService executor = Executors.newFixedThreadPool(4);

    main.executeOnRunnable(executor);
    main.executeOnRunnableAsync(executor);
    main.executeOnSupplier(executor);
    main.executeApplyEither(executor);
    main.executeAnyOf(executor);
    main.executeAsynchronous(executor);

    executor.shutdown();
  }

  /**
   * Chaining runnable -> runnable -> consumer
   * Additionally test how asynchronous calls are handled synchronously (i.e. input of previous process is piped
   * synchronously to next process)
   * <p>
   * It usually doesn't make any sense to chain a function or supplier over a runnable
   */
  private void executeOnRunnable(Executor executor) {
    System.out.println("======executeOnRunnable=====");
    CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
      System.out.println("Running runner in " + Thread.currentThread().getName());
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }, executor)
            .thenRun(() -> System.out.println("My long running process completed in " + Thread.currentThread().getName()))
            .thenAccept(l -> System.out.println("Running consumer in " + Thread.currentThread().getName()));

    System.out.println("Should be printed out before anything else (mostly)");
    try {
      Thread.sleep(30);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("Some minor task completed in " + Thread.currentThread().getName());
    cf.join();
  }

  /**
   * Async flavour of executeOnRunnable
   * <p>
   * Chaining runnable -> runnable -> consumer
   * Additionally test how asynchronous calls are handled synchronously (i.e. input of previous process is piped
   * synchronously to next process)
   * <p>
   * It usually doesn't make any sense to chain a function or supplier over a runnable
   */
  private void executeOnRunnableAsync(Executor executor) {
    System.out.println("======executeOnRunnableAsync=====");
    CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
      System.out.println("Running runner in " + Thread.currentThread().getName());
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }, executor)
            .thenRunAsync(() -> System.out.println("My long running process completed in " + Thread.currentThread().getName()), executor)
            .thenAcceptAsync(l -> System.out.println("Running consumer in " + Thread.currentThread().getName()), executor);

    System.out.println("Should be printed out before anything else (mostly)");
    try {
      Thread.sleep(30);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("Some minor task completed in " + Thread.currentThread().getName());
    cf.join();
  }

  /**
   * Chaining supplier -> function -> consumer
   *
   * @param executor
   */
  private void executeOnSupplier(Executor executor) {
    System.out.println("======executeOnSupplier=====");
    CompletableFuture<Void> voidCompletableFuture = CompletableFuture.supplyAsync(() -> {
      int current = ThreadLocalRandom.current().nextInt(1000);
      System.out.println("In supplier :" + Thread.currentThread().getName() + "::" + current);
      return current;
    }, executor)
            .thenApply(l -> {
              System.out.println("In Function: " + Thread.currentThread().getName());
              return l * 2;
            })
            .thenAccept(l -> {
              System.out.println("In Consumer: " + Thread.currentThread().getName());
              System.out.println("Final Value of l: " + l);
            });
    voidCompletableFuture.join();
  }

  /**
   * To test return completableFuture which is completed first from either of 2 CompletionStage
   * <p>
   * Note: Notice how cf1 is returned as final value, even after giving longer sleep time (3 ms more) than cf2.
   * This could possibly be because, invoking FJP's common-pool might take more resources than invoking Executor's pool.
   *
   * @param executor
   */
  private void executeApplyEither(Executor executor) {
    System.out.println("======executeApplyEither=====");
    CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> {
      try {
        System.out.println("Task one running :: " + Thread.currentThread().getName());
        Thread.sleep(403);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return "cf1";
    }, executor);

    CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> {
      try {
        System.out.println("Task two running :: " + Thread.currentThread().getName());
        Thread.sleep(400);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return "cf2";
    });

    CompletableFuture<String> cf3 = cf1.applyToEither(cf2, s -> s);
    String finalValue = cf3.join();

    System.out.println("Return value was::" + finalValue);
    cf3.join();
  }

  /**
   * To test return completableFuture which is completed first from any number of CompletionStage processes
   * <p>
   * To make sure that random CompletionStage tasks will be back, intentionally added 1 ms to each passing task
   *
   * @param executor
   */
  private void executeAnyOf(Executor executor) {
    System.out.println("======executeAnyOf=====");
    CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> {
      System.out.println("cf1 running :: " + Thread.currentThread().getName());
      try {
        Thread.sleep(103);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return "cf1";
    }, executor);

    CompletableFuture<String> cf2 = CompletableFuture.supplyAsync(() -> {
      System.out.println("cf2 running :: " + Thread.currentThread().getName());
      try {
        Thread.sleep(102);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return "cf2";
    }, executor);

    CompletableFuture<String> cf3 = CompletableFuture.supplyAsync(() -> {
      System.out.println("cf3 running :: " + Thread.currentThread().getName());
      try {
        Thread.sleep(101);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return "cf3";
    }, executor);

    CompletableFuture<String> cf4 = CompletableFuture.supplyAsync(() -> {
      System.out.println("cf4 running :: " + Thread.currentThread().getName());
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return "cf4";
    }, executor);

    CompletableFuture<?> cf = CompletableFuture.anyOf(cf1, cf2, cf3, cf4);

    Object finalValue = cf.join();

    System.out.println("Final Value is:: " + finalValue);
  }

  private void executeAsynchronous(Executor executor) {
    System.out.println("======executeAsynchronous=====");
    CompletableFuture<List<Integer>> cf = CompletableFuture.supplyAsync(() -> List.of(1, 2, 3, 4), executor);

    CompletableFuture<Integer> cf1 = cf.thenApply(l -> {
      System.out.println("Inside cf1::" + Thread.currentThread().getName());
      return l.size() - 1;
    });

    CompletableFuture<Integer> cf2 = cf.thenApplyAsync(l -> {
      System.out.println("Inside cf2::" + Thread.currentThread().getName());
      return l.size();
    }, executor);

    CompletableFuture<Void> cf3 = cf.thenAcceptAsync(l -> {
      System.out.println("Inside cf3::" + Thread.currentThread().getName());
    }, executor);

    // below statement doesn't make any difference due to synchronous CompletionStage task among them
    //CompletableFuture<Object> cfFinal = CompletableFuture.anyOf(cf1, cf2, cf3);

    //System.out.println(cfFinal.join());
  }
}
