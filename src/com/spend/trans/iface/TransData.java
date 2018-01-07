/**
 * 
 */
package com.spend.trans.iface;

import java.io.File;

/**
 * @author Administrator
 *
 */
public interface TransData {

	// // 初始化配置文件
	// public void init(Map<String, String> mapSysInfo, File fileConf);

	// 初始化配置文件
	public void init(File fileConf);

	// 待处理的文件file此时应该在process目录。folderTmpWrite是写文件的临时目录，此目录与读文件的临时目录是分隔开的（不同目录）
	public void transData(File file, File folderTmpWrite, File folderOut, File folderBad);

}
