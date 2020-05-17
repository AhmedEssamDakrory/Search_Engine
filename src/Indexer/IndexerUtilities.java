import com.mongodb.client.FindIterable;
import org.bson.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class IndexerUtilities {

    public static String readHtml(String path) {
        String content = "";
        try
        {
            content = new String ( Files.readAllBytes( Paths.get(path) ) );
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return content;
    }

    public static ArrayList<String> pullWebsites(FindIterable<Document> cursor){
        if (cursor.first() == null) return null;
        return (ArrayList<String>) cursor.first().get("urls");
    }
}
