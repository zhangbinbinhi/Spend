package com.spend.engine.run;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.spend.engine.consumer.FilesTranser;
import com.spend.engine.pool.ThreadPool;
import com.spend.engine.producer.FilesMonitor;
import com.spend.engine.props.SProperties;

public class SpendRun {
	/**
	 * 程序启动，作为进程一直处理
	 */

	private static Logger logger = LogManager.getLogger();

	public static void main(String[] args) throws Exception {

		logger.info("START !");

		logger.debug("START TIME: " + System.currentTimeMillis());

		SProperties sp = SProperties.getInstance();
		Map<String, String> map = sp.getMapProperties();

		String PATH_IN = map.get("PATH_IN");
		String PATH_TMP_READ = map.get("PATH_TMP_READ");
		String PATH_TMP_PROCESS = map.get("PATH_TMP_PROCESS");
		String PATH_TMP_WRITE = map.get("PATH_TMP_WRITE");
		String PATH_OUT = map.get("PATH_OUT");
		String PATH_BAD = map.get("PATH_BAD");
		String PATH_BAK = map.get("PATH_BAK");
		String SCAN_WATI_TIME = map.get("SCAN_WATI_TIME");
		String WATCH_FILE_EXTENSION = map.get("WATCH_FILE_EXTENSION");
		String SCAN_IN_SUB_FOLDER = map.get("SCAN_IN_SUB_FOLDER");
		String PLUGIN_TRANS_PATH = map.get("PLUGIN_TRANS_PATH");
		String PLUGIN_TRANS_PACKAGE = map.get("PLUGIN_TRANS_PACKAGE");
		String PLUGIN_TRANS_CONF = map.get("PLUGIN_TRANS_CONF");
		String THREAD_CONSUMEN_NUM = map.get("THREAD_CONSUMEN_NUM");

		String CORE_POOL_SIZE = map.get("CORE_POOL_SIZE");
		String MAXIMUM_POOL_SIZE = map.get("MAXIMUM_POOL_SIZE");
		String KEEP_ALIVE_TIME = map.get("KEEP_ALIVE_TIME");

		File folderIn = new File(PATH_IN);
		File folderTmpRead = new File(PATH_TMP_READ);
		File folderTmpProcess = new File(PATH_TMP_PROCESS);
		File folderTmpWrite = new File(PATH_TMP_WRITE);
		File folderOut = new File(PATH_OUT);
		File folderBad = new File(PATH_BAD);
		File folderBak = new File(PATH_BAK);

		if (!folderIn.exists()) {
			folderIn.mkdirs();
		}
		// 读取文件临时目录
		if (!folderTmpRead.exists()) {
			folderTmpRead.mkdirs();
		}

		// 处理文件临时目录
		if (!folderTmpProcess.exists()) {
			folderTmpProcess.mkdirs();
		}

		// 输出文件临时目录
		if (!folderTmpWrite.exists()) {
			folderTmpWrite.mkdirs();
		}

		// 数据输出目录
		if (!folderOut.exists()) {
			folderOut.mkdirs();
		}

		// 错误数据备份目录
		if (!folderBad.exists()) {
			folderBad.mkdirs();
		}

		// 数据全量备份目录
		if (!folderBak.exists()) {
			folderBak.mkdirs();
		}

		BlockingQueue<File> filesQueen = new ArrayBlockingQueue<File>(10240);

		/**
		 * 消费者 文件处理
		 */
		int threadCousumerNum = 1;
		try {
			threadCousumerNum = Integer.valueOf(THREAD_CONSUMEN_NUM);
		} catch (Exception e) {
			logger.error(e.getMessage());
			threadCousumerNum = 1;
		}
		if (threadCousumerNum < 1) {
			threadCousumerNum = 1;
		}

		List<ThreadPool> listThreadPoolCousumer = new ArrayList<ThreadPool>();
		for (int i = 0; i < threadCousumerNum; i++) {
			FilesTranser filesTranser = new FilesTranser(map, filesQueen, folderTmpProcess, folderTmpWrite, folderOut,
					folderBad, PLUGIN_TRANS_PATH, PLUGIN_TRANS_PACKAGE, PLUGIN_TRANS_CONF);
			ThreadPool threadFilesTranser = new ThreadPool(filesTranser);
			listThreadPoolCousumer.add(threadFilesTranser);
		}

		/**
		 * 生产者 文件扫描监视器
		 */
		FilesMonitor filesMonitor = new FilesMonitor(SCAN_WATI_TIME, folderIn, folderTmpRead, filesQueen, folderBad,
				SCAN_IN_SUB_FOLDER, WATCH_FILE_EXTENSION);

		if ((PATH_BAK != null) && (!PATH_BAK.isEmpty()) && (folderBak.exists())) {
			filesMonitor.setBakFolder(folderBak);
		}

		ThreadPool threadFilesMonitor = new ThreadPool(filesMonitor);

		int corePoolSize = 16;
		int maximumPoolSize = 512;
		long keepAliveTime = 3600;
		
		if (CORE_POOL_SIZE != null) {
			corePoolSize = Integer.valueOf(CORE_POOL_SIZE);
		}
		if (MAXIMUM_POOL_SIZE != null) {
			maximumPoolSize = Integer.valueOf(MAXIMUM_POOL_SIZE);
		}
		if (KEEP_ALIVE_TIME != null) {
			keepAliveTime = Integer.valueOf(KEEP_ALIVE_TIME);
		}

		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime,
				TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());

		for (int i = 0; i < listThreadPoolCousumer.size(); i++) {
			threadPoolExecutor.execute(listThreadPoolCousumer.get(i));
		}

		threadPoolExecutor.execute(threadFilesMonitor);

	}

}
