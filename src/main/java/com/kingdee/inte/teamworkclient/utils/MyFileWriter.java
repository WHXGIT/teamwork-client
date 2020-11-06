package com.kingdee.inte.teamworkclient.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * description: 文件填写器
 *
 * @author RD_haoxin_wang
 * @date 2020/9/2 17:28
 */
public class MyFileWriter {
	private static final Logger LOGGER = LoggerFactory.getLogger(MyFileWriter.class);

	public static void write(String path, String filename, List<String> lines) {
		// 相对路径，如果没有则要建立一个新的output。txt文件
		File writename = new File(path + File.separator + filename);
		BufferedWriter out = null;
		try {
			if (!writename.exists()) {
				writename.createNewFile();
			}
			out = new BufferedWriter(new FileWriter(writename));
			for (String line : lines) {
				out.write(line);
				out.write("\r\n");
			}
			out.flush();
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
	}


}
