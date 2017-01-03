/**
 * 
 */
package saturntools;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/** Try to read a marks spreadsheet using Apache POI
 * 
 * @author cmg
 *
 */
public class ReadMarksSpreadsheet {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length==0) {
			System.err.println("Usage: <marks-spreadsheet.xls> ...");
			System.exit(-1);
		}
		try {
			for (int ai=0; ai<args.length; ai++) {
				File f = new File(args[ai]);
				InputStream inp = new FileInputStream(f);
			    //InputStream inp = new FileInputStream("workbook.xlsx");

				Biff8EncryptionKey.setCurrentUserPassword("marks0910");
			    Workbook wb = WorkbookFactory.create(inp);
			    Sheet sheet = wb.getSheetAt(0);
			    System.out.println("Sheet: "+sheet);
			    System.out.println("FirstRow: "+sheet.getFirstRowNum());
			    System.out.println("LastRow: "+sheet.getLastRowNum());
			    System.out.println("LeftCol: "+sheet.getLeftCol());
			    for (int ri=sheet.getFirstRowNum(); ri<sheet.getLastRowNum(); ri++) {
			    	Row row = sheet.getRow(ri);
			    	if (row==null) {
			    		System.out.println("Row "+ri+" null");
			    		continue;
			    	}
			    	System.out.println("Row "+ri+", lastCell="+row.getLastCellNum()+": "+row);
			    	for (int ci=0; ci<row.getLastCellNum(); ci++) {
			    		Cell cell = row.getCell(ci);
			    		if (cell!=null) {
			    			System.out.println("Cell "+ci+": "+cell+" (type: "+cell.getCellType()+")");
			    			if (cell.getCellType()==Cell.CELL_TYPE_FORMULA) {
			    				System.out.println("  formula type="+cell.getCachedFormulaResultType());
			    				if (cell.getCachedFormulaResultType()==Cell.CELL_TYPE_NUMERIC)
			    					System.out.println("  number="+cell.getNumericCellValue());
			    				//cell.getCellStyle().g
		    					//System.out.println("  string="+cell.getStringCellValue());
			    			}
			    		}
			    	}
			    	
			    }
			}
		}
		catch (Exception e) {
			System.err.println("Error: "+e);
			e.printStackTrace(System.err);
		}
	}

}
