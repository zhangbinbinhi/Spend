package com.spend.engine.consumer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.spend.engine.producer.FilesMonitor;
import com.spend.trans.iface.TransData;

public class FilesTranser implements Runnable {
	/**
	 * 消费者
	 **/

	private static Logger logger = LogManager.getLogger();

	Map<String, String> mapSysInfo;

	public BlockingQueue<File> filesQueen;

	// 处理临时路径的绝对路径值
	private String pathTmpProcess;
	// 处理的临时目录
	private File folderTmpProcess;

	// 输出的临时目录
	private File folderTmpWrite;

	private File folderOut;
	private File folderBad;

	private String pluginTransPath;
	private String pluginTransPackage;
	private String pluginTransConf;

	private File filePluginTransPath;
	private File filePluginTransConf;

	private TransData transData;

	public FilesTranser(Map<String, String> mapSysInfo, BlockingQueue<File> filesQueen, File folderTmpProcess,
			File folderTmpWrite, File folderOut, File folderBad, String PLUGIN_TRANS_PATH, String PLUGIN_TRANS_PACKAGE,
			String PLUGIN_TRANS_CONF) {

		this.mapSysInfo = mapSysInfo;

		this.filesQueen = filesQueen;
		this.pathTmpProcess = folderTmpProcess.getAbsolutePath();
		this.folderTmpProcess = folderTmpProcess;
		this.folderTmpWrite = folderTmpWrite;
		this.folderOut = folderOut;
		this.folderBad = folderBad;
		if (!folderTmpProcess.exists()) {
			this.folderTmpProcess.mkdirs();
		}
		if (!folderTmpWrite.exists()) {
			this.folderTmpWrite.mkdirs();
		}
		if (!folderOut.exists()) {
			this.folderOut.mkdirs();
		}
		if (!folderBad.exists()) {
			this.folderBad.mkdirs();
		}
		this.transData = initTransData(PLUGIN_TRANS_PATH, PLUGIN_TRANS_PACKAGE, PLUGIN_TRANS_CONF);
		if (transData == null) {
			logger.error("Load plugin TransData failed! Detail: " + this.transData);
			System.exit(1);
		} else {
			logger.info("Load plugin TransData success! Detail: " + this.transData);
		}
		// 清除程序的临时目录的数据
		List<File> listFilesProcessTmp = FilesMonitor.scanFilesWithSub(folderTmpProcess);
		for (File file : listFilesProcessTmp) {
			String fileName = file.getName();
			File fileDest = new File(folderBad.getAbsolutePath() + File.separator + fileName);
			if (fileDest.exists()) {
				FileUtils.deleteQuietly(fileDest);
			}
			try {
				FileUtils.moveFile(file, fileDest);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
		List<File> listFilesWriteTmp = FilesMonitor.scanFilesWithSub(folderTmpWrite);
		for (File file : listFilesWriteTmp) {
			String fileName = file.getName();
			File fileDest = new File(folderBad.getAbsolutePath() + File.separator + fileName);
			if (fileDest.exists()) {
				FileUtils.deleteQuietly(fileDest);
			}
			try {
				FileUtils.moveFile(file, fileDest);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
	}

	private TransData initTransData(String PLUGIN_TRANS_PATH, String PLUGIN_TRANS_PACKAGE, String PLUGIN_TRANS_CONF) {
		// 此模式需要精确到jar包的绝对路径,只加载指定的一个插件
		TransData transData = null;
		// pluginTransPath指jar包的路径
		this.pluginTransPath = PLUGIN_TRANS_PATH == null ? new String() : PLUGIN_TRANS_PATH;
		this.pluginTransConf = PLUGIN_TRANS_CONF == null ? new String() : PLUGIN_TRANS_CONF;
		this.pluginTransPackage = PLUGIN_TRANS_PACKAGE == null ? new String() : PLUGIN_TRANS_PACKAGE;

		if (this.pluginTransPath.isEmpty()) {
			logger.debug("加载本工程的插件");
			try {
				Class<?> cls = Class.forName(pluginTransPackage);
				Object obj = cls.newInstance();
				if (obj instanceof TransData) {
					transData = (TransData) obj;
				} else {
					logger.error("The class in the package not instanceof TransData! Detail: " + pluginTransPackage);
					System.exit(1);
				}
			} catch (ClassNotFoundException e) {
				logger.error(e.toString());
			} catch (InstantiationException e) {
				logger.error(e.toString());
			} catch (IllegalAccessException e) {
				logger.error(e.toString());
			}
		} else {
			logger.debug("加载指定jar包的插件！路径：" + pluginTransPath);
			this.filePluginTransPath = new File(pluginTransPath);
			if (filePluginTransPath.exists()) {
				try {
					URL url = (filePluginTransPath).toURI().toURL();
					URL[] listURL = new URL[] { url };
					URLClassLoader loader = new URLClassLoader(listURL);
					Class<?> cls = loader.loadClass(pluginTransPackage);
					Object obj = cls.newInstance();
					if (obj instanceof TransData) {
						transData = (TransData) obj;
					} else {
						logger.error(
								"The class in the package not instanceof TransData! Detail: " + pluginTransPackage);
						System.exit(1);
					}
					loader.close();
				} catch (ClassNotFoundException e) {
					logger.error(e.toString());
				} catch (InstantiationException e) {
					logger.error(e.toString());
				} catch (IllegalAccessException e) {
					logger.error(e.toString());
				} catch (MalformedURLException e) {
					logger.error(e.toString());
				} catch (IOException e) {
					logger.error(e.toString());
				}
			} else {
				logger.error("Plugin path can't find! Detail: " + pluginTransPath);
				filePluginTransPath.getParentFile().mkdirs();
			}
		}

		if (pluginTransConf.isEmpty()) {
			logger.info("Plugin conf is empty! ");
		} else {
			this.filePluginTransConf = new File(pluginTransConf);
			if (!filePluginTransConf.exists()) {
				logger.warn("Plugin conf can't find! Detail: " + pluginTransConf);
				filePluginTransConf.getParentFile().mkdirs();
			}
		}
		if (transData == null) {
			logger.error("Can't get Trans plugin in path ! Detail " + PLUGIN_TRANS_PATH);
		} else {
			transData.init(filePluginTransConf);
			// transData.init(this.mapSysInfo, filePluginTransConf);
		}
		return transData;
	}

	@SuppressWarnings("unused")
	private TransData initTransDataWithJarPath(String PLUGIN_TRANS_PATH, String PLUGIN_TRANS_PACKAGE,
			String PLUGIN_TRANS_CONF) {
		// 此模式是指定jar包所在文件夹，下面所有插件都加载
		TransData transData = null;
		// pluginTransPath所在目录的路径
		this.pluginTransPath = PLUGIN_TRANS_PATH;
		this.pluginTransConf = PLUGIN_TRANS_CONF;

		this.pluginTransPackage = PLUGIN_TRANS_PACKAGE;

		if (this.pluginTransPath.isEmpty()) {
			logger.debug("加载本工程的插件！");
			try {
				Class<?> cls = Class.forName(pluginTransPackage);
				Object obj = cls.newInstance();
				if (obj instanceof TransData) {
					transData = (TransData) obj;
				} else {
					logger.error("The class in the package not instanceof TransData! Detail: " + pluginTransPackage);
					System.exit(1);
				}
			} catch (ClassNotFoundException e) {
				logger.error(e.toString());
			} catch (InstantiationException e) {
				logger.error(e.toString());
			} catch (IllegalAccessException e) {
				logger.error(e.toString());
			}
		} else {
			logger.debug("加载指定jar包路径下的所有的插件！路径：" + pluginTransPath);
			this.filePluginTransPath = new File(pluginTransPath);
			if (filePluginTransPath.exists()) {
				try {
					URL[] listURL = null;
					String[] listFile = filePluginTransPath.list();
					if (listFile == null) {
						logger.error("Can't get Trans plugin in path ! Detail " + PLUGIN_TRANS_PATH);
					} else {
						if (listFile.length > 0) {
							listURL = new URL[listFile.length];
							for (int i = 0; i < listFile.length; i++) {
								File file = new File(pluginTransPath + File.separator + listFile[i]);
								URL url = (file).toURI().toURL();
								listURL[i] = url;
							}
							URLClassLoader loader = new URLClassLoader(listURL);
							Class<?> cls = loader.loadClass(pluginTransPackage);
							Object obj = cls.newInstance();
							if (obj instanceof TransData) {
								transData = (TransData) obj;
							} else {
								logger.error("The class in the package not instanceof TransData! Detail: "
										+ pluginTransPackage);
								System.exit(1);
							}
							loader.close();
						} else {
							logger.error("Can't get Trans plugin in path ! Detail " + PLUGIN_TRANS_PATH);
						}
					}
				} catch (ClassNotFoundException e) {
					logger.error(e.toString());
				} catch (InstantiationException e) {
					logger.error(e.toString());
				} catch (IllegalAccessException e) {
					logger.error(e.toString());
				} catch (MalformedURLException e) {
					logger.error(e.toString());
				} catch (IOException e) {
					logger.error(e.toString());
				}
			} else {
				logger.error("Plugin path can't find! Detail: " + pluginTransPath);
				filePluginTransPath.getParentFile().mkdirs();
			}
		}
		if (pluginTransConf.isEmpty()) {
			logger.info("Plugin conf is empty! ");
		} else {
			this.filePluginTransConf = new File(pluginTransConf);
			if (filePluginTransConf.exists()) {
				if (transData == null) {
					logger.error("Can't get Trans plugin in path ! Detail " + PLUGIN_TRANS_PATH);
				} else {
					transData.init(filePluginTransConf);
					// transData.init(this.mapSysInfo, filePluginTransConf);
				}
			} else {
				logger.warn("Plugin conf can't find! Detail: " + pluginTransConf);
				filePluginTransConf.getParentFile().mkdirs();
			}
		}
		return transData;
	}

	@Override
	public void run() {
		while (true) {
			transData();
		}
	}

	public void start() {
		int size = filesQueen.size();
		if (size > 0) {
			do {
				transData();
				size = filesQueen.size();
			} while (size > 0);
		} else {
			logger.warn("There isn't any file in PATH_IN ! ");
		}
	}

	private void transData() {
		// 数据现在在读取的临时目录，先移动到处理的临时目录，再处理
		try {
			File file = null;
			if (filesQueen.size() > 0) {
				file = filesQueen.take();
			} else {
				return;
			}
			if ((file == null) || (!file.exists())) {
				return;
			}
			String fileName = file.getName();
			File fileTmpProcess = new File(pathTmpProcess + File.separator + fileName);
			while (fileTmpProcess.exists()) {
				logger.warn("Already exist file " + file.getName());
				synchronized (this) {
					this.notifyAll();
					this.wait();
				}
			}
			FileUtils.moveFile(file, fileTmpProcess);
			logger.info("Trans file : " + fileName);
			logger.debug(this.transData.toString() + " Trans file: " + fileName);
			this.transData.transData(fileTmpProcess, folderTmpWrite, folderOut, folderBad);
			if (fileTmpProcess.exists()) {
				FileUtils.deleteQuietly(fileTmpProcess);
			}
		} catch (IOException e) {
			logger.error(e.getMessage());
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
	}

	@SuppressWarnings("unused")
	private synchronized void transDataSYN() {
		// 数据现在在读取的临时目录，先移动到处理的临时目录，再处理
		try {
			File file = filesQueen.take();
			if (!file.exists()) {
				return;
			}
			String fileName = file.getName();
			logger.info("Trans file: " + fileName);
			File fileTmpProcess = new File(pathTmpProcess + File.separator + fileName);
			while (fileTmpProcess.exists()) {
				logger.warn("Already exist file " + file.getName());
				this.notifyAll();
				this.wait();
			}
			FileUtils.moveFile(file, fileTmpProcess);
			this.transData.transData(fileTmpProcess, folderTmpWrite, folderOut, folderBad);
			if (fileTmpProcess.exists()) {
				FileUtils.deleteQuietly(fileTmpProcess);
			}
		} catch (IOException e) {
			logger.error(e.getMessage());
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
	}

}
