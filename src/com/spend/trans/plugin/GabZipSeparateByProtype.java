package com.spend.trans.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;

import com.spend.trans.iface.TransData;
import com.spend.util.com.XmlDom4j;

public class GabZipSeparateByProtype implements TransData {

	/**
	 * 分析文件包含哪些数据集代码，将文件分开来
	 **/

	public static Logger logger = LogManager.getLogger();
	public static final String entryIndexName = "GAB_ZIP_INDEX.xml";

	private String indexKey = "PROTYPE";

	@Override
	public void init(File fileConf) {
		logger.info("init!");
	}

	@Override
	public void transData(File file, File folderTmpWrite, File folderOut, File folderBad) {
		String fileName = file.getName();
		String extension = FilenameUtils.getExtension(fileName);
		if (extension.toLowerCase().equals("zip")) {
			Set<String> setFileIndexItem = ansGabZipFile(file, fileName);

			logger.debug(fileName + " : " + setFileIndexItem);

			for (String item : setFileIndexItem) {
				File folder = new File(folderOut.getAbsolutePath() + File.separator + item);
				if (!folder.exists()) {
					folder.mkdirs();
				}
				logger.info(fileName + " has " + this.indexKey + " value is " + item);
				FileUtils.deleteQuietly(new File(folder.getAbsolutePath() + File.separator + fileName));
				try {
					FileUtils.copyFileToDirectory(file, folder, true);
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}

		} else {
			logger.error("File is not zip! Detail: " + fileName);
			FileUtils.deleteQuietly(new File(folderBad.getAbsolutePath() + File.separator + fileName));
			try {
				FileUtils.moveFileToDirectory(file, folderBad, true);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}

	}

	@SuppressWarnings("unused")
	private Set<String> ansGabZipFile(File file) {
		String fileName = file.getName();
		return ansGabZipFile(file, fileName);
	}

	private Set<String> ansGabZipFile(File file, String fileName) {
		Set<String> setFileIndexItem = new HashSet<String>();
		try {
			boolean hasIndex = false;
			Document doc = null;
			java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(file);

			ZipEntry zipEntryIndex = null;
			List<ZipEntry> listEntry = new ArrayList<ZipEntry>();
			for (Enumeration<? extends ZipEntry> enumeration = zipFile.entries(); enumeration.hasMoreElements();) {
				ZipEntry zipEntry = enumeration.nextElement();
				if (zipEntry.getName().equals(entryIndexName)) {
					zipEntryIndex = zipEntry;
				} else {
					if (FilenameUtils.getExtension(zipEntry.getName()).equals("bcp")) {
						listEntry.add(zipEntry);
					}
				}
			}

			if (zipEntryIndex != null) {
				InputStream is = zipFile.getInputStream(zipEntryIndex);
				doc = XmlDom4j.parse(is);
				is.close();
				hasIndex = true;
			}

			Map<String, Map<String, List<String>>> mapIndexInfo = XmlDom4j.parseGABIndexInfo(doc);
			for (int i = 0; i < listEntry.size(); i++) {
				ZipEntry entry = listEntry.get(i);
				String entryName = entry.getName();
				String protype = new String();
				if (hasIndex) {
					Map<String, List<String>> mapEntryIndex = mapIndexInfo.get(entryName);
					if (mapEntryIndex != null) {
						List<String> listProtype = mapEntryIndex.get("PROTYPE");
						if (listProtype != null && listProtype.size() > 0) {
							protype = listProtype.get(0);
						} else {
							logger.error("Get " + entryName + " 's protype failed! ");
						}
					} else {
						logger.error(entryName + " is not contain in " + entryIndexName);
					}
				} else {
					logger.warn("File don't have index! Can't parse! Detail: " + fileName);
					String[] nameInfo = entryName.split("-");
					if (nameInfo.length == 6) {
						protype = nameInfo[4];
					} else {
						logger.warn("Not right GAB bcp file ! Detail: " + entryName);
					}
				}
				if (!protype.isEmpty()) {
					setFileIndexItem.add(protype);
				}
			}
			zipFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return setFileIndexItem;
	}

}
