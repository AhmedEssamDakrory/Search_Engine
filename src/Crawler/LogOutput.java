import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogOutput {
	private static Logger logger;
	private static FileHandler fh;  
	
	public static void init() {
		logger = Logger.getLogger("MyLog");  
		try {
			fh = new FileHandler("crawlingLogFile");
			logger.addHandler(fh);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter);  
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	public static void printMessage(String message) {
		logger.info(message); 
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
	
	}
}
