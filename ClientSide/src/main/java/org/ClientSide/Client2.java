package org.ClientSide;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;




public class Client2 {
  private static final int initThreadGroupSize = 10;
  private static final int initAPICalls = 100;
  private static final int updatedAPICalls = 1000;
  private static final AtomicInteger totalRequests = new AtomicInteger(0);
  private static final List<Map<String, Object>> requestRecords = new ArrayList<>();

  public static CloseableHttpClient createHttpClient() {
    PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
    manager.setDefaultMaxPerRoute(100);
    manager.setMaxTotal(100);
    return HttpClients.custom().setConnectionManager(manager).disableAutomaticRetries().build();
  }

  public static void startInitialThreads(ExecutorService executor, RequestHandler requestHandler, File imageFile) {
    for (int i = 0; i < initThreadGroupSize; i++) {
      executor.execute(() -> {
        for (int j = 0; j < initAPICalls; j++) {
          performRequests("POST", requestHandler, imageFile);
          performRequests("GET", requestHandler, imageFile);
        }
      });
    }
    executor.shutdown();
    while (!executor.isTerminated()){
    }
  }

  public static void startAdditionalThreads(RequestHandler requestHandler,
      File imageFile, int delay) {

    for (int k = 0; k < updatedAPICalls; k++) {
      totalRequests.addAndGet(performRequests("POST", requestHandler, imageFile));
      totalRequests.addAndGet(performRequests("GET", requestHandler, imageFile));
    }
  }

  private static int performRequests(String requestType, RequestHandler requestHandler, File imageFile) {
    boolean requestSuccessful = false;
    int retryCount = 1;
    int statusCode = -1;
    long start = System.currentTimeMillis();
    // 5 represents the maximum number of retries
    while (!requestSuccessful && retryCount < 5) {
      try {
        if ("GET".equals(requestType)) {
          statusCode = requestHandler.sendGetRequest("1");
        } else if ("POST".equals(requestType)) {
          statusCode = requestHandler.sendPostRequest("Sex Pistols", "Never Mind The Bollocks!", "1977", imageFile);
        }
        requestSuccessful = true;

        long end = System.currentTimeMillis();
        long latency = end - start;

        Map<String, Object> requestRecord = new HashMap<>();
        requestRecord.put("startTime", start);
        requestRecord.put("requestType", requestType);
        requestRecord.put("latency", latency);
        requestRecord.put("responseCode", statusCode);

        requestRecords.add(requestRecord);
      } catch (Exception e) {
        e.printStackTrace();
        retryCount++;
      }
    }
    return retryCount;
  }

  private static void writeToCSV(String fileName) {
    StringBuilder sb = new StringBuilder();
    sb.append("Start Time,Request Type,Latency (ms),Response Code\n");

    for (Map<String, Object> requestRecord : requestRecords) {
      sb.append(requestRecord.get("startTime")).append(",")
          .append(requestRecord.get("requestType")).append(",")
          .append(requestRecord.get("latency")).append(",")
          .append(requestRecord.get("responseCode")).append("\n");
    }

    try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
      writer.write(sb.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void calculateAndDisplayStatistics() {
    long sum = 0;
    long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
    List<Long> responseTimes = new ArrayList<>();

    for (Map<String, Object> requestRecord : requestRecords) {
      responseTimes.add((Long) requestRecord.get("latency"));
    }

    for (long time : responseTimes) {
      sum += time;
      min = Math.min(min, time);
      max = Math.max(max, time);
    }

    Collections.sort(responseTimes);
    long mean = sum / responseTimes.size();
    long median = calculateMedian(responseTimes);
    long p99 = calculateP99(responseTimes);

    System.out.println("Mean response time: " + mean + " milliseconds");
    System.out.println("Median response time: " + median + " milliseconds");
    System.out.println("p99 response time: " + p99 + " milliseconds");
    System.out.println("Min response time: " + min + " milliseconds");
    System.out.println("Max response time: " + max + " milliseconds");
  }

  private static long calculateMedian(List<Long> responseTimes) {
    int middle = responseTimes.size() / 2;
    if (responseTimes.size() % 2 == 1) {
      return responseTimes.get(middle);
    } else {
      long left = responseTimes.get(middle - 1);
      long right = responseTimes.get(middle);
      return (left + right) / 2;
    }
  }

  private static long calculateP99(List<Long> responseTimes) {
    int index = (int) Math.ceil(99 / 100.0 * responseTimes.size()) - 1;
    return responseTimes.get(index);
  }

  public static void main(String[] args) {
    if (args.length != 4) {
      System.out.println("Invalid args! " +
          "Arguments should be:  <threadGroupSize> <numThreadGroups> <delay> <IPAddr>");
      System.exit(1);
    }
    int threadGroupSize = Integer.parseInt(args[0]);
    int numThreadGroups = Integer.parseInt(args[1]);
    int delay = Integer.parseInt(args[2]);
    String IPAddr = args[3];

    CloseableHttpClient httpClient = createHttpClient();

    ExecutorService executor = Executors.newFixedThreadPool(initThreadGroupSize);
    RequestHandler requestHandler = new RequestHandler(httpClient, IPAddr);
    File imageFile = new File("src/main/resources/nmtb.png");

    startInitialThreads(executor, requestHandler, imageFile);

    ExecutorService addExecutor = Executors.newFixedThreadPool(threadGroupSize);
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < numThreadGroups; i++) {
      addExecutor.execute(() -> {
        ExecutorService executor1 = Executors.newFixedThreadPool(threadGroupSize);
        for (int j = 0; j < threadGroupSize; j++) {
          executor1.execute(() -> {
            //System.out.println(Thread.currentThread().getName());
            startAdditionalThreads(requestHandler, imageFile, delay);
          });
        }
        executor1.shutdown();
        while (!executor1.isTerminated()) {
        }
      });
      try {
        Thread.sleep(delay * 1000L);
      } catch (InterruptedException ex) {
        ex.printStackTrace();
      }
    }
    addExecutor.shutdown();
    while (!addExecutor.isTerminated()) {
    }

    long endTime = System.currentTimeMillis();
    long wallTime = (endTime - startTime) / 1000;
    double throughput = (double) totalRequests.get() / wallTime;

    writeToCSV("records.csv");
    System.out.println("\nWall Time: " + wallTime + " seconds");
    System.out.println("Throughput: " + throughput + " requests per second");
    calculateAndDisplayStatistics();
  }
}

