import java.io.File;
import java.io.IOException;

/**
 * @author Oliver on 12/4/2017
 */
public class HuffmanTestEncode {
    public static void main(String[] args) throws IOException{
        if (args.length == 1 && new File(args[0]).exists()) {
            Huffman.encodeFile(args[0]);
        } else {
            System.out.println("Usage: $ java HuffmanTestEncode (file path)");
        }
    }
}
