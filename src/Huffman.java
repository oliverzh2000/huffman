import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author Oliver on 11/13/2017
 */
public class Huffman {

    /**
     * Returns a mapping from each {@code Character} in the given {@code char[]} to its number of occurrences.
     */
    public static HashMap<Character, Integer> getFreqTableOld(char[] chars) {
        HashMap<Character, Integer> freqTable = new HashMap<>();
        for (char character : chars) {
            freqTable.putIfAbsent(character, 0);
            freqTable.computeIfPresent(character, (key, value) -> value + 1);
        }
        return freqTable;
    }

    public static HashMap<Character, Integer> getFreqTableNew(char[] chars) {
        HashMap<Character, Integer> freqTable = new HashMap<>();
        char[] charsCopy = Arrays.copyOf(chars, chars.length);
        Arrays.sort(charsCopy);
        char prevChar = charsCopy[0];
        int freq = 0;
        for (char ch : charsCopy) {
            if (ch == prevChar) {
                freq++;
            } else {
                freqTable.put(prevChar, freq);
                freq = 1;
                prevChar = ch;
            }
        }
        freqTable.put(prevChar, freq);
        return freqTable;
    }

    /**
     * Returns the topmost {@code Node} of the Huffman tree generated from the given {@code String}.
     */
    public static Node getHuffmanTree(HashMap<Character, Integer> freqTable) {
        PriorityQueue<Node> huffmanForest = new PriorityQueue<>();
        for (Map.Entry<Character, Integer> entry : freqTable.entrySet()) {
            huffmanForest.add(new Node(entry.getKey(), entry.getValue()));
        }
//        freqTable.forEach((character, freq) -> huffmanForest.add(new Node(character, freq)));
        while (huffmanForest.size() > 1) {
            huffmanForest.add(huffmanForest.poll().combineWith(huffmanForest.poll()));
        }
        return huffmanForest.poll();
    }

    /**
     * Returns a mapping from the {@code Character} of each terminal {@code Node} in the given huffman tree
     * to its codeword. The codeword of each {@code Node} is determined by the
     * path taken to reach it from the root of the huffman tree.
     *
     * @param huffmanTree the Huffman tree
     * @return the mapping from each {@code Character} to its codeword
     */
    public static HashMap<Character, boolean[]> getSymbolTable(Node huffmanTree) {
        HashMap<Character, String> codewordStrings = new HashMap<>();
        populateSymbolTable(codewordStrings, huffmanTree, "");
        HashMap<Character, boolean[]> codeWordBooleanArrays = new HashMap<>();
        for (Map.Entry<Character, String> entry : codewordStrings.entrySet()) {
            boolean[] codewordArray = new boolean[entry.getValue().length()];
            char[] codeword = entry.getValue().toCharArray();
            for (int i = 0; i < codeword.length; i++) {
                codewordArray[i] = (codeword[i] == '1');
            }
            codeWordBooleanArrays.put(entry.getKey(), codewordArray);
        }
        return codeWordBooleanArrays;
    }

    // Recursively populate the given symbol_table with all symbols and their corresponding codeword strings.
    private static void populateSymbolTable(HashMap<Character, String> symbolTable,
                                            Node node, String codeword) {
        if (node.isTerminal()) {
            symbolTable.put(node.character, codeword);
        } else {
            populateSymbolTable(symbolTable, node.childLeft, codeword + '0');
            populateSymbolTable(symbolTable, node.childRight, codeword + '1');
        }
    }

    //    public static char[] getDecodedChars(BitSet encodedBits, int nEncodedBits, int nDecodedChars, Node huffmanTree) {
//        int nTrailingZeroes = nEncodedBits - encodedBits.length();
//        char[] decodedChars = new char[nDecodedChars];
//        for (int charIndex = 0, bitIndex = 0; charIndex < decodedChars.length && bitIndex < nEncodedBits; charIndex++) {
//            Node currentNode = huffmanTree;
//            while (!currentNode.isTerminal()) {
//                if (encodedBits.get(bitIndex)) {
//                    currentNode = currentNode.childRight;
//                } else {
//                    currentNode = currentNode.childLeft;
//                }
//                bitIndex++;
//            }
//            decodedChars[charIndex] = currentNode.character;
//        }
//        return decodedChars;
//    }

    /**
     * Returns a {@code long[]} representing the bits from encoding the given string with the given symbol table.
     *
     * @param chars the {@code char[]} to encode
     * @return the encoded bits
     */
    public static BitSet getEncodedBits(char[] chars, HashMap<Character, boolean[]> symbolTable) {
        BitSet encodedBits = new BitSet();
        int encodedBitsIndex = 0;
        for (char character : chars) {
            boolean[] codeword = symbolTable.get(character);
            for (boolean codewordBit : codeword) {
                encodedBits.set(encodedBitsIndex, codewordBit);
                encodedBitsIndex++;
            }
        }
        return encodedBits;
    }

    public static char[] getDecodedChars(BitSet encodedBits, int nEncodedBits,
                                         HashMap<Character, Integer> freqTable,
                                         Node huffmanTree) {
        int nDecodedChars = freqTable.values().stream().reduce(0, (freq, sum) -> freq + sum);
        int nTrailingZeroes = nEncodedBits - encodedBits.length();
        char[] decodedChars = new char[nDecodedChars];
        for (int charIndex = 0, bitIndex = 0; charIndex < decodedChars.length && bitIndex < nEncodedBits; charIndex++) {
            Node currentNode = huffmanTree;
            while (!currentNode.isTerminal()) {
                if (encodedBits.get(bitIndex)) {
                    currentNode = currentNode.childRight;
                } else {
                    currentNode = currentNode.childLeft;
                }
                bitIndex++;
            }
            decodedChars[charIndex] = currentNode.character;
        }
        return decodedChars;
    }

    /**
     * Returns the true number of encoded bits from the given frequency and symbol table,
     * by accounting for the trailing zeroes that the BitSet does not store.
     *
     * @param freqTable   the frequency table
     * @param symbolTable the symbol table
     * @return the number of bits in the resulting encoding
     */
    public static int getEncodedBitsLength(HashMap<Character, Integer> freqTable,
                                           HashMap<Character, boolean[]> symbolTable) {
        int nEncodedBits = 0;
        for (char character : freqTable.keySet()) {
            nEncodedBits += symbolTable.get(character).length * freqTable.get(character);
        }
        return nEncodedBits;
    }

    /**
     * Compress (enocode) the contents of the file located at {@code inFilename}
     * and write the results to the file located at {@code outFilename}.
     *
     * @param inFilename the path of the input file
     */
    public static void compressFile(String inFilename) throws IOException {
        long start = System.nanoTime();
        File inFile = new File(inFilename);
        FileReader fileReader = new FileReader(inFilename);
        char[] inChars = new char[(int) inFile.length()];
        int nCharsRead = fileReader.read(inChars);
        inChars = Arrays.copyOfRange(inChars, 0, nCharsRead);  // Trim the chars of any empty bytes.
        fileReader.close();
        System.out.println("read file " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        HashMap<Character, Integer> freqTable = getFreqTableNew(inChars);
        System.out.println("freq table " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        Node huffmanTree = getHuffmanTree(freqTable);
        System.out.println("huffman tree " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        HashMap<Character, boolean[]> symbolTable = getSymbolTable(huffmanTree);
        System.out.println("symbol table " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        BitSet encodedBits = getEncodedBits(inChars, symbolTable);
        System.out.println("encoded bits " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        ByteBuffer header = ByteBuffer.allocate(8 + freqTable.size() * 6);
        // File header consists of:
        // (int) x 1 - number of encoded bits, (int) x 1 - number of symbols,
        // (char, int) x (number of symbols) - symbol and frequency pairs.
        header.putInt(getEncodedBitsLength(freqTable, symbolTable));
        header.putInt(freqTable.size());
        freqTable.forEach((character, freq) -> {
            header.putChar(character);
            header.putInt(freq);
        });
        System.out.println("header " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        String outFilename = inFilename.substring(0, inFilename.lastIndexOf('.')) + ".huffman";
        FileOutputStream fileOutputStream = new FileOutputStream(outFilename);
        fileOutputStream.write(header.array());
        fileOutputStream.write(encodedBits.toByteArray());
        fileOutputStream.close();
        System.out.println("write file " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

//        System.out.println(freqTable);
//        StringBuilder s = new StringBuilder();
//        for (int i = 0; i < encodedBits.length(); i++)
//            s.append(encodedBits.get(i) ? 1 : 0);
//
//        System.out.println(getEncodedBitsLength(freqTable, symbolTable));
//        System.out.println(encodedBits.length() + " " + freqTable.size());
//        System.out.println(s);
//        System.out.println(Arrays.toString(encodedBits.toByteArray()));
//        System.out.println(encodedBits.length());
    }

    /**
     * Compress (enocode) the contents of the file located at {@code inFilename}
     * and write the results to the file located at {@code outFilename}.
     *
     * @param inFilename the path of the input file
     */
    public static void uncompressFile(String inFilename) throws IOException {
        long start = System.nanoTime();
        File file = new File(inFilename);
        FileInputStream fileInputStream = new FileInputStream(inFilename);

        byte[] headerBytes = new byte[8];
        fileInputStream.read(headerBytes);
        ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes);
        int nEncodedBits = headerBuffer.getInt();
        int nSymbols = headerBuffer.getInt();
        System.out.println("read header " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        // Each [(char) symbol, (int) freq] pair is 2 + 4 = 6 bytes.
        byte[] freqTableBytes = new byte[nSymbols * 6];
        fileInputStream.read(freqTableBytes);
        ByteBuffer freqTableBuffer = ByteBuffer.wrap(freqTableBytes);
        HashMap<Character, Integer> freqTable = new HashMap<>();
        for (int i = 8; i < 8 + nSymbols * 6; i += 6)
            freqTable.put(freqTableBuffer.getChar(), freqTableBuffer.getInt());
        System.out.println("freq table " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();
        Node huffmanTree = getHuffmanTree(freqTable);
        System.out.println("huffman tree " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        byte[] encodedBytes = new byte[(int) Math.ceil(nEncodedBits / 8.0)];
        fileInputStream.read(encodedBytes);
        BitSet encodedBits = BitSet.valueOf(encodedBytes);
        System.out.println("read encoded bits " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();
        char[] decodedChars = getDecodedChars(encodedBits, nEncodedBits, freqTable, huffmanTree);
        fileInputStream.close();
        System.out.println("decode " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

        String outFilename = inFilename.substring(0, inFilename.lastIndexOf('.')) + ".txt";
        File Outfile = new File(outFilename);
        FileWriter fileWriter = new FileWriter(Outfile);
        fileWriter.write(decodedChars);
        fileWriter.close();
        System.out.println("write file " + (System.nanoTime() - start) / 1000000);
        start = System.nanoTime();

//        System.out.println(nEncodedBits + " " + nSymbols);
//        System.out.println(freqTable);
//        System.out.println(encodedBits.length() + " " + freqTable.size());
//        StringBuilder s = new StringBuilder();
//        for (int i = 0; i < encodedBits.length(); i++)
//            s.append(encodedBits.get(i) ? 1 : 0);
//        System.out.println(s);
//        System.out.println(Arrays.toString(encodedBits.toByteArray()));
//        System.out.println(encodedBits.length());
//        System.out.println(Arrays.toString(decodedChars));
    }

    public static void main(String[] args) throws IOException {
        Huffman.compressFile("C:\\Users\\Oliver\\IdeaProjects\\Huffman\\src\\war_and_peace.txt");
//        System.out.println();
//        Huffman.uncompressFile("C:\\Users\\Oliver\\IdeaProjects\\Huffman\\src\\war_and_peace.huffman");
    }
}

class Node implements Comparable<Node> {
    int freq;
    char character;
    Node childLeft, childRight;

    public Node(char character, int freq, Node childLeft, Node childRight) {
        this.freq = freq;
        this.character = character;
        this.childLeft = childLeft;
        this.childRight = childRight;
    }

    public Node(char character, int freq) {
        this(character, freq, null, null);
    }

    public Node(int freq, Node childLeft, Node childRight) {
        this('\u0000', freq, childLeft, childRight);
    }

    public static void printTree(Node node, int indent) {
        if (node.character != 0) {
            System.out.println(String.join("", Collections.nCopies(indent, "    ")) + node.freq + ": " + node.character);
        } else {
            if (node.childLeft != null) {
                printTree(node.childLeft, indent + 1);
            }
            System.out.println(String.join("", Collections.nCopies(indent, "    ")) + node.freq);
            if (node.childRight != null) {
                printTree(node.childRight, indent + 1);
            }
        }
    }

    public boolean isTerminal() {
        return childLeft == null && childRight == null;
    }

    public int compareTo(Node other) {
        return this.freq - other.freq;
    }

    public Node combineWith(Node other) {
        return new Node(this.freq + other.freq, this, other);
    }
}