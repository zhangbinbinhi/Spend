package com.spend.trans.plugin;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GabZipSeparateByCode {

	/**
	 * 自動根據文件信息，創建目錄并移動到目的目錄
	 **/

	public static Logger logger = LogManager.getLogger();

	public static void TransFile(File file, File folderTmp, File folderOut, File folderBad) {
		String fileName = file.getName();
		String extension = FilenameUtils.getExtension(fileName);
		if (extension.toLowerCase().equals("zip")) {
			String baseName = FilenameUtils.getBaseName(fileName);
			String[] fileInfo = baseName.split("-");
			if (fileInfo.length == 6) {
				String destCode = fileInfo[3];
				File folderCode = new File(folderOut.getAbsolutePath() + File.separator + destCode);
				if (!folderCode.exists()) {
					folderCode.mkdirs();
				}
				try {
					FileUtils.moveFileToDirectory(file, folderCode, true);
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			} else {
				logger.error("Egnore Bad file: " + fileName);
			}
		}
	}
}
