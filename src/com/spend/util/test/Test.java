package com.spend.util.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Test {

	private static Logger logger = LogManager.getLogger();
	
	
	public static void main(String[] args) throws Exception{
		File file = new File("F:\\APP_SVN\\java\\Spend\\备份\\FilesMonitor.java");
		
		System.out.println(file.getParent());
		System.out.println(file.getParentFile().getName());
	}
	
	public static void main2(String[] args) throws Exception{
		
		Class<?> a  = Class.forName("com.plugin.trans.GabZipAddIndexKey");
		System.out.println(a.toString());
		System.out.println(a.newInstance().toString());
		logger.info(a.toString());
//		Class<?> b  = Class.forName("com.plugin.trans.GabZipAddIndexKey");
		
	}
	

	public static void main1(String[] args)
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		// TODO Auto-generated method stub

		String packagePath = "./plugin/";
		File folder = new File(packagePath);

		String[] listFile = folder.list();

		if (listFile == null) {

		} else {
			if (listFile.length > 0) {

				URL[] listURL = new URL[listFile.length];

				for (int i = 0; i < listFile.length; i++) {
					File file = new File(packagePath + File.separator + listFile[i]);
					URL url = (file).toURI().toURL();
					System.out.println(url);
					listURL[i] = url;
				}
				
				System.out.println("--------------------------------------------------------------");

//				ClassLoader loader = new URLClassLoader(listURL);
//				Class<?> cls = loader.loadClass("com.test.consumer.TransFile");
//				System.out.println(cls);
//				
//				Class<?> cls2 = loader.loadClass("com.test.consumer.TransData");
//				System.out.println(cls2);
//				
//				Class<?> cls3 = loader.loadClass("com.find.FindWifiData");
//				System.out.println(cls3);

			} else {

			}
		}

		// Class<? extends TransData> cls = (Class<? extends TransData>)
		// loader.loadClass("com.engine.consumer.TransFile");
		//
		// TransData obj = cls.newInstance();

		// String packagePath = "com.plugin.trans.GabZipAddIndexKey";
		// String fileConfPath = "conf";
		// File fileConf = new File(fileConfPath);
		//
		// File file =null;
		// File folderTmp =null;
		// File folderOut =null;
		// File folderBad =null;
		//
		// try {
		// Class<?> cls = Class.forName(packagePath);
		// Object obj = cls.newInstance();
		// if (obj instanceof TransData) {
		// TransData transData = (TransData) obj;
		// transData.init(fileConf);
		//// transData.transData(file, folderTmp, folderOut, folderBad);
		// } else {
		// logger.error("The class in the package not instanceof ReadData!
		// Detail: " + fileConfPath);
		// System.exit(1);
		// }
		// } catch (ClassNotFoundException e) {
		// logger.error(e.toString());
		// e.printStackTrace();
		// System.exit(1);
		// } catch (InstantiationException e) {
		// logger.error(e.toString());
		// e.printStackTrace();
		// System.exit(1);
		// } catch (IllegalAccessException e) {
		// logger.error(e.toString());
		// e.printStackTrace();
		// System.exit(1);
		// }

	}

}