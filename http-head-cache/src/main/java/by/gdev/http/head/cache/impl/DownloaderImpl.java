/**
 * 
 */
package by.gdev.http.head.cache.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;

import com.google.common.eventbus.EventBus;

import by.gdev.http.head.cache.exeption.StatusExeption;
import by.gdev.http.head.cache.model.downloader.DownloadElement;
import by.gdev.http.head.cache.model.downloader.DownloaderContainer;
import by.gdev.http.head.cache.model.downloader.DownloaderStatus;
import by.gdev.http.head.cache.model.downloader.DownloaderStatusEnum;
import by.gdev.http.head.cache.service.Downloader;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Robert Makrytski This class is responsible for the state of the file
 *         upload
 */

@Slf4j
@Data
@AllArgsConstructor
public class DownloaderImpl implements Downloader {
	/**
	 * Path to download file
	 */
	private String pathToDownload;
	private EventBus eventBus;
	private CloseableHttpClient httpclient;
	private RequestConfig requestConfig;
	/**
	 * Put new elements to download them.
	 */
	private Queue<DownloadElement> downloadElements = new ConcurrentLinkedQueue<>();
	/**
	 * Put processed elements after process to get info about status
	 */
	private List<DownloadElement> processedElements = Collections.synchronizedList(new ArrayList<DownloadElement>());
	/**
	 * Shown status of the downloading
	 */
	private List<Long> allConteinerSize = new ArrayList<Long>();
	private volatile DownloaderStatusEnum status;
	private DownloadRunnableImpl runnable;
	private volatile Integer allCountElement;
//	private volatile long fullDownloadSize;
	private long fullDownloadSize1;
	private long downloadBytesNow1;
	private LocalTime start;


	public DownloaderImpl(EventBus eventBus, CloseableHttpClient httpclient, RequestConfig requestConfig) {
		this.eventBus = eventBus;
		this.httpclient = httpclient;
		this.requestConfig = requestConfig;
		status = DownloaderStatusEnum.IDLE;
		start = LocalTime.now();
		runnable = new DownloadRunnableImpl(downloadElements, processedElements, httpclient, requestConfig);
	}

	@Override
	public void addContainer(DownloaderContainer container) {
		if (Objects.nonNull(container.getRepo().getResources())) {
			container.getRepo().getResources().forEach(metadata -> {
				DownloadElement element = new DownloadElement();
				element.setMetadata(metadata);
				element.setRepo(container.getRepo());
				element.setPathToDownload(container.getDestinationRepositories());
				element.setHandlers(container.getHandlers());
				downloadElements.add(element);
			});
		}
//		fullDownloadSize = totalSize(downloadElements);
		allConteinerSize.add(container.getContainerSize());
		pathToDownload = container.getDestinationRepositories();
	}

	@Override
	public void startDownload(boolean sync) throws InterruptedException, ExecutionException, StatusExeption, IOException {
		fullDownloadSize1 = totalSize1(allConteinerSize);
		if (status.equals(DownloaderStatusEnum.IDLE) || status.equals(DownloaderStatusEnum.CANCEL)) {
			status = DownloaderStatusEnum.WORK;
			runnable.setStatus(status);
			allCountElement = downloadElements.size();
			List<CompletableFuture<Void>> listThread = new ArrayList<>();
			for (int i = 0; i < 3; i++)
				listThread.add(CompletableFuture.runAsync(runnable));
			if (sync) {
				waitThreadDone(listThread);
			} else {
				CompletableFuture.runAsync(() -> {
					try {
						waitThreadDone(listThread);
					} catch (IOException|InterruptedException e) {
						log.error("Error", e);
					}
				}).get();
			}
		} else
			throw new StatusExeption(status.toString());
	}

	@Override
	public void cancelDownload() {
		status = DownloaderStatusEnum.CANCEL;
		runnable.setStatus(DownloaderStatusEnum.CANCEL);
	}

	private DownloaderStatus averageSpeed() throws IOException {
		DownloaderStatus statusDownload = new DownloaderStatus();
		long downloadBytesNow = 0;
		List<DownloadElement> list = new ArrayList<DownloadElement>(processedElements);
		double thirty = Duration.between(start, LocalTime.now()).getSeconds();
		for (DownloadElement elem : list) {
			downloadBytesNow += elem.getDownloadBytes();
//			statusDownload.setDownloadSize(downloadBytesNow);
//			statusDownload.setDownloadSize(sizeDownloadNow());
		}
		statusDownload.setDownloadSize(sizeDownloadNow());
		statusDownload.setSpeed((downloadBytesNow/1048576) / thirty);
		statusDownload.setDownloaderStatusEnum(status);
		statusDownload.setAllDownloadSize(fullDownloadSize1);
		statusDownload.setLeftFiles(processedElements.size());
		statusDownload.setAllFiles(allCountElement);
		return statusDownload;
	}

	private void waitThreadDone(List<CompletableFuture<Void>> listThread) throws InterruptedException, IOException {
		LocalTime start = LocalTime.now();
		LocalTime test = LocalTime.now().plusSeconds(1);
		boolean workedAnyThread = true;
		while (workedAnyThread) {
			workedAnyThread = false;
			Thread.sleep(50);
			workedAnyThread = listThread.stream().anyMatch(e -> !e.isDone());
			if (start.isBefore(LocalTime.now())) {
				start = start.plusSeconds(1);
				if (allCountElement != 0) {
					if (start.getSecond() != test.getSecond())
					eventBus.post(averageSpeed());
				}
			}
		}
	}

//	private long totalSize(Queue<DownloadElement> downloadElements) {
//		List<Long> sizeList = new ArrayList<Long>();
//		downloadElements.forEach(size -> {
//			sizeList.add(size.getMetadata().getSize());
//		});
//		long sum = 0;
//		for (long l : sizeList) {
//			sum += l;
//		}
//		return sum;
//	}
	
	private long totalSize1(List<Long> containerSize) {
		long sum = 0;
		for (long l : containerSize)
			sum += l;
		return sum;
	}
	
	private long sizeDownloadNow() throws IOException {
		return Files.walk(Paths.get(pathToDownload)).filter(p -> p.toFile().isFile()).mapToLong(p -> p.toFile().length()).sum();
	}
}