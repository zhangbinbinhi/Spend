package com.spend.engine.run;

import java.io.File;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.spend.engine.consumer.FilesTranser;
import com.spend.engine.producer.FilesMonitor;
import com.spend.engine.props.SProperties;

public class SpendStart {
	/**
	 * 程序启动，只处理一次
	 */

	private static Logger logger = LogManager.getLogger();

	public static void main(String[] args) throws Exception {

		logger.info("Start! ");

		long time = System.currentTimeMillis();

		SProperties sp = SProperties.getInstance();
		Map<String, String> map = sp.getMapProperties();

		String PATH_IN = map.get("PATH_IN");
		String PATH_TMP_READ = map.get("PATH_TMP_READ");

		String PATH_TMP_PROCESS = map.get("PATH_TMP_PROCESS");

		String PATH_TMP_WRITE = map.get("PATH_TMP_WRITE");
		String PATH_OUT = map.get("PATH_OUT");
		String PATH_BAD = map.get("PATH_BAD");

		String PATH_BAK = map.get("PATH_BAK");

		String WATCH_FILE_EXTENSION = map.get("WATCH_FILE_EXTENSION");
		String SCAN_IN_SUB_FOLDER = map.get("SCAN_IN_SUB_FOLDER");
		String SCAN_WATI_TIME = map.get("SCAN_WATI_TIME");

		String PLUGIN_TRANS_PATH = map.get("PLUGIN_TRANS_PATH");
		String PLUGIN_TRANS_PACKAGE = map.get("PLUGIN_TRANS_PACKAGE");
		String PLUGIN_TRANS_CONF = map.get("PLUGIN_TRANS_CONF");

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

		LinkedBlockingQueue<File> filesQueen = new LinkedBlockingQueue<File>();

		/**
		 * 生产者 文件扫描监视器
		 */
		FilesMonitor filesMonitor = new FilesMonitor(SCAN_WATI_TIME, folderIn, folderTmpRead, filesQueen, folderBad,
				SCAN_IN_SUB_FOLDER, WATCH_FILE_EXTENSION);
		if ((PATH_BAK != null) && (!PATH_BAK.isEmpty()) && (folderBak.exists())) {
			filesMonitor.setBakFolder(folderBak);
		}
		filesMonitor.start();

		Thread.sleep(1);

		/**
		 * 消费者 文件处理
		 */
		FilesTranser filesTranser = new FilesTranser(map, filesQueen, folderTmpProcess, folderTmpWrite, folderOut, folderBad,
				PLUGIN_TRANS_PATH, PLUGIN_TRANS_PACKAGE, PLUGIN_TRANS_CONF);
		filesTranser.start();

		logger.info("Finish! ");

		long minSecond = System.currentTimeMillis() - time;
		logger.debug("Total use time: " + minSecond + " ms");
		logger.info("Total use time: " + (minSecond / 1000) + " s");

	}

}
