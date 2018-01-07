package com.spend.trans.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
//import java.nio.file.Files;
//import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BigStatToSp5 {
	/**
	 * 将ds统计生成的WZ4.1的统计转换为SP5版本
	 *  
	 **/

	public static Logger logger = LogManager.getLogger();

	public static String split = "\t";
	public static String feed = "\n";
	public static Charset charset = Charset.forName("UTF-8");

	public static void TransFile(File file, File folderTmp, File folderOut, File folderBad) {
		String fileName = file.getName();
		if (fileName.startsWith("NW_FILEDATANUM_DETAIL")) {
			FileDataCut(fileName, file, folderTmp, folderOut, folderBad, 23, 23);
		} else if (fileName.startsWith("NW_DATANUM_STAT")) {
			FileDataCut(fileName, file, folderTmp, folderOut, folderBad, 15, 11);
		} else if (fileName.startsWith("NW_DC_OUTPUT_STAT")) {
			FileDataCut(fileName, file, folderTmp, folderOut, folderBad, 17, 12);
		} else if (fileName.startsWith("NW_EXCEPTIONALDATA_DETAIL")) {
			FileDataCut(fileName, file, folderTmp, folderOut, folderBad, 17, 11);
		} else if (fileName.startsWith("NW_EXCEPTIONALDATA_STAT")) {
			FileDataCut(fileName, file, folderTmp, folderOut, folderBad, 15, 13);
		} else {
			logger.error("Not right file to trans! Detail: " + fileName);
			try {
				FileUtils.moveFileToDirectory(file, folderBad, true);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
		if (file.exists()) {
			FileUtils.deleteQuietly(file);
		}
	}

	public static void FileDataCut(String fileName, File file, File folderTmp, File folderOut, File folderBad,
			int oldNum, int newNum) {
		try {
			File fileTmp = new File(folderTmp + File.separator + fileName);
			FileOutputStream fos = new FileOutputStream(fileTmp, true);
			BufferedReader br = new BufferedReader(new FileReader(file));
			while (br.ready()) {
				String str = br.readLine();
				String info = str + split + "ABCDEFGHIGK";
				String[] data = info.split(split);
				int size = data.length - 1;
				String strNew = "";
				if (oldNum > newNum) {
					if (size < newNum) {
						strNew = str;
					} else {
						StringBuffer sb = new StringBuffer();
						for (int i = 0; i < newNum; i++) {
							if (i == (newNum - 1)) {
								sb.append(data[i]);
							} else {
								sb.append(data[i]).append(split);
							}
						}
						strNew = sb.toString();
					}
				} else {
					strNew = str;
				}
				fos.write(strNew.getBytes(charset));
				fos.write(feed.getBytes(charset));
			}
			br.close();
			fos.close();
			try {
				FileUtils.moveFileToDirectory(fileTmp, folderOut, true);
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}
}
