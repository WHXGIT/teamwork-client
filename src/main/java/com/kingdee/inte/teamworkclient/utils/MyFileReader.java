package com.kingdee.inte.teamworkclient.utils;

import com.alibaba.druid.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * description: 文件读取器
 *
 * @author RD_haoxin_wang
 * @date 2020/9/2 17:06
 */
public class MyFileReader {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyFileReader.class);

	public static List<String> getAllLines(File file) {
		List<String> lineList = new ArrayList<>();
		try {
			BufferedReader textFile = new BufferedReader(new FileReader(file));
			String lineDta = "";

			//第三步：将文档的下一行数据赋值给lineData，并判断是否为空，若不为空则输出
			while ((lineDta = textFile.readLine()) != null) {
				lineList.add(lineDta);
			}
		} catch (FileNotFoundException e) {
			LOGGER.error("没有找到指定文件");
		} catch (IOException e) {
			LOGGER.error("文件读写出错");
		}
		return lineList;
	}

	public static List<String> getWordContainsLinesIgnoreCase(File file, String word) {
		List<String> lineList = new ArrayList<>();
		try {
			BufferedReader textFile = new BufferedReader(new FileReader(file));
			String lineDta = "";

			//第三步：将文档的下一行数据赋值给lineData，并判断是否为空，若不为空则输出
			while ((lineDta = textFile.readLine()) != null) {
				if (lineDta.toLowerCase().contains(word.toLowerCase())) {
					lineList.add(lineDta);
				}
			}
		} catch (FileNotFoundException e) {
			LOGGER.error("没有找到指定文件");
		} catch (IOException e) {
			LOGGER.error("文件读写出错");
		}
		return lineList;
	}

	public static List<String> findContainsWordLine(List<File> fileList) {
		List<String> allRelativeLines = new ArrayList<>();
		fileList.forEach(item -> {
			List<String> list = getAllLines(item);
			allRelativeLines.addAll(list);
		});
		return allRelativeLines;
	}

	public static List<File> getFiles(String path, String suffix) {
		List<File> fileList = new ArrayList<>();
		File file = new File(path);
		// 获取目录下的所有文件或文件夹
		File[] files = file.listFiles();
		// 如果目录为空，直接退出
		if (files == null || files.length <= 0) {
			return null;
		}
		// 遍历，目录下的所有文件
		for (File f : files) {
			if (f.isFile()) {
				if (suffix == null) {
					fileList.add(f);
				} else {
					if (StringUtils.equals(getFileSuffix(f).toLowerCase(), suffix.toLowerCase())) {
						fileList.add(f);
					}
				}
			} else if (f.isDirectory()) {
				List<File> childFileList = getFiles(f.getAbsolutePath(), suffix);
				if (childFileList != null && childFileList.size() > 0) {
					fileList.addAll(childFileList);
				}
			}
		}
		return fileList;
	}

	public static String getFileSuffix(File file) {
		String fileName = file.getName();
		String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
		return suffix;
	}

}
