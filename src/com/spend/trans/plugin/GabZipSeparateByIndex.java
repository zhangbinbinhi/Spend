package com.spend.trans.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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

import com.spend.engine.props.NProperties;
import com.spend.trans.iface.TransData;
import com.spend.util.com.XmlDom4j;

public class GabZipSeparateByIndex implements TransData {

	/**
	 * 根据文件的索引的某一列，将文件分发到对应的目录。
	 **/

	public static Logger logger = LogManager.getLogger();
	public static final String entryIndexName = "GAB_ZIP_INDEX.xml";

	private String indexKey;
	private String lookPath;
	private File lookFolder;

	@Override
	public void init(File fileConf) {
		logger.info("init!");
		Map<String, String> mapPlugin = NProperties.getTheProperties(fileConf);
		indexKey = mapPlugin.get("INDEX_KEY");
		lookPath = mapPlugin.get("LOOK_PATH") + File.separator + indexKey;
		lookFolder = new File(lookPath);
		if (!lookFolder.exists()) {
			lookFolder.mkdirs();
		}
	}

	@Override
	public void transData(File file, File folderTmpWrite, File folderOut, File folderBad) {
		String fileName = file.getName();
		String extension = FilenameUtils.getExtension(fileName);
		if (extension.toLowerCase().equals("zip")) {
			Set<String> setFileIndexItem = ansGabZipFile(file, fileName);
			logger.debug(fileName + " : " + setFileIndexItem);
			for (String item : setFileIndexItem) {
				File folder = new File(
						folderOut.getAbsolutePath() + File.separator + this.indexKey + File.separator + item);
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

			if (hasIndex) {
				Map<String, Map<String, List<String>>> mapIndexInfo = XmlDom4j.parseGABIndexInfo(doc);

				for (int i = 0; i < listEntry.size(); i++) {
					ZipEntry entry = listEntry.get(i);
					String entryName = entry.getName();
					Map<String, List<String>> mapEntryIndex = mapIndexInfo.get(entryName);

					if (mapEntryIndex == null) {
						logger.error(entryName + " is not contain in " + entryIndexName);
						continue;
					}
					List<String> listIndex = mapEntryIndex.get("FIELD");
					String lineSplit = mapEntryIndex.get("LINE_SPLIT").get(0);
					String fieldSplit = mapEntryIndex.get("FIELD_SPLIT").get(0);
					if (listIndex != null) {

						int indexKeyIndex = -1;
						for (int j = 0; j < listIndex.size(); j++) {
							if (listIndex.get(j).equals(this.indexKey)) {
								indexKeyIndex = j;
							}
						}

						if (indexKeyIndex == -1) {
							logger.info("Index key [" + this.indexKey + "] is not include in the entry! Detail: "
									+ entryName);
							continue;
						}

						if (lineSplit == null) {
							lineSplit = "\n";
						} else {
							if (lineSplit.isEmpty()) {
								lineSplit = "\n";
							} else {
								if (lineSplit.equals("\\n")) {
									lineSplit = "\n";
								}
							}
						}
						if (fieldSplit == null) {
							fieldSplit = "\t";
						} else {
							if (fieldSplit.isEmpty()) {
								fieldSplit = "\t";
							} else {
								if (fieldSplit.equals("\\t")) {
									fieldSplit = "\t";
								}
							}
						}
						logger.debug(this.indexKey + " : " + entryName + " : " + indexKeyIndex + " : " + fieldSplit
								+ " : " + lineSplit);

						Set<String> setEntryIndexItem = new HashSet<String>();

						InputStream is = zipFile.getInputStream(entry);
						BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
						int line = 0;
						while (reader.ready()) {
							String str = reader.readLine();
							++line;
							String[] listItem = str.split(fieldSplit);
							String item = listItem[indexKeyIndex];
							setEntryIndexItem.add(item);
							setFileIndexItem.add(item);
							logger.debug("[" + this.indexKey + "] value is [" + item + "] in file [" + fileName
									+ "] on entry [" + entryName + "] at line [" + line + "]");
						}
						reader.close();
						is.close();

						if (setEntryIndexItem.size() > 0) {
							for (String item : setEntryIndexItem) {
								File lookFile = new File(this.lookPath + File.separator + item + ".txt");
								FileOutputStream fos = new FileOutputStream(lookFile, true);
								String info = "[" + this.indexKey + "]\t[" + item + "]\t[" + fileName + "]\t["
										+ entryName + "]";
								fos.write(info.getBytes());
								fos.write("\r\n".getBytes(Charset.forName("UTF-8")));
								fos.close();
							}
						}
					} else {
						logger.error(entryName + " is not define in " + entryIndexName);
					}
				}
			} else {
				logger.warn("File don't have index! Can't parse! Detail: " + fileName);
			}

			zipFile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return setFileIndexItem;
	}

}
