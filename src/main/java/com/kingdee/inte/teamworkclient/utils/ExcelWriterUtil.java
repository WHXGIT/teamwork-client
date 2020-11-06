package com.kingdee.inte.teamworkclient.utils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * description: Excel 写文件
 *
 * @author Administrator
 * @date 2020/9/24 10:19
 */
public class ExcelWriterUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExcelWriterUtil.class);

	public static boolean updateWrite(String sourcePath, String targetPath, int sheetIndex, int headRowSize, List<Map<String, String>> data)
			throws Exception {
		boolean result = false;
		Workbook workbook = null;
		try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(sourcePath));) {
			// 直接传入输入流即可，此时excel就已经解析了
			workbook = new XSSFWorkbook(inputStream);
			Sheet sheet = workbook.getSheetAt(sheetIndex);
			List<Row> rowList = ExcelReaderUtil.getRowList(sheet, headRowSize);
			for (int i = 0; i < rowList.size(); i++) {
				Map<String, Cell> cellMap = ExcelReaderUtil.getCellMap(rowList.get(i));
				for (Map.Entry<String, Cell> entry : cellMap.entrySet()) {
					setCellValue(entry.getValue(), data.get(i).get(entry.getKey()));
				}
			}
			FileOutputStream fos = new FileOutputStream(targetPath);
			workbook.write(fos);
			fos.close();
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (workbook != null) {
				try {
					workbook.close();
				} catch (IOException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
		return result;
	}

	public static Cell setCellValue(Cell cell, String value) {
		if (cell == null) {
			return null;
		}
		switch (cell.getCellType()) {
			case STRING:
				cell.setCellValue(value);
				break;
			case FORMULA:
				break;
			case NUMERIC:
				break;
			case BLANK:
				cell.setBlank();
				break;
			case BOOLEAN:
				break;
			case ERROR:
				break;
			default:
				break;
		}
		return cell;
	}


	//创建一个不存在的excel文件
	private static Workbook createNewWorkbook(String targetPath) throws Exception {
		Workbook wb = null;
		if (targetPath.endsWith(".xls")) {
			wb = new HSSFWorkbook();
		} else if (targetPath.endsWith(".xlsx")) {
			wb = new XSSFWorkbook();
		} else {
			throw new Exception("文件类型错误！");
		}
		try {
			OutputStream output = new FileOutputStream(targetPath);
			wb.write(output);
		} catch (FileNotFoundException e) {
			LOGGER.error("文件创建失败，失败原因为：" + e.getMessage());
			throw new FileNotFoundException();
		}
		LOGGER.info(targetPath + "文件创建成功！");
		return wb;
	}

	public static boolean write(String targetPath, List<Map<String, String>> data) {
		boolean result = false;
		// 指定excel文件，创建缓存输入流
		Workbook workbook = null;
		File file = new File(targetPath);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));) {

			// 直接传入输入流即可，此时excel就已经解析了
			workbook = new XSSFWorkbook(inputStream);

			String filePath = "E:\\workspace\\testdir\\引入模板.xlsx";
			FileOutputStream fos = new FileOutputStream(filePath);
			workbook.write(fos);
			fos.close();
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (workbook != null) {
				try {
					workbook.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
}
