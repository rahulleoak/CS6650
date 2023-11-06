package org.ClientSide;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;




public class Client2 {
  private static final int initThreadGroupSize = 10;
  private static final int initAPICalls = 100;
  private static final int updatedAPICalls = 1000;
  private static final AtomicInteger totalRequests = new AtomicInteger(0);
  private static final ConcurrentLinkedQueue<Map<String, Object>> requestRecords = new ConcurrentLinkedQueue<>();

  public static CloseableHttpClient createHttpClient() {
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(5000)
        .setSocketTimeout(5000)
        .build();

    PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
    manager.setDefaultMaxPerRoute(100);
    manager.setMaxTotal(500);
    return HttpClients.custom().setConnectionManager(manager).disableAutomaticRetries().setDefaultRequestConfig(requestConfig).build();
  }

  public static void threadStart(ExecutorService executor, RequestHandler requestHandler, File imageFile) {
    for (int i = 0; i < initThreadGroupSize; i++) {
      executor.execute(() -> {
        for (int j = 0; j < initAPICalls; j++) {
          sendRequests("POST", requestHandler, imageFile);
          sendRequests("GET", requestHandler, imageFile);
        }
      });
    }
    executor.shutdown();
    while (!executor.isTerminated()){
    }
  }

  public static void testThreads(RequestHandler requestHandler,
      File imageFile, int delay) {

    for (int k = 0; k < updatedAPICalls; k++) {
      totalRequests.addAndGet(sendRequests("POST", requestHandler, imageFile));
      totalRequests.addAndGet(sendRequests("GET", requestHandler, imageFile));
    }
  }

  public static int sendRequests(String requestType, RequestHandler requestHandler, File imageFile) {
    boolean successfulRequest = false;
    int retryCount = 1;
    int statusCode = -1;
    long start = System.currentTimeMillis();
    long backoffTime = 1000;
    // 5 represents the maximum number of retries
    while (!successfulRequest && retryCount < 6) {
      try {
        if ("GET".equals(requestType)) {
          String albumID = requestHandler.getLastAlbumID();;
          if (albumID != null) {
            statusCode = requestHandler.sendGetRequest(albumID);
          }
        } else if ("POST".equals(requestType)) {
          statusCode = requestHandler.sendPostRequest("Rock Pistols", "Never Mind The Bollocks!", "1977", imageFile);
        }
        successfulRequest = true;

        long end = System.currentTimeMillis();
        long latency = end - start;

        Map<String, Object> requestRecord = new HashMap<>();
        requestRecord.put("startTime", start);
        requestRecord.put("requestType", requestType);
        requestRecord.put("latency", latency);
        requestRecord.put("responseCode", statusCode);

        requestRecords.offer(requestRecord);
      } catch (Exception e) {
        e.printStackTrace();
        try { // Implement exponential backoff
          Thread.sleep(backoffTime); // Wait for the backoff period before retrying
        } catch (InterruptedException ie){
          Thread.currentThread().interrupt(); // Reset the interrupted status
          break;  // Exit the retry loop if the thread is interrupted
        }
        backoffTime *= 2; // Double the backoff time for the next retry
        retryCount++;
      }
    }
    return retryCount;
  }

  public static void writeToCSV(String fileName) {
    StringBuilder sb = new StringBuilder();
    sb.append("Start Time (ms),Request Type,Latency (ms),Response Code\n");

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

  public static void statsPerRequestType(String requestType){
    List<Long> latencies = requestRecords.stream()
        .filter(record -> record.get("requestType").equals(requestType))
        .map(record -> (Long) record.get("latency"))
        .sorted()
        .collect(Collectors.toList());

    long sum = 0;
    long min = latencies.get(0);
    long max = latencies.get(latencies.size() - 1);

    for (long latency : latencies) {
      sum += latency;
    }

    long mean = sum / latencies.size();
    long median = calculateMedian(latencies);
    long p99 = calculateP99(latencies);

    System.out.println("--------------------------------------------");
    System.out.println("Statistics for " + requestType + " requests:");
    System.out.println("Mean latency: " + mean + " ms");
    System.out.println("Median latency: " + median + " ms");
    System.out.println("99th percentile latency: " + p99 + " ms");
    System.out.println("Min latency: " + min + " ms");
    System.out.println("Max latency: " + max + " ms");
    System.out.println("--------------------------------------------");

  }

  public static void LoadTestingStats() {
    synchronized (requestRecords) { // Synchronize access to the shared list for reading
      statsPerRequestType("GET");
      statsPerRequestType("POST");
    }
  }

  public static long calculateMedian(List<Long> responseTimes) {
    int middle = responseTimes.size() / 2;
    if (responseTimes.size() % 2 == 1) {
      return responseTimes.get(middle);
    } else {
      long left = responseTimes.get(middle - 1);
      long right = responseTimes.get(middle);
      return (left + right) / 2;
    }
  }

  public static long calculateP99(List<Long> responseTimes) {
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
    RequestHandler requestHandler = new RequestHandler(httpClient, IPAddr);
    File imageFile = new File("src/main/resources/nmtb.png");

    ExecutorService addExecutor = Executors.newFixedThreadPool(threadGroupSize);
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < numThreadGroups; i++) {
      addExecutor.execute(() -> {
        ExecutorService executor1 = Executors.newFixedThreadPool(threadGroupSize);
        for (int j = 0; j < threadGroupSize; j++) {
          executor1.execute(() -> {
            testThreads(requestHandler, imageFile, delay);
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
    //new add
    try {
      // Wait for previously submitted tasks to complete execution
      if (!addExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
        addExecutor.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!addExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
          System.err.println("Pool did not terminate");
        }
      }
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      addExecutor.shutdownNow();
    }

    long endTime = System.currentTimeMillis();
    long wallTime = (endTime - startTime) / 1000;
    System.out.println("Total Requests: " + totalRequests);
    int expectedRequestCount = numThreadGroups * threadGroupSize *updatedAPICalls *2;
    int failedRequests = totalRequests.get() - expectedRequestCount;
    int successRequest = expectedRequestCount - failedRequests;
    System.out.println("Successful requests: " + successRequest);
    System.out.println("Failed Requests: " + failedRequests);
    double throughput = (double) successRequest / wallTime;

    writeToCSV("records.csv");
    System.out.println("\nWall Time: " + wallTime + " seconds");
    System.out.println("Throughput: " + throughput + " requests per second");
    LoadTestingStats();
  }
}

