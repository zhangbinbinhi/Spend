package com.spend.engine.producer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FilesMonitor implements Runnable {

	/**
	 * 生产者 文件扫描器
	 **/

	private static boolean flagScanWithSub = false;

	private static Logger logger = LogManager.getLogger();

	private int scanWaitTime;

	private File folderIn;
	private File folderTmpRead;
	private String folderTmpPath;
	// 数据全量备份路径
	private File bakFolder;
	private String bakPath;
	private File folderBad;
	private boolean flagBak = false;

	public BlockingQueue<File> filesQueen;

	private static String[] watchFileExtension;

	public FilesMonitor(String SCAN_WATI_TIME, File folderIn, File folderTmpRead, BlockingQueue<File> filesQueen,
			File folderBad, String SCAN_IN_SUB_FOLDER, String WATCH_FILE_EXTENSION) {
		if (SCAN_WATI_TIME != null && !SCAN_WATI_TIME.isEmpty()) {
			int scanWaitTime = 1;
			try {
				scanWaitTime = Integer.valueOf(SCAN_WATI_TIME);
			} catch (Exception e) {
				logger.error(e.getMessage());
				scanWaitTime = 1;
			}
			this.scanWaitTime = scanWaitTime;
		} else {
			this.scanWaitTime = 1;
		}

		this.folderIn = folderIn;
		this.folderTmpRead = folderTmpRead;
		this.folderTmpPath = folderTmpRead.getAbsolutePath();
		this.folderBad = folderBad;

		this.filesQueen = filesQueen;

		if (WATCH_FILE_EXTENSION != null && !WATCH_FILE_EXTENSION.isEmpty()) {
			watchFileExtension = WATCH_FILE_EXTENSION.split(",");
		}

		if (SCAN_IN_SUB_FOLDER != null && !SCAN_IN_SUB_FOLDER.isEmpty()) {
			if (SCAN_IN_SUB_FOLDER.toLowerCase().equals("true")) {
				flagScanWithSub = true;
			} else {
				flagScanWithSub = false;
			}
		} else {
			flagScanWithSub = false;
		}

		if (!folderTmpRead.exists()) {
			folderTmpRead.mkdirs();
		}
		if (!folderIn.exists()) {
			folderIn.mkdirs();
		}
	}

	public void setBakFolder(File folderBak) {
		this.bakFolder = folderBak;
		this.bakPath = folderBak.getAbsolutePath();
		if (this.bakFolder.isDirectory()) {
			this.flagBak = true;
		}
	}

	// 如果设定要备份，则备份
	public void bakFile(File file) {
		if (flagBak) {
			String fileName = file.getName();
			File destFile = new File(bakPath + File.separator + fileName);
			if (destFile.exists()) {
				FileUtils.deleteQuietly(destFile);
			}
			try {
				FileUtils.copyFile(file, destFile);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
	}

	@Override
	public void run() {
		List<File> listTmpFiles = scan(folderTmpRead);
		for (File file : listTmpFiles) {
			try {
				filesQueen.put(file);
				logger.info("Scan file : " + file.getName());
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		}

		while (true) {
			try {
				monitor();
				Thread.sleep(scanWaitTime * 1000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		}

	}

	public void start() {
		// 只执行一次的方法
		try {
			List<File> listTmpFiles = scan(folderTmpRead);
			for (File file : listTmpFiles) {
				logger.info("Scan file: " + file.getName());
				synchronized (this) {
					filesQueen.put(file);
				}
			}
			monitor();
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
	}

	private void monitor() {
		logger.debug("WATCH TIME: " + System.currentTimeMillis());
		List<File> listFiles = scan(folderIn);
		for (File file : listFiles) {
			File fileTmp = new File(folderTmpPath + File.separator + file.getName());
			if (filesQueen.contains(fileTmp)) {
				logger.debug("Same file! Detail: " + fileTmp.getName());
				continue;
			} else {
				try {
					FileUtils.moveFile(file, fileTmp);
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
				try {
					filesQueen.put(fileTmp);
					logger.info("Scan file : " + file.getName());
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void monitorSYN() {
		logger.debug("WATCH TIME: " + System.currentTimeMillis());
		List<File> listFiles = scan(folderIn);
		for (File file : listFiles) {
			File fileTmp = new File(folderTmpPath + File.separator + file.getName());
			synchronized (filesQueen) {
				if (filesQueen.contains(fileTmp)) {
					logger.debug("Same file! Detail: " + fileTmp.getName());
					continue;
				} else {
					logger.info("Scan file: " + file.getName());
					try {
						FileUtils.moveFile(file, fileTmp);
					} catch (IOException e) {
						logger.error(e.getMessage());
					}
					try {
						filesQueen.put(fileTmp);
					} catch (InterruptedException e) {
						logger.error(e.getMessage());
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private synchronized void monitorWait() {
		logger.debug("WATCH TIME: " + System.currentTimeMillis());
		List<File> listFiles = scan(folderIn);
		for (File file : listFiles) {
			File fileTmp = new File(folderTmpPath + File.separator + file.getName());
			while (filesQueen.contains(fileTmp)) {
				// 可以加一个机制，当经过N次扫描，发现此文件依然存在，则跳过这个文件！不要等待
				try {
					logger.warn("Already exist file " + file.getName());
					this.notifyAll();
					this.wait();
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}
			}
			logger.info("Scan file: " + file.getName());
			try {
				FileUtils.moveFile(file, fileTmp);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
			try {
				filesQueen.put(fileTmp);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		}
	}

	public List<File> scan(File folder) {
		// 扫描的总控方法，包含对是否扫描子目录，是否只扫描监控文件的处理
		List<File> listFiles;
		if (flagScanWithSub) {
			listFiles = scanFilesWithSub(folder);
		} else {
			listFiles = scanFiles(folder);
		}
		for (File file : listFiles) {
			bakFile(file);
		}
		if (watchFileExtension != null && watchFileExtension.length > 0) {
			List<File> listFilesSee = new ArrayList<File>();
			for (int i = 0; i < listFiles.size(); i++) {
				File file = listFiles.get(i);
				String fileName = file.getName();
				String extension = FilenameUtils.getExtension(fileName);
				boolean flag = false;
				for (String watchExtension : watchFileExtension) {
					if (extension.toLowerCase().equals(watchExtension.toLowerCase())) {
						flag = true;
					}
				}
				if (flag) {
					listFilesSee.add(file);
				} else {
					File destFile = new File(folderBad.getAbsolutePath() + File.separator + file.getName());
					if (destFile.exists()) {
						FileUtils.deleteQuietly(destFile);
					}
					try {
						FileUtils.moveFile(file, destFile);
					} catch (IOException e) {
						logger.error(e.getMessage());
					}
				}
			}
			return listFilesSee;
		} else {
			return listFiles;
		}
	}

	public static List<File> scanFiles(File folder) {
		List<File> listFiles = new ArrayList<File>();
		if (!folder.exists()) {
			return listFiles;
		}
		if (folder.isDirectory()) {
			File[] files = folder.listFiles();
			for (File file : files) {
				if (file.isFile()) {
					listFiles.add(file);
				}
			}
		}
		return listFiles;
	}

	public static List<File> scanFilesWithSub(File file) {
		List<File> listFiles = new LinkedList<File>();
		if (!file.exists()) {
			return listFiles;
		}
		if (file.isDirectory()) {
			for (File subFile : file.listFiles()) {
				if (subFile.isDirectory()) {
					listFiles.addAll(scanFilesWithSub(subFile));
				} else {
					listFiles.add(subFile);
				}
			}
		} else {
			listFiles.add(file);
		}
		return listFiles;
	}

}
