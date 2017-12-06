import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Defines compress and uncompress static methods for Huffman encoding.
 *
 * @author Oliver on 11/13/2017
 */
public class Huffman {
    // Returns a mapping from each unique symbol to its count.
    private static HashMap<Byte, Integer> getFreqTable(byte[] symbols) {
        HashMap<Byte, Integer> freqTable = new HashMap<>();
        byte[] symbolsCopy = Arrays.copyOf(symbols, symbols.length);
        Arrays.sort(symbolsCopy);
        byte prevSymbol = symbolsCopy[0];
        int freq = 0;
        for (byte symbol : symbolsCopy) {
            if (symbol == prevSymbol) {
                freq++;
            } else {
                freqTable.put(prevSymbol, freq);
                freq = 1;
                prevSymbol = symbol;
            }
        }
        freqTable.put(prevSymbol, freq);
        return freqTable;
    }

    // Returns the parent node of the Huffman tree.
    private static Node getHuffmanTree(HashMap<Byte, Integer> freqTable) {
        PriorityQueue<Node> huffmanForest = new PriorityQueue<>();
        for (Map.Entry<Byte, Integer> entry : freqTable.entrySet()) {
            huffmanForest.add(new Node(entry.getKey(), entry.getValue()));
        }
        while (huffmanForest.size() > 1) {
            huffmanForest.add(huffmanForest.poll().combineWith(huffmanForest.poll()));
        }
        return huffmanForest.poll();
    }

    // Returns a mapping from each symbol in the Huffman Tree
    // to its codeword represented by a string of '1' and '0'.
    private static HashMap<Byte, String> getCodewords(Node huffmanTree) {
        HashMap<Byte, String> codewords = new HashMap<>();
        getCodewordsHelper(codewords, huffmanTree, "");
        return codewords;
    }

    // Recursively populate the given symbol_table with all symbols and their corresponding encoding strings.
    private static void getCodewordsHelper(HashMap<Byte, String> symbolTable,
                                           Node node, String codeword) {
        if (node.isTerminal()) {
            symbolTable.put(node.symbol, codeword);
        } else {
            getCodewordsHelper(symbolTable, node.childLeft, codeword + '0');
            getCodewordsHelper(symbolTable, node.childRight, codeword + '1');
        }
    }

    // Returns the bits resulting from encoding all symbols. Trailing zeroes are ignored.
    private static BitSet getEncodedBits(byte[] symbols, HashMap<Byte, String> codewords) {
        BitSet encodedBits = new BitSet();
        int encodedBitsIndex = 0;
        for (byte symbol : symbols) {
            String codeword = codewords.get(symbol);
            for (int i = 0; i < codeword.length(); i++) {
                if (codeword.charAt(i) == '1')
                    encodedBits.set(encodedBitsIndex);
                encodedBitsIndex++;
            }
        }
        return encodedBits;
    }

    // Returns the symbols resulting from decoding all bits.
    // nEncodedBits accounts for the trailing zeroes that were ignored by the BitSet.
    private static byte[] getDecodedBytes(BitSet encodedBits, int nEncodedBits,
                                          HashMap<Byte, Integer> freqTable,
                                          Node huffmanTree) {
        int nDecodedBytes = 0;
        for (int freq : freqTable.values())
            nDecodedBytes += freq;
        byte[] decodedBytes = new byte[nDecodedBytes];
        for (int byteIndex = 0, bitIndex = 0; byteIndex < nDecodedBytes && bitIndex < nEncodedBits; byteIndex++) {
            Node currentNode = huffmanTree;
            while (!currentNode.isTerminal()) {
                if (encodedBits.get(bitIndex)) {
                    currentNode = currentNode.childRight;
                } else {
                    currentNode = currentNode.childLeft;
                }
                bitIndex++;
            }
            decodedBytes[byteIndex] = currentNode.symbol;
        }
        return decodedBytes;
    }

    // Returns the true number of bits resulting from encoding all symbols. Accounts for trailing zeroes.
    private static int getNEncodedBits(HashMap<Byte, Integer> freqTable,
                                       HashMap<Byte, String> symbolTable) {
        int nEncodedBits = 0;
        for (byte symbol : freqTable.keySet()) {
            nEncodedBits += symbolTable.get(symbol).length() * freqTable.get(symbol);
        }
        return nEncodedBits;
    }

    /**
     * Encodes the file at {@code inPath} and stores the result into a new file
     * with extension '.huffman'.
     *
     * @param inPath the path of the file to be encoded
     * @throws IOException if an error occurs during file IO
     */
    public static void encodeFile(String inPath) throws IOException {
        long start = System.nanoTime();
        File inFile = new File(inPath);
        byte[] inBytes = Files.readAllBytes(inFile.toPath());

        HashMap<Byte, Integer> freqTable = getFreqTable(inBytes);
        Node huffmanTree = getHuffmanTree(freqTable);
        HashMap<Byte, String> symbolTable = getCodewords(huffmanTree);
        BitSet encodedBits = getEncodedBits(inBytes, symbolTable);

        String outFilename = inPath.substring(0, inPath.lastIndexOf('.')) + ".huffman";
        DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(outFilename));
        dataOutputStream.writeInt(getNEncodedBits(freqTable, symbolTable));
        dataOutputStream.writeInt(freqTable.size());
        for (Map.Entry<Byte, Integer> entry : freqTable.entrySet()) {
            dataOutputStream.writeByte(entry.getKey());
            dataOutputStream.writeInt(entry.getValue());
        }
        dataOutputStream.write(encodedBits.toByteArray());
        dataOutputStream.close();
    }

    /**
     * Decodes the file at {@code inPath} and stores the result into a new file
     * with extension '.txt'.
     *
     * @param inPath the path of the file to be decoded
     * @throws IOException if an error occurs during file IO
     */
    public static void decodeFile(String inPath) throws IOException {
        long start = System.nanoTime();
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(inPath));

        int nEncodedBits = dataInputStream.readInt();
        int nSymbols = dataInputStream.readInt();
        HashMap<Byte, Integer> freqTable = new HashMap<>();
        for (int i = 0; i < nSymbols; i++) {
            freqTable.put(dataInputStream.readByte(), dataInputStream.readInt());
        }
        byte[] encodedBytes = new byte[nEncodedBits / 8 + nEncodedBits % 8];
        dataInputStream.read(encodedBytes);
        dataInputStream.close();
        BitSet encodedBits = BitSet.valueOf(encodedBytes);

        Node huffmanTree = getHuffmanTree(freqTable);
        byte[] decodedBytes = getDecodedBytes(encodedBits, nEncodedBits, freqTable, huffmanTree);

        String outFilename = inPath.substring(0, inPath.lastIndexOf('.')) + ".txt";
        File outFile = new File(outFilename);
        Files.write(outFile.toPath(), decodedBytes);
    }

    /**
     * Command line client for compressing and uncompressing.
     *
     * @param args specifies compress/uncompress and the file to use.
     * @throws IOException if an error occurs during file IO.
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 2) {
            if (args[0].equals("encode")) {
                encodeFile(args[1]);
                return;
            } else if (args[0].equals("decode")) {
                decodeFile(args[1]);
                return;
            }
        }
        System.out.println("Usage: $ java Huffman ('encode'/'decode') (file path)");
    }
}

/**
 * Implements a simple binary tree structure.
 */
class Node implements Comparable<Node> {
    int freq;
    byte symbol;
    Node childLeft, childRight;

    /**
     * Instantiates a new terminal {@code Node} with given symbol and frequency.
     *
     * @param symbol the symbol
     * @param freq   the frequency
     */
    Node(byte symbol, int freq) {
        this(symbol, freq, null, null);
    }

    private Node(int freq, Node childLeft, Node childRight) {
        this((byte) 0, freq, childLeft, childRight);
    }

    private Node(byte symbol, int freq, Node childLeft, Node childRight) {
        this.freq = freq;
        this.symbol = symbol;
        this.childLeft = childLeft;
        this.childRight = childRight;
    }

    /**
     * Print the tree at the given root {@code Node}.
     *
     * @param node the root node
     */
    static void printTree(Node node) {
        printTreeHelper(node, 0);
    }

    // Recursively print the given Node and all its children using an inorder traversal and with visual indent.
    private static void printTreeHelper(Node node, int indent) {
        if (node.symbol != 0) {
            System.out.println(String.join("", Collections.nCopies(indent, "    ")) + node.freq + ": " + node.symbol);
        } else {
            if (node.childLeft != null) {
                printTreeHelper(node.childLeft, indent + 1);
            }
            System.out.println(String.join("", Collections.nCopies(indent, "    ")) + node.freq);
            if (node.childRight != null) {
                printTreeHelper(node.childRight, indent + 1);
            }
        }
    }

    /**
     * Returns true is the {@code Node} has no children. Otherwise false.
     */
    boolean isTerminal() {
        return childLeft == null && childRight == null;
    }

    /**
     * Returns a new parent {@code Node} which contains the given children,
     * has a freq that is the sum of its children, and no symbol.
     *
     * @param that other {@code Node} to be combined with
     * @return combined {@code Node}
     */
    Node combineWith(Node that) {
        return new Node(this.freq + that.freq, this, that);
    }

    public int compareTo(Node that) {
        return this.freq - that.freq;
    }
}