package com.spend.trans.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AlterUnivExtract {
	/**
	 * 自动根据通用接收器的配置新增配置
	 * 
	 **/

	public static Logger logger = LogManager.getLogger();

	// 列分隔符
	public static final String SPLIT_COL = "\t";
	// 行分隔符
	public static final String SPLIT_ROW = "\n";

	// 要新增的协议的信息。第一行为标题
	public static final String DATATYPE_MAP = new String(
			"conf" + File.separator + "AlterUnivExtract" + File.separator + "DATATYPE_MAP.txt");

	// 要新增的数据源的信息。第一行为标题
	public static final String DATASOURCE_MAP = new String(
			"conf" + File.separator + "AlterUnivExtract" + File.separator + "DATASOURCE_MAP.txt");

	// 要新增的公共字段的信息。第一行为标题
	public static final String PUBLIC_KEY = new String(
			"conf" + File.separator + "AlterUnivExtract" + File.separator + "PUBLIC_KEY.txt");

	// 要新增的协议的目录的路径
	public static final String DATA_PATH = new String(
			"conf" + File.separator + "AlterUnivExtract" + File.separator + "DATA" + File.separator);

	public static Set<String> setSrcDataType = new HashSet<String>();
	public static Set<String> setSrcDatasource = new HashSet<String>();
	public static Set<String> setSrcPublicKey = new HashSet<String>();
	// 是否仍然添加已经存在的数据源、协议、公共字段？
	private static boolean flagAddExistConf = false;

	public static void TransFile(File file, File folderTmp, File folderOut, File folderBad) {

		List<String> listDatatypeMap = readAddInfoCommon(DATATYPE_MAP);
		List<String> listDatasourceMap = readAddInfoCommon(DATASOURCE_MAP);
		List<String> listPublicKey = readAddInfoCommon(PUBLIC_KEY);

		Map<String, List<String>> mapAddDataInfo = readAddDataInfo();

		// DATATYPE_MAP所在行
		@SuppressWarnings("unused")
		int lineDATATYPE_MAP = 0;
		// DATATYPE_MAP结束所在行
		int lineDATATYPE_MAP_END = 0;

		// DATASOURCE_MAP所在行
		@SuppressWarnings("unused")
		int lineDATASOURCE_MAP = 0;
		// DATASOURCE_MAP结束所在行
		int lineDATASOURCE_MAP_END = 0;

		// PUBLIC_KEY所在行
		@SuppressWarnings("unused")
		int linePUBLIC_KEY = 0;
		// PUBLIC_KEY的结束所在行
		int linePUBLIC_KEY_END = 0;

		// 最后一行有效配置信息的行，非空行！
		int lineLastAvailableInfo = 0;

		List<String> listSourceInfo = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			int line = 0;

			boolean flagDATATYPE_MAP = false;
			boolean flagDATASOURCE_MAP = false;

			boolean flagPUBLIC_KEY = false;

			while (reader.ready()) {
				++line;
				String info = reader.readLine();
				listSourceInfo.add(info);

				if (info.equals("DATATYPE_MAP")) {
					lineDATATYPE_MAP = line;
					flagDATATYPE_MAP = true;
				}
				if (flagDATATYPE_MAP) {
					if (info.isEmpty()) {
						lineDATATYPE_MAP_END = line - 1;
						flagDATATYPE_MAP = false;
					} else {
						if (!info.equals("DATATYPE_MAP")) {
							setSrcDataType.add(info.split("\t")[2]);
						}
					}
				}

				if (info.equals("DATASOURCE_MAP")) {
					lineDATASOURCE_MAP = line;
					flagDATASOURCE_MAP = true;
				}
				if (flagDATASOURCE_MAP) {
					if (info.isEmpty()) {
						lineDATASOURCE_MAP_END = line - 1;
						flagDATASOURCE_MAP = false;
					} else {
						if (!info.equals("DATASOURCE_MAP")) {
							setSrcDatasource.add(info.split("\t")[2]);
						}
					}
				}

				if (info.equals("PUBLIC_KEY")) {
					linePUBLIC_KEY = line;
					flagPUBLIC_KEY = true;
				}
				if (flagPUBLIC_KEY) {
					if (info.isEmpty()) {
						linePUBLIC_KEY_END = line - 1;
						flagPUBLIC_KEY = false;
					} else {
						if (!info.equals("PUBLIC_KEY")) {
							setSrcPublicKey.add(info.split("\t")[3]);
						}
					}
				}

				if (!info.isEmpty()) {
					lineLastAvailableInfo = line;
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// logger.info("setSrcDataType : " + setSrcDataType.size());
		// logger.info("setSrcDatasource : " + setSrcDatasource.size());
		// logger.info("setSrcPublicKey : " + setSrcPublicKey.size());
		// logger.info("setSrcDataType : " + setSrcDataType);
		// logger.info("setSrcDatasource : " + setSrcDatasource);
		// logger.info("setSrcPublicKey : " + setSrcPublicKey);

		int srcDataTypeId = -1;

		// 获取原始文件的最后一个协议的ID
		if (listSourceInfo.size() > lineDATATYPE_MAP_END) {
			String infoLastDataType = listSourceInfo.get(lineDATATYPE_MAP_END - 1);
			if (infoLastDataType != null && !infoLastDataType.isEmpty()) {
				String strID = infoLastDataType.split(SPLIT_COL)[1];
				srcDataTypeId = Integer.valueOf(strID);
			} else {
				logger.error("Get lineDATATYPE_MAP_END error! ");
			}
		} else {
			logger.error("Parse file error! listSourceInfo.size()<lineDATATYPE_MAP_END! File: " + file.getName());
		}

		// 获取原始文件的最后一个数据源的ID
		int srcDatasourceId = -1;
		if (listSourceInfo.size() > lineDATASOURCE_MAP_END) {
			String infoLastDatasource = listSourceInfo.get(lineDATASOURCE_MAP_END - 1);
			if (infoLastDatasource != null && !infoLastDatasource.isEmpty()) {
				String strID = infoLastDatasource.split(SPLIT_COL)[1];
				srcDatasourceId = Integer.valueOf(strID);
			} else {
				logger.error("Get lineDATASOURCE_MAP_END error! ");
			}
		} else {
			logger.error("Parse file error! listSourceInfo.size()<lineDATASOURCE_MAP_END! File: " + file.getName());
		}

		// 获取原始文件最后一个公共字段的ID
		int srcPublicKeyId = -1;
		if (listSourceInfo.size() > linePUBLIC_KEY_END) {
			String infoLastPublicKey = listSourceInfo.get(linePUBLIC_KEY_END - 1);
			if (infoLastPublicKey != null && !infoLastPublicKey.isEmpty()) {
				String strID = infoLastPublicKey.split(SPLIT_COL)[1];
				srcPublicKeyId = Integer.valueOf(strID);
			} else {
				logger.error("Get linePUBLIC_KEY_END error! ");
			}
		} else {
			logger.error("Parse file error! listSourceInfo.size()<linePUBLIC_KEY_END! File: " + file.getName());
		}

		// 此次增加的所有协议及对应的序列
		LinkedHashMap<String, Integer> mapDatatypeID = new LinkedHashMap<String, Integer>();
		// List<String> listAddDatatype = new ArrayList<String>();

		for (int i = 0; i < listDatatypeMap.size(); i++) {
			String info = listDatatypeMap.get(i);
			String DATATYPE = info.split(SPLIT_COL)[2];
			int theID = srcDataTypeId + 1 + i;
			mapDatatypeID.put(DATATYPE, theID);
		}

		// 新增DATASOURCE的ID
		LinkedHashMap<String, Integer> mapDatasourceID = new LinkedHashMap<String, Integer>();
		for (int i = 0; i < listDatasourceMap.size(); i++) {
			String info = listDatasourceMap.get(i);
			String DATASOURCE = info.split(SPLIT_COL)[2];
			int theID = srcDatasourceId + 1 + i;
			mapDatasourceID.put(DATASOURCE, theID);
		}

		// 新增的公共字段PUBLIC_KEY的ID
		LinkedHashMap<String, Integer> mapPublicID = new LinkedHashMap<String, Integer>();

		for (int i = 0; i < listPublicKey.size(); i++) {
			String info = listPublicKey.get(i);
			String PUBLIC_KEY = info.split(SPLIT_COL)[3];
			int theID = srcPublicKeyId + 1 + i;
			mapPublicID.put(PUBLIC_KEY, theID);
		}

		// 转换后最终形成的配置信息链表
		List<String> listResultInfo = new ArrayList<String>();

		for (int i = 0; i < listSourceInfo.size(); i++) {
			String srcInfo = listSourceInfo.get(i);
			if (i == lineDATATYPE_MAP_END) {
				// 此时刚好在DATATYPE_MAP_END的下一行，刚好开始增加DATATYPE_MAP信息
				for (int j = 0; j < listDatatypeMap.size(); j++) {
					String info = listDatatypeMap.get(j);
					String tmp = info + SPLIT_COL + "abcdefghigk";
					String[] infos = tmp.split(SPLIT_COL);
					int length = infos.length - 1;
					String DATATYPE = infos[2];
					if (setSrcDataType.contains(DATATYPE)) {
						if (flagAddExistConf) {
							logger.warn("DATATYPE already exist in " + file.getName() + "! Detail: " + DATATYPE);
						} else {
							// logger.error("DATATYPE already exist in " +
							// file.getName() + "! Detail: " + DATATYPE);
							// System.exit(0);
							logger.warn("DATATYPE already exist in " + file.getName() + "! Detail: " + DATATYPE);
							continue;
						}
					}
					String id = mapDatatypeID.get(DATATYPE).toString();
					StringBuffer sb = new StringBuffer();
					for (int n = 0; n < length; n++) {
						String item = new String();
						if (n == 1) {
							// 微调ID
							item = id;
						} else {
							item = infos[n];
						}
						if (n == (length - 1)) {
							sb.append(item);
						} else {
							sb.append(item).append(SPLIT_COL);
						}
					}
					String addInfo = sb.toString();
					listResultInfo.add(addInfo);
				}
				listResultInfo.add(srcInfo);
			} else if (i == lineDATASOURCE_MAP_END) {
				for (int j = 0; j < listDatasourceMap.size(); j++) {
					String info = listDatasourceMap.get(j);
					String tmp = info + SPLIT_COL + "abcdefghigk";
					String[] infos = tmp.split(SPLIT_COL);
					int length = infos.length - 1;
					String DATASOURCE = infos[2];
					if (setSrcDatasource.contains(DATASOURCE)) {
						if (flagAddExistConf) {
							logger.warn("DATASOURCE already exist in " + file.getName() + "! Detail: " + DATASOURCE);
						} else {
							// logger.error("DATASOURCE already exist in " +
							// file.getName() + "! Detail: " + DATASOURCE);
							// System.exit(0);
							logger.warn("DATASOURCE already exist in " + file.getName() + "! Detail: " + DATASOURCE);
							continue;
						}
					}
					String id = mapDatasourceID.get(DATASOURCE).toString();
					StringBuffer sb = new StringBuffer();
					for (int n = 0; n < length; n++) {
						String item = new String();
						if (n == 1) {
							// 微调ID
							item = id;
						} else {
							item = infos[n];
						}
						if (n == (length - 1)) {
							sb.append(item);
						} else {
							sb.append(item).append(SPLIT_COL);
						}
					}
					String addInfo = sb.toString();
					listResultInfo.add(addInfo);
				}
				listResultInfo.add(srcInfo);
			} else if (i == linePUBLIC_KEY_END) {
				for (int j = 0; j < listPublicKey.size(); j++) {
					String info = listPublicKey.get(j);
					String tmp = info + SPLIT_COL + "abcdefghigk";
					String[] infos = tmp.split(SPLIT_COL);
					int length = infos.length - 1;
					String PUB_KEY = infos[3];
					if (setSrcPublicKey.contains(PUB_KEY)) {
						if (flagAddExistConf) {
							logger.warn("PUB_KEY already exist in " + file.getName() + "! Detail: " + PUB_KEY);
						} else {
							// logger.error("PUB_KEY already exist in " +
							// file.getName() + "! Detail: " + PUB_KEY);
							// System.exit(0);
							// logger.warn("PUB_KEY already exist in " +
							// file.getName() + "! Detail: " + PUB_KEY);
							continue;
						}
					}
					String id = mapPublicID.get(PUB_KEY).toString();
					StringBuffer sb = new StringBuffer();
					for (int n = 0; n < length; n++) {
						String item = new String();
						if (n == 1) {
							// 微调ID
							item = id;
						} else {
							item = infos[n];
						}
						if (n == (length - 1)) {
							sb.append(item);
						} else {
							sb.append(item).append(SPLIT_COL);
						}
					}
					String addInfo = sb.toString();
					listResultInfo.add(addInfo);
				}
				listResultInfo.add(srcInfo);

			} else if (i == lineLastAvailableInfo) {
				// 最后一行有效信息的下一行
				for (Iterator<Entry<String, Integer>> it = mapDatatypeID.entrySet().iterator(); it.hasNext();) {
					Entry<String, Integer> entry = it.next();
					String datatype = entry.getKey();
					if (setSrcDataType.contains(datatype)) {
						if (flagAddExistConf) {
							logger.warn("DATATYPE already exist in " + file.getName() + "! Detail: " + datatype);
						} else {
							// logger.error("DATATYPE already exist in " +
							// file.getName() + "! Detail: " + setSrcDataType);
							// System.exit(0);
							logger.warn("DATATYPE already exist in " + file.getName() + "! Detail: " + datatype);
							continue;
						}
					}
					Integer id = entry.getValue();
					// 两个空行
					listResultInfo.add("");
					listResultInfo.add("");
					String idDatatype = String.valueOf(id) + "-" + datatype;
					listResultInfo.add(idDatatype);
					List<String> listTheDatatypeAddInfo = mapAddDataInfo.get(datatype);
					for (int j = 0; j < listTheDatatypeAddInfo.size(); j++) {
						String addInfo = listTheDatatypeAddInfo.get(j);
						listResultInfo.add(addInfo);
					}
				}
				// 其实这个srcInfo现在应该是空行
				listResultInfo.add(srcInfo);
			} else {
				listResultInfo.add(srcInfo);
			}
		}

		// 写入文件
		String outFileName = file.getName();
		File fileOutTmp = new File(folderTmp.getAbsolutePath() + File.separator + outFileName);
		try {
			FileOutputStream fos = new FileOutputStream(fileOutTmp);
			for (int k = 0; k < listResultInfo.size(); k++) {
				String info = listResultInfo.get(k);
				fos.write(info.getBytes());
				fos.write(SPLIT_ROW.getBytes());
			}
			fos.close();
			File fileOut = new File(folderOut.getAbsolutePath() + File.separator + outFileName);
			if (fileOut.exists()) {
				File fileBak = new File(folderBad.getAbsolutePath() + File.separator + outFileName);
				FileUtils.deleteQuietly(fileBak);
				FileUtils.moveFile(fileOut, fileBak);
			}
			FileUtils.moveFile(fileOutTmp, fileOut);
			// 删除原始文件
			if (file.exists()) {
				FileUtils.deleteQuietly(file);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static List<String> readAddInfoCommon(String filePath) {
		// 通用接收器平台业务配置，要增加的公共项的解析，包括协议、数据源、公共字段等。首行的标题跳过。
		List<String> listInfo = new ArrayList<String>();
		File file = new File(filePath);

		if (!file.exists()) {
			logger.warn("File not exist! Detail: " + filePath);
			return listInfo;
		}

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			int line = 0;
			while (reader.ready()) {
				++line;
				String info = reader.readLine();
				// 首行标题，跳过
				if (line > 1) {
					if (info.isEmpty()) {
						logger.warn("Info is empty in file [" + filePath + "] at line [" + line + "]");
					} else {
						listInfo.add(info);
					}
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return listInfo;
	}

	private static Map<String, List<String>> readAddDataInfo() {
		// 解析要添加的具体协议的配置信息
		Map<String, List<String>> map = null;
		File folder = new File(DATA_PATH);
		if (folder.exists()) {
			String[] files = folder.list();
			if (files != null && files.length > 0) {
				map = new HashMap<String, List<String>>();
				for (String fileName : files) {
					String filePath = DATA_PATH + File.separator + fileName;
					List<String> listInfo = readAddInfoData(filePath);
					if (listInfo == null) {
						logger.warn("Add data info is folder! Detail: " + filePath);
					} else if (listInfo.isEmpty()) {
						logger.error("Add data info is empty! Detail: " + filePath);
					} else {
						String fileBaseName = FilenameUtils.getBaseName(fileName);
						map.put(fileBaseName, listInfo);
					}
				}
			} else {
				logger.warn("There isn't any data to be add in folder! Detail: " + DATA_PATH);
			}
		} else {
			logger.error("Folder not exist! Will not get right conf! Detail: " + DATA_PATH);
		}
		return map;
	}

	private static List<String> readAddInfoData(String filePath) {
		// 通用接收器平台业务配置，针对要增加的私有项的配置
		List<String> listInfo = new ArrayList<String>();
		File file = new File(filePath);
		if (file.isDirectory()) {
			return null;
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			int line = 0;
			while (reader.ready()) {
				++line;
				String info = reader.readLine();
				// 首行标题，跳过
				if (info.isEmpty()) {
					logger.error("Info is empty in file [" + filePath + "] at line [" + line + "]");
				} else {
					listInfo.add(info);
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return listInfo;
	}

}
