package com.spend.trans.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.spend.trans.iface.TransData;
import com.spend.util.com.XmlDom4j;

public class DataToExcel implements TransData {

	Logger logger = LogManager.getLogger();

	// 主要先统计数据源，协议
	private Map<String, Set<String>> mapStat = new HashMap<String, Set<String>>();

	@SuppressWarnings("unused")
	private long numZip = 0L;
	@SuppressWarnings("unused")
	private long numBcp = 0L;
	@SuppressWarnings("unused")
	private long numLine = 0L;
	@SuppressWarnings("unused")
	private long time = 0L;

	@Override
	public void init(File fileConf) {
//	public void init(Map<String, String> mapSysInfo, File fileConf) {
		logger.info("init " + this.toString());
	}

	@Override
	public void transData(File file, File folderTmpWrite, File folderOut, File folderBad) {
		File tmpFile = writeFileAsExcel(file, folderTmpWrite, folderBad);
		if (tmpFile == null) {
			logger.error("DataToExcel failed! Detail: " + file.getName());
		} else {
			File destFile = new File(folderOut.getAbsolutePath() + File.separator + tmpFile.getName());
			try {
				if (destFile.exists()) {
					FileUtils.deleteQuietly(destFile);
				}
				FileUtils.moveFile(tmpFile, destFile);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
	}

	private File writeFileAsExcel(File file, File folderTmpWrite, File folderBad) {
		File tmpFile = null;
		String extension = FilenameUtils.getExtension(file.getName());

		if (extension.equals("bcp") || extension.equals("txt") || extension.equals("nb")) {
			++numBcp;
			tmpFile = writeBcpFileAsExcel(file, folderTmpWrite);
		} else if (extension.equals("zip")) {
			++numZip;
			tmpFile = writeZipFileAsExcel(file, folderTmpWrite);
		} else {
			logger.info("Not ok file! Detail: " + file.getName());
			try {
				FileUtils.copyFileToDirectory(file, folderBad);
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return tmpFile;
	}

	private File writeBcpFileAsExcel(File file, File folderTmpWrite) {
		File dataOutTmpFile = new File(folderTmpWrite.getAbsolutePath() + File.separator + file.getName() + ".xlsx");

		Workbook wb = new XSSFWorkbook();
		CellStyle css = wb.createCellStyle();
		css.setBorderBottom(HSSFCellStyle.BORDER_THIN);// 下边框
		css.setBorderLeft(HSSFCellStyle.BORDER_THIN);// 左边框
		css.setBorderRight(HSSFCellStyle.BORDER_THIN);// 右边框
		css.setBorderTop(HSSFCellStyle.BORDER_THIN);// 上边框
		css.setAlignment(HSSFCellStyle.ALIGN_LEFT);// 左对齐
		Sheet sheet = wb.createSheet();
		try {
			Row titleRow = null;
			int titleLength = 0;
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			int line = 0;
			while (in.ready()) {
				++line;
				String str = in.readLine();
				String tmp = str + "\t" + "abcdefg";
				String[] info = tmp.split("\t");
				int length = info.length - 1;
				if (length > titleLength) {
					titleLength = length;
				}
				if (line == 1) {
					titleRow = sheet.createRow(0);
				}
				Row row = sheet.createRow(line);
				for (int j = 0; j < length; j++) {
					Cell cell = row.createCell(j);
					cell.setCellValue(info[j]);
					cell.setCellStyle(css);
				}
			}
			numLine += line;
			for (int t = 0; t < titleLength; t++) {
				Cell titleCell = titleRow.createCell(t);
				titleCell.setCellValue((1 + t));
				titleCell.setCellStyle(css);
			}
			FileOutputStream fos = new FileOutputStream(dataOutTmpFile);
			in.close();
			wb.write(fos);
			fos.close();
			wb.close();
		} catch (IOException e) {
			// e.printStackTrace();
			logger.error(e.getMessage());
		}
		return dataOutTmpFile;
	}

	private File writeZipFileAsExcel(File file, File folderTmpWrite) {
		File dataOutTmpFile = new File(folderTmpWrite.getAbsolutePath() + File.separator + file.getName() + ".xlsx");
		try {
			Workbook wb = new XSSFWorkbook();
			CellStyle css = wb.createCellStyle();
			css.setBorderBottom(HSSFCellStyle.BORDER_THIN);// 下边框
			css.setBorderLeft(HSSFCellStyle.BORDER_THIN);// 左边框
			css.setBorderRight(HSSFCellStyle.BORDER_THIN);// 右边框
			css.setBorderTop(HSSFCellStyle.BORDER_THIN);// 上边框
			css.setAlignment(HSSFCellStyle.ALIGN_LEFT);// 左对齐

			ZipFile zipFile = new ZipFile(file);
			ZipEntry zipEntryIndex = null;
			List<ZipEntry> listZipEntry = new ArrayList<ZipEntry>();

			for (Enumeration<? extends ZipEntry> enumeration = zipFile.entries(); enumeration.hasMoreElements();) {
				ZipEntry zipEntry = enumeration.nextElement();
				if (zipEntry.getName().equals("GAB_ZIP_INDEX.xml")) {
					zipEntryIndex = zipEntry;
					logger.info(zipEntry.getName());
				} else {
					listZipEntry.add(zipEntry);
				}
			}

			if (zipEntryIndex != null) {
				// 有索引
				InputStream isIndex = zipFile.getInputStream(zipEntryIndex);
				Map<String, Map<String, List<String>>> mapIndexInfo = XmlDom4j.parseGABIndexInfo(isIndex);
				isIndex.close();
				// logger.info(mapIndexInfo);
				for (int i = 0; i < listZipEntry.size(); i++) {
					ZipEntry zipEntry = listZipEntry.get(i);
					String entryName = zipEntry.getName();
					logger.info(entryName);
					if (FilenameUtils.getExtension(entryName).equals("bcp")) {
						++numBcp;
						InputStream is = zipFile.getInputStream(zipEntry);
						if (!wbAddSheet(wb, css, 1 + i, is, entryName, mapIndexInfo)) {
							logger.warn("write error!");
						}
						is.close();
					}
				}
			} else {
				// 无索引
				for (int i = 0; i < listZipEntry.size(); i++) {
					ZipEntry zipEntry = listZipEntry.get(i);
					InputStream is = zipFile.getInputStream(zipEntry);
					String entryName = zipEntry.getName();
					logger.info(entryName);
					String sheetName = String.valueOf(i + 1) + " | " + entryName.substring(0, 25);
					if (!wbAddSheet(wb, css, sheetName, is)) {
						logger.warn("write error!");
					}
					is.close();
				}
			}

			FileOutputStream fos = new FileOutputStream(dataOutTmpFile);
			wb.write(fos);
			fos.close();
			wb.close();
			zipFile.close();
		} catch (IOException e) {
			e.printStackTrace();

		}
		return dataOutTmpFile;
	}

	private boolean wbAddSheet(Workbook wb, CellStyle css, int id, InputStream is, String entryName,
			Map<String, Map<String, List<String>>> mapIndexInfo) {
		String sheetName = new String();
		Map<String, List<String>> mapFileIndexInfo = mapIndexInfo.get(entryName);
		if (mapFileIndexInfo == null) {
			logger.info("索引错误！ 数据文件： " + entryName + " 在数据的索引中没有记录！");
		} else {
			List<String> listProtype = mapFileIndexInfo.get("PROTYPE");
			if (listProtype == null) {
				logger.info("索引警告！ 数据文件： " + entryName + " 在数据的索引中的中文解释序列为空！");
			} else {
				if (listProtype.size() > 0) {
					if (!listProtype.get(0).isEmpty()) {
						sheetName = id + " | " + listProtype.get(0);
					}
				}
			}
		}
		if (sheetName == null || sheetName.isEmpty()) {
			sheetName = id + " | " + entryName.substring(0, 25);
		}
		return wbAddSheet(wb, css, sheetName, is, entryName, mapIndexInfo);
	}

	private boolean wbAddSheet(Workbook wb, CellStyle css, String sheetName, InputStream is, String entryName,
			Map<String, Map<String, List<String>>> mapIndexInfo) {
		boolean flag = true;
		Sheet sheet = wb.createSheet(sheetName);
		try {
			Row titleRow = null;
			Row chnRow = null;
			Row indexRow = null;
			int titleLength = 0;
			BufferedReader in = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			int line = 0;
			while (in.ready()) {
				++line;
				String str = in.readLine();
				String tmp = str + "\t" + "abcdefg";
				String[] info = tmp.split("\t");
				int length = info.length - 1;
				if (length > titleLength) {
					titleLength = length;
				}
				if (line == 1) {
					titleRow = sheet.createRow(0);
					chnRow = sheet.createRow(1);
					indexRow = sheet.createRow(2);
				}
				Row row = sheet.createRow(line + 2);
				for (int j = 0; j < length; j++) {
					Cell cell = row.createCell(j);
					cell.setCellValue(info[j]);
					cell.setCellStyle(css);
				}
			}
			numLine += line;
			for (int t = 0; t < titleLength; t++) {
				Cell titleCell = titleRow.createCell(t);
				titleCell.setCellValue((1 + t));
				titleCell.setCellStyle(css);
				sheet.setColumnWidth(t, 4800);
			}
			Map<String, List<String>> mapFileIndexInfo = mapIndexInfo.get(entryName);

			if (mapFileIndexInfo == null) {
				logger.info("索引错误！ 数据文件： " + entryName + " 在数据的索引中没有记录！");
			} else {

				// 统计用
				String datasource = new String();
				String protype = new String();
				List<String> infoDatasource = mapFileIndexInfo.get("DATASOURCE");
				List<String> infoProtype = mapFileIndexInfo.get("PROTYPE");
				if (infoDatasource != null && infoDatasource.size() > 0) {
					datasource = infoDatasource.get(0);
				}
				if (infoProtype != null && infoProtype.size() > 0) {
					protype = infoProtype.get(0);
				}
				if (!(datasource.isEmpty() || protype.isEmpty())) {
					Set<String> setProtype = mapStat.get(datasource);
					if (setProtype == null) {
						setProtype = new HashSet<String>();
					}
					setProtype.add(protype);
					mapStat.put(datasource, setProtype);
				}

				List<String> listCHNField = mapFileIndexInfo.get("CHN");
				if (listCHNField == null) {
					logger.info("索引警告！ 数据文件： " + entryName + " 在数据的索引中的中文解释序列为空！");
				} else {
					for (int t = 0; t < listCHNField.size(); t++) {
						Cell chnCell = chnRow.createCell(t);
						chnCell.setCellValue(listCHNField.get(t));
						chnCell.setCellStyle(css);
					}
				}
				@SuppressWarnings("unused")
				List<String> listEngField = mapFileIndexInfo.get("ENG");

				List<String> listDataField = mapFileIndexInfo.get("FIELD");
				if (listDataField == null) {
					logger.info("索引错误！ 数据文件： " + entryName + " 在数据的索引中的字段顺序读取失败！");
				} else {
					for (int t = 0; t < listDataField.size(); t++) {
						Cell indexCell = indexRow.createCell(t);
						indexCell.setCellValue(listDataField.get(t));
						indexCell.setCellStyle(css);
					}
				}
			}
			in.close();
		} catch (IOException e) {
			flag = false;
			e.printStackTrace();
		}
		return flag;

	}

	protected boolean wbAddSheet(Workbook wb, CellStyle css, String sheetName, InputStream is) {
		boolean flag = true;
		Sheet sheet = wb.createSheet(sheetName);
		try {
			Row titleRow = null;
			int titleLength = 0;
			BufferedReader in = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			int line = 0;
			while (in.ready()) {
				++line;
				String str = in.readLine();
				String tmp = str + "\t" + "abcdefg";
				String[] info = tmp.split("\t");
				int length = info.length - 1;
				if (length > titleLength) {
					titleLength = length;
				}
				if (line == 1) {
					titleRow = sheet.createRow(0);
				}
				Row row = sheet.createRow(line);
				for (int j = 0; j < length; j++) {
					Cell cell = row.createCell(j);
					cell.setCellValue(info[j]);
					cell.setCellStyle(css);
				}
			}
			for (int t = 0; t < titleLength; t++) {
				Cell titleCell = titleRow.createCell(t);
				titleCell.setCellValue((1 + t));
				titleCell.setCellStyle(css);
			}
			in.close();
		} catch (IOException e) {
			flag = false;
			e.printStackTrace();
		}
		return flag;
	}

	public static boolean writeXmlFile(Document doc, File file) {
		boolean flag = false;
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setLineSeparator("\n");
		format.setEncoding("UTF-8");
		try {
			FileWriter fileWriter = new FileWriter(file, false);
			XMLWriter writer = new XMLWriter(fileWriter, format);
			writer = new XMLWriter(new FileWriter(file), format);
			writer.write(doc);
			fileWriter.close();
			writer.close();
			flag = true;
		} catch (IOException e) {
			flag = false;
		}
		return flag;
	}

}
