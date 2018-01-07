package com.spend.trans.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.spend.engine.producer.FilesMonitor;
import com.spend.engine.props.NProperties;
import com.spend.trans.iface.TransData;

public class DataDistribute implements TransData {

	/*
	 * 数据分发，可以将数据分发到N个目录中
	 */
	private static Logger logger = LogManager.getLogger();

	private static final String OUT_PATH_FOLDERS = "OUT_PATH_FOLDERS";
	private static final String OUT_PATH_BAK_TMP = "OUT_PATH_BAK_TMP";
	// 配置文件
	private File fileConf;
	// 要输出的所有路径
	private String outPaths;
	// 要输出的目录列表
	private List<File> listFolderOut;

	// #若输出有同名文件，先缓存到此目录
	private String folderOutBakTmpPath;
	// #若输出有同名文件，先缓存到此文件夹
	private File folderOutBakTmp;

	// 记序
	private static int sub1 = 0;
	private static int sub2 = 0;

	// 计数
	private static int seq = 0;

	@Override
	public void init(File fileConf) {
//	public void init(Map<String, String> mapSysInfo, File fileConf) {
		// 初始化配置
		this.fileConf = fileConf;

		Map<String, String> map = NProperties.getTheProperties(this.fileConf);
		this.outPaths = map.get(OUT_PATH_FOLDERS);
		this.folderOutBakTmpPath = map.get(OUT_PATH_BAK_TMP);

		this.listFolderOut = new ArrayList<File>();

		if (outPaths == null) {
			logger.error("OUT_PATH_FOLDERS at conf [" + fileConf.getAbsolutePath() + "] is null ! Detail :"
					+ OUT_PATH_FOLDERS);
		} else {
			if (outPaths.isEmpty()) {
				logger.error("OUT_PATH_FOLDERS at conf [" + fileConf.getAbsolutePath() + "] is empty ! Detail :"
						+ OUT_PATH_FOLDERS);
			} else {
				String[] listPath = outPaths.split(",");
				int length = listPath.length;
				logger.info("计划要将会分发数据到" + length + "个目录！");
				for (int i = 0; i < length; i++) {
					String path = listPath[i];
					File folder = new File(path);
					if (listFolderOut.contains(folder)) {
						logger.warn("要分发的路径，有两个目录相同，请检查配置!详情：" + folder.getAbsolutePath());
					} else {
						folder.mkdirs();
						listFolderOut.add(folder);
					}
				}
				logger.info("实际会将会分发数据到" + listFolderOut.size() + "个目录！");
			}
		}

		this.folderOutBakTmp = new File(folderOutBakTmpPath);
		if (!folderOutBakTmp.exists()) {
			folderOutBakTmp.mkdirs();
		}

	}

	@Override
	public void transData(File file, File folderTmpWrite, File folderOut, File folderBad) {
		outputSaveData();
		String fileName = file.getName();
		for (int i = 0; i < listFolderOut.size(); i++) {
			File folder = listFolderOut.get(i);
			++seq;
			if (seq > 99999) {
				seq = 0;
			}
			long time = System.currentTimeMillis();
			++seq;
			File tmpFile = new File(folderTmpWrite.getAbsolutePath() + File.separator + fileName + "_"
					+ String.valueOf(seq) + "_" + String.valueOf(time));
			while (tmpFile.exists()) {
				++seq;
				time = System.currentTimeMillis();
				tmpFile = new File(folderTmpWrite.getAbsolutePath() + File.separator + fileName + "_"
						+ String.valueOf(seq) + "_" + String.valueOf(time));
			}
			File destFile = new File(folder.getAbsolutePath() + File.separator + fileName);
			try {
				FileUtils.copyFile(file, tmpFile);
				outputData(tmpFile, i, destFile);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
	}

	private void outputData(File tmpFile, int index, File destFile) throws IOException {
		String fileName = destFile.getName();
		if (destFile.exists()) {
			String savePath = folderOutBakTmpPath + File.separator + String.valueOf(sub1) + File.separator
					+ String.valueOf(sub2) + File.separator + String.valueOf(index);
			File fileSave = new File(savePath + File.separator + fileName);
			while (fileSave.exists()) {
				if (sub1 > 10000) {
					sub1 = 0;
				}
				if (sub2 > 10000) {
					sub2 = 0;
					++sub1;
				} else {
					++sub2;
				}
				savePath = folderOutBakTmpPath + File.separator + String.valueOf(sub1) + File.separator
						+ String.valueOf(sub2) + File.separator + String.valueOf(index);
				fileSave = new File(savePath + File.separator + fileName);
			}
			File saveFolder = new File(savePath);
			if (!saveFolder.exists()) {
				saveFolder.mkdirs();
			}
			FileUtils.moveFile(tmpFile, fileSave);
		} else {
			FileUtils.moveFile(tmpFile, destFile);
		}
	}

	private void outputSaveData() {
		// 旧数据扫描
		List<File> listSaveFiles = FilesMonitor.scanFilesWithSub(folderOutBakTmp);
		logger.debug(listSaveFiles.size());
		for (File saveFile : listSaveFiles) {
			File folderOut = null;
			try {
				int index = Integer.valueOf(saveFile.getParentFile().getName());
				folderOut = this.listFolderOut.get(index);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
			if (folderOut == null) {
				logger.error("Save an error file at ");
				FileUtils.deleteQuietly(saveFile);
			} else {
				File outFile = new File(folderOut.getAbsolutePath() + File.separator + saveFile.getName());
				if (!outFile.exists()) {
					try {
						FileUtils.moveFile(saveFile, outFile);
						logger.info("ReSend save file! Detail: " + saveFile.getName());
					} catch (IOException e) {
						logger.error(e.getMessage());
					}
				}
			}
		}
	}

}
