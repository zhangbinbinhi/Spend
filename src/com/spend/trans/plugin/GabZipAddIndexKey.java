package com.spend.trans.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;

import com.spend.engine.producer.FilesMonitor;
import com.spend.engine.props.NProperties;
import com.spend.trans.iface.TransData;
import com.spend.util.com.XmlDom4j;

import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class GabZipAddIndexKey implements TransData {

	/**
	 * 自动为zip包的索引新增key
	 **/

	public static Logger logger = LogManager.getLogger();

	private static int sub1 = 0;
	private static int sub2 = 0;

	private String folderOutSavePath;
	private File folderOutSave;

	@Override
	public void init(File fileConf) {
//	public void init(Map<String, String> mapSysInfo, File fileConf) {
		logger.info("init GabZipAddIndexKey!");
		// String PATH_BAD = mapSysInfo.get("PATH_BAD");
		// folderOutSavePath = PATH_BAD + File.separator + "GabZipAddIndexKey";
		Map<String, String> mapPlugin = NProperties.getTheProperties(fileConf);
		folderOutSavePath = mapPlugin.get("SAVE_PATH");
		folderOutSave = new File(folderOutSavePath);
		if (!folderOutSave.exists()) {
			folderOutSave.mkdirs();
		}
	}

	private void saveExistFile(File file, String fileName) {
		// 将已经存在的数据临时存下来
		logger.debug("Already exist same name file " + fileName);
		File fileSave = new File(folderOutSavePath + File.separator + fileName);
		String savePath = new String(folderOutSavePath);
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
			savePath = folderOutSavePath + File.separator + String.valueOf(sub1) + File.separator
					+ String.valueOf(sub2);
			fileSave = new File(savePath + File.separator + fileName);
		}
		File folderSave = new File(savePath);
		if (!folderSave.exists()) {
			folderSave.mkdirs();
		}
		try {
			FileUtils.moveFile(file, fileSave);
			logger.info("Save file : " + fileName);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	private void sendSaveFile(File folderOut) {
		// 旧数据扫描
		List<File> listSaveFiles = FilesMonitor.scanFilesWithSub(folderOutSave);
		logger.debug("Save file num" + listSaveFiles.size());
		for (File saveFile : listSaveFiles) {
			File outFile = new File(folderOut.getAbsolutePath() + File.separator + saveFile.getName());
			if (!outFile.exists()) {
				try {
					FileUtils.moveFile(saveFile, outFile);
					String fileName = saveFile.getName();
					logger.info("Send file : " + fileName);
				} catch (IOException e) {
					logger.warn(e.getMessage());
				}
			}
		}
	}

	@Override
	public void transData(File file, File folderTmpWrite, File folderOut, File folderBad) {
		sendSaveFile(folderOut);
		String fileName = file.getName();
		String extension = FilenameUtils.getExtension(fileName);
		if (extension.toLowerCase().equals("zip")) {
			// zip补充索引
			// file = updateZipFile(file);
			file = updateZipFile(file, folderTmpWrite);
			try {
				// 输出数据到最终目录
				File fileDest = new File(folderOut.getAbsolutePath() + File.separator + fileName);
				if (fileDest.exists()) {
					saveExistFile(file, fileName);
				} else {
					FileUtils.moveFile(file, fileDest);
					logger.info("Write file : " + fileName);
				}
			} catch (IOException e) {
				logger.error(e.getMessage());
			}

		} else {
			logger.warn("Not rigth file! Detail: " + file.getName());
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

	@SuppressWarnings("unused")
	private File updateZipFile(File file) {
		try {
			net.lingala.zip4j.core.ZipFile zipFile = new net.lingala.zip4j.core.ZipFile(file);
			FileHeader fileHeader = zipFile.getFileHeader("GAB_ZIP_INDEX.xml");
			if (fileHeader != null) {
				InputStream is = zipFile.getInputStream(fileHeader);
				Document doc = XmlDom4j.parse(is);
				is.close();
				boolean flagNeedUpdate = updateDoc(doc);
				if (flagNeedUpdate) {
					logger.info(file.getName() + " need update ! ");
					fileHeader = zipFile.getFileHeader("GAB_ZIP_INDEX.xml");
					if (fileHeader != null) {
						zipFile.removeFile(fileHeader);
						ZipParameters parameters = new ZipParameters();
						parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
						parameters.setFileNameInZip("GAB_ZIP_INDEX.xml");
						parameters.setSourceExternalStream(true);
						InputStream isX = XmlDom4j.parse(doc);
						zipFile.addStream(isX, parameters);
						isX.close();
					}
				}
			}
		} catch (net.lingala.zip4j.exception.ZipException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		return file;
	}

	private File updateZipFile(File file, File folderTmp) {
		try {
			Document doc = null;
			boolean flagNeedUpdate = false;
			java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(file);
			String entryIndexName = "GAB_ZIP_INDEX.xml";
			ZipEntry zipEntryIndex = null;
			List<ZipEntry> listEntry = new ArrayList<ZipEntry>();
			for (Enumeration<? extends ZipEntry> enumeration = zipFile.entries(); enumeration.hasMoreElements();) {
				ZipEntry zipEntry = enumeration.nextElement();
				listEntry.add(zipEntry);
				if (zipEntry.getName().equals(entryIndexName)) {
					zipEntryIndex = zipEntry;
				}
			}
			if (zipEntryIndex != null) {
				InputStream is = zipFile.getInputStream(zipEntryIndex);
				doc = XmlDom4j.parse(is);
				flagNeedUpdate = updateDoc(doc);
				is.close();
			}
			boolean hasUpdate = false;
			File writeFile = new File(folderTmp.getAbsolutePath() + File.separator + file.getName());
			if (flagNeedUpdate) {
				logger.info(file.getName() + " need update ! ");
				if (doc != null) {
					// 根据文件路径构造一个文件输出流
					FileOutputStream out = new FileOutputStream(writeFile);
					// 传入文件输出流对象,创建ZIP数据输出流对象
					org.apache.tools.zip.ZipOutputStream zipOut = new org.apache.tools.zip.ZipOutputStream(out);
					for (int i = 0; i < listEntry.size(); i++) {
						ZipEntry entryData = listEntry.get(i);
						String entryName = entryData.getName();
						org.apache.tools.zip.ZipEntry entry = new org.apache.tools.zip.ZipEntry(entryName);
						entry.setUnixMode(644);// 解决linux乱码
						zipOut.putNextEntry(entry);
						InputStream isX = null;
						if (entryName.equals(entryIndexName)) {
							isX = XmlDom4j.parse(doc);
						} else {
							isX = zipFile.getInputStream(entryData);
						}
						// 向压缩文件中输出数据
						int nNumber = 0;
						byte[] buffer = new byte[1024];
						while ((nNumber = isX.read(buffer)) != -1) {
							zipOut.write(buffer, 0, nNumber);
						}
						// 关闭创建的流对象
						isX.close();
					}
					zipOut.close();
					hasUpdate = true;
				} else {
					logger.error("Get a error document of xml in zip " + file.getName());
				}
			}
			zipFile.close();
			if (hasUpdate) {
				FileUtils.deleteQuietly(file);
				file = writeFile;
			}
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		return file;
	}

	private static boolean updateDoc(Document doc) {
		boolean flagNeedUpdate = false;
		Element rootElm = doc.getRootElement();
		Element dataElement = rootElm.element("DATASET").element("DATA").element("DATASET");
		@SuppressWarnings("unchecked")
		List<Element> listDataElement = dataElement.elements("DATA");
		for (int i = 0; i < listDataElement.size(); i++) {
			Element dataKindElem = listDataElement.get(i);
			@SuppressWarnings("unchecked")
			List<Element> listDataSetElem = dataKindElem.elements("DATASET");
			for (int j = 0; j < listDataSetElem.size(); j++) {
				Element dataSetElem = listDataSetElem.get(j);
				String type = dataSetElem.attributeValue("name");
				if (type != null && type.equals("WA_COMMON_010015")) {
					Element dataElem = dataSetElem.element("DATA");
					@SuppressWarnings("unchecked")
					List<Element> listItemElem = dataElem.elements("ITEM");
					for (int k = 0; k < listItemElem.size(); k++) {
						Element itemElem = listItemElem.get(k);
						String key = itemElem.attributeValue("key");
						boolean flagItemNeedUpdate = false;
						if (key == null) {
							flagItemNeedUpdate = true;
						} else {
							if (key.isEmpty()) {
								flagItemNeedUpdate = true;
							}
						}
						if (flagItemNeedUpdate) {
							// 只要有一个ITEM需要更新，则改zip需要更新的的flag是true
							flagNeedUpdate = true;
							String eng = itemElem.attributeValue("eng");
							String keyX = k + "_" + eng.trim();
							itemElem.addAttribute("key", keyX);
						}
					}
				}
			}
		}
		// logger.debug(doc.asXML());
		return flagNeedUpdate;
	}

}
