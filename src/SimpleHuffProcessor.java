/*  Student information for assignment:
 *
 *  On MY honor, Tri Nguyen, this programming assignment is MY own work
 *  and I have not provided this code to any other student.
 *
 *  Number of slip days used: 2
 *
 *  Student 1 (Student whose turnin account is being used)
 *  UTEID: ttn2797
 *  email address: tritn928@gmail.com
 *  Grader name: Nina
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TreeMap;

public class SimpleHuffProcessor implements IHuffProcessor {

	private IHuffViewer myViewer;
	private TreeMap<Integer, Integer> freqMap;
	private HuffmanTree hfTree;
	private int bitsInInput;
	private int bitsInOutput;
	private boolean countsFormat;

	/**
	 * Preprocess data so that compression is possible --- count characters/create
	 * tree/store state so that a subsequent call to compress will work. The
	 * InputStream is <em>not</em> a BitInputStream, so wrap it int one as needed.
	 * 
	 * @param in           is the stream which could be subsequently compressed
	 * @param headerFormat a constant from IHuffProcessor that determines what kind
	 *                     of header to use, standard count format, standard tree
	 *                     format, or possibly some format added in the future.
	 * @return number of bits saved by compression or some other measure Note, to
	 *         determine the number of bits saved, the number of bits written
	 *         includes ALL bits that will be written including the magic number,
	 *         the header format number, the header to reproduce the tree, AND the
	 *         actual data.
	 * @throws IOException if an error occurs while reading from the input file.
	 */
	public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
		BitInputStream reader = new BitInputStream(in);
		bitsInInput = in.available() * 8;
		showString("Starting preprocessing...");
		showString("Creating frequency mapping and huffman tree...");
		freqMap = createFreqMap(reader);
		PriorityQueue<TreeNode> prioQueue = createQueue(freqMap);
		hfTree = new HuffmanTree(prioQueue);
		// print out result
		for (Integer i : hfTree.huffMapping.keySet()) {
			showString("Value: " + i + ", equivalent char: " + (char) i.intValue() + ", frequency: "
					+ freqMap.get(i) + ", new code: " + hfTree.huffMapping.get(i));
		}
		showString("Completed Preprocessing...");
		showString("Counting Bits saved...");
		countsFormat = headerFormat == STORE_COUNTS;
		bitsInOutput = countBitsOutput();
		showString("Would save " + (bitsInInput - bitsInOutput) + " bits.");
		reader.close();
		return bitsInInput - bitsInOutput;
	}

	// Counts the number of bits saved by this compression
	// Must be called after preprocessing.
	private int countBitsOutput() {
		// add actual data
		bitsInOutput = 0;
		for (Integer i : hfTree.huffMapping.keySet()) {
			bitsInOutput += freqMap.get(i) * hfTree.huffMapping.get(i).length();
		}
		// add magic number identifier and header format identifier
		bitsInOutput += BITS_PER_INT;
		bitsInOutput += BITS_PER_INT;
		// add header data
		if (countsFormat) {
			bitsInOutput += BITS_PER_INT * 256;
		} else {
			// tree header format
			bitsInOutput += BITS_PER_INT;
			// header content
			int numLeafBits = hfTree.huffMapping.keySet().size() * (BITS_PER_WORD + 1);
			int numNodes = (hfTree.huffMapping.keySet().size() * 2) - 1;
			bitsInOutput += numLeafBits + numNodes;
		}
		return bitsInOutput;
	}

	// Uses a frequency map to create a priority queue of tree nodes
	private PriorityQueue<TreeNode> createQueue(TreeMap<Integer, Integer> freqMap)
			throws IOException {
		PriorityQueue<TreeNode> result = new PriorityQueue<>();
		// Create a tree node for every byte representation,
		// Add to queue
		for (Integer bytes : freqMap.keySet()) {
			TreeNode toAdd = new TreeNode(bytes, freqMap.get(bytes));
			result.enqueue(toAdd);
		}
		return result;
	}

	// Creates a map for the frequency of bytes in the file
	private TreeMap<Integer, Integer> createFreqMap(BitInputStream reader) throws IOException {
		TreeMap<Integer, Integer> result = new TreeMap<>();
		boolean reading = true;
		while (reading) {
			int currentByte = reader.readBits(BITS_PER_WORD);
			// Read until there isn't enough bits left
			if (currentByte == -1) {
				reading = false;
			} else {
				// If the byte has existed before, increment
				// otherwise create a new key,value pair.
				Integer frequency = result.get(currentByte);
				if (frequency == null)
					result.put(currentByte, 1);
				else {
					result.put(currentByte, frequency + 1);
				}
			}
		}
		// PEOF char at the end
		result.put(256, 1);
		return result;
	}

	// Private Nested Class for a huffman tree
	private static class HuffmanTree {

		// The root will store the entire tree and is how you acess its contents
		private TreeNode root;
		// Map to store the codes
		private TreeMap<Integer, String> huffMapping;

		// Constructor for a HuffmanTree when given a BitInputStream where the
		// information of the tree starts.
		public HuffmanTree(BitInputStream reader) throws IOException {
			root = hfTreeHelper(root, reader);
		}

		// Recursively recreates the huffman tree
		private TreeNode hfTreeHelper(TreeNode n, BitInputStream reader) throws IOException {
			// Read in one bit
			int currentBit = reader.readBits(1);
			if (currentBit == 0) {
				// If 0, the bit represents a node with two children
				TreeNode result = new TreeNode(hfTreeHelper(n, reader), 0, hfTreeHelper(n, reader));
				return result;
			} else if (currentBit == 1) {
				// If 1, the bit represents a leaf
				// Read in the next nine bits
				int nineNextBits = reader.readBits(BITS_PER_WORD + 1);
				TreeNode result = new TreeNode(nineNextBits, 0);
				return result;
			} else {
				throw new IOException("ERROR in STANDARD TREE FORMAT DATA ");
			}
		}

		// Constructor for a Huffman tree
		// Uses a priority queue of tree nodes
		public HuffmanTree(PriorityQueue<TreeNode> prioQueue) {
			if (prioQueue == null) {
				throw new IllegalArgumentException(
						"HuffmanTree constructor error. prioQueue is null");
			}
			// Takes the two tree nodes at the front of the queue
			// Makes a new tree node with those two nodes as children
			// new freq is the sum of the nodes' freqs
			while (prioQueue.size() >= 2) {
				TreeNode leftChild = prioQueue.dequeue();
				TreeNode rightChild = prioQueue.dequeue();
				int freqSum = leftChild.getFrequency() + rightChild.getFrequency();
				TreeNode newNode = new TreeNode(leftChild, freqSum, rightChild);
				// Counts the size, if adding two leafs, size increases by 3
				// if only one is a child, size increases by 2
				// if adding two subtrees, size increases by 1
				prioQueue.enqueue(newNode); // Adds back into queue
			}
			// When while loop is over, only one item will be left, the root
			root = prioQueue.dequeue();
			huffMapping = createMapping();
		}

		// Creates a map according to the huffman tree
		// Where every byte value is assigned a new string code
		private TreeMap<Integer, String> createMapping() {
			TreeMap<Integer, String> result = new TreeMap<>();
			String pathSoFar = "";
			traversalHelp(root, result, pathSoFar);
			return result;
		}

		// Recursively travels through the tree
		// Adds a 0 if travelling left, 1 if travelling right
		// When a leaf is reached, add it to the mapping
		private void traversalHelp(TreeNode n, TreeMap<Integer, String> result, String pathSoFar) {
			if (n != null) {
				if (n.isLeaf()) {
					result.put(n.getValue(), pathSoFar);
				}
				traversalHelp(n.getLeft(), result, pathSoFar + "0");
				traversalHelp(n.getRight(), result, pathSoFar + "1");
			}
		}
	}

	/**
	 * Compresses input to output, where the same InputStream has previously been
	 * pre-processed via <code>preprocessCompress</code> storing state used by this
	 * call. <br>
	 * pre: <code>preprocessCompress</code> must be called before this method
	 * 
	 * @param in    is the stream being compressed (NOT a BitInputStream)
	 * @param out   is bound to a file/stream to which bits are written for the
	 *              compressed file (not a BitOutputStream)
	 * @param force if this is true create the output file even if it is larger than
	 *              the input file. If this is false do not create the output file
	 *              if it is larger than the input file.
	 * @return the number of bits written.
	 * @throws IOException if an error occurs while reading from the input file or
	 *                     writing to the output file.
	 */
	public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
		if (!force && (bitsInInput - bitsInOutput) < 0) {
			myViewer.showError(
					"The compressed file would be larger than the original file, aborting compression.\n"
							+ "Select \"force compression\" option to compress");
			return 0;
		}
		showString("Now compressing file...");
		showString("Writing Huffman Identifier...");
		BitOutputStream writer = new BitOutputStream(out);
		writer.writeBits(BITS_PER_INT, MAGIC_NUMBER);

		showString("Writing Header...");
		if (countsFormat) {
			// Header for STORE_COUNTS
			writer.writeBits(BITS_PER_INT, STORE_COUNTS);
			// Content for STORE_COUNTS
			for (int i = 0; i < ALPH_SIZE; i++) {
				if (freqMap.get(i) != null)
					writer.writeBits(BITS_PER_INT, freqMap.get(i));
				else
					writer.writeBits(BITS_PER_INT, 0);
			}
		} else {
			// Header for STORE_TREE
			writer.writeBits(BITS_PER_INT, STORE_TREE);
			// Content for STORE_TREE
			int numLeafBits = hfTree.huffMapping.keySet().size() * 9;
			int numNodes = (hfTree.huffMapping.keySet().size() * 2) - 1;
			int bitsForTree = numLeafBits + numNodes;
			writer.writeBits(BITS_PER_INT, bitsForTree);
			treeHeaderHelper(writer, hfTree.root);
		}

		showString("Writing Data...");
		boolean reading = true;
		BitInputStream reader = new BitInputStream(in);
		while (reading) {
			int currentByte = reader.readBits(BITS_PER_WORD);
			// Read until there isn't enough bits left
			if (currentByte == -1) {
				reading = false;
			} else {
				// Get the new code, write in bits
				String code = hfTree.huffMapping.get(currentByte);
				for (int i = 0; i < code.length(); i++) {
					if (code.charAt(i) == '0')
						writer.writeBits(1, 0);
					else if (code.charAt(i) == '1')
						writer.writeBits(1, 1);
					else {
						myViewer.showError("Error in huffman mapping, cannot compress");
						return 0;
					}
				}
			}
		}
		// close reader
		reader.close();

		showString("Writing PEOF...");
		String PEOF_CODE = hfTree.huffMapping.get(256);
		for (int i = 0; i < PEOF_CODE.length(); i++) {
			if (PEOF_CODE.charAt(i) == '0')
				writer.writeBits(1, 0);
			else if (PEOF_CODE.charAt(i) == '1')
				writer.writeBits(1, 1);
			else {
				myViewer.showError("Error in writing PEOF");
				return 0;
			}
		}
		// close writer
		writer.close();
		showString("Compression Completed!");
		showString("Output file bits: " + bitsInOutput);
		return bitsInOutput;
	}

	// Recursively writes a bit representation of the HuffmanTree
	private void treeHeaderHelper(BitOutputStream writer, TreeNode n) {
		if (n != null) {
			// Write a 1 if it is a leaf, then write its value
			// else write a 0
			if (n.isLeaf()) {
				writer.writeBits(1, 1);
				writer.writeBits(BITS_PER_WORD + 1, n.getValue());
			} else {
				writer.writeBits(1, 0);
			}
			treeHeaderHelper(writer, n.getLeft());
			treeHeaderHelper(writer, n.getRight());
		}
	}

	/**
	 * Uncompress a previously compressed stream in, writing the uncompressed
	 * bits/data to out.
	 * 
	 * @param in  is the previously compressed data (not a BitInputStream)
	 * @param out is the uncompressed file/stream
	 * @return the number of bits written to the uncompressed file/stream
	 * @throws IOException if an error occurs while reading from the input file or
	 *                     writing to the output file.
	 */
	public int uncompress(InputStream in, OutputStream out) throws IOException {
		int bitsWritten = 0;
		showString("Uncompressing file...");
		BitInputStream reader = new BitInputStream(in);
		showString("Checking for Huffman Identifier...");
		int currentByte = reader.readBits(BITS_PER_INT);
		if (currentByte != MAGIC_NUMBER) {
			myViewer.showError("Huffman Identifier not found");
			return 0;
		}
		showString("Determining which header format is used...");
		currentByte = reader.readBits(BITS_PER_INT);
		if (currentByte == STORE_COUNTS) {
			countsFormat = true;
		} else if (currentByte == STORE_TREE) {
			countsFormat = false;
		} else {
			myViewer.showError("No valid header format found");
			return 0;
		}

		if (countsFormat) {
			showString("Using Standard Count Format...");
			showString("Reading header data...");
			// Derive a tree from a frequency array
			freqMap = new TreeMap<>();
			for (int i = 0; i < ALPH_SIZE; i++) {
				int frequency = reader.readBits(BITS_PER_INT);
				if (frequency == -1) {
					myViewer.showError("STANDARD COUNT FORMAT error");
					return 0;
				}
				if (frequency != 0) {
					freqMap.put(i, frequency);
				}
			}
			freqMap.put(256, 1);
			// Make a priority queue from the freq map
			PriorityQueue<TreeNode> pQueue = createQueue(freqMap);
			// Make a huffmanTree from a priority queue
			hfTree = new HuffmanTree(pQueue);

		} else {
			showString("Using Standard Tree Format...");
			showString("Reading header data...");
			// Derive a tree from bits representing the huffman tree
			int numBitsInTree = reader.readBits(BITS_PER_INT);
			if (numBitsInTree == -1) {
				myViewer.showError("STANDARD TREE FORMAT error");
				return 0;
			}
			hfTree = new HuffmanTree(reader);
		}
		showString("Huffman Tree created...");
		showString("Reading compressed data...");
		TreeNode current = hfTree.root;
		BitOutputStream writer = new BitOutputStream(out);
		boolean reading = true;
		while (reading) {
			// Read in a bit at a time
			int currentBit = reader.readBits(1);
			if (currentBit == -1) {
				myViewer.showError("Data ended before reaching PEOF");
				return 0;
			} else {
				// Bit tells where to travel in the huffman tree
				if (currentBit == 0)
					current = current.getLeft();
				else if (currentBit == 1)
					current = current.getRight();
				if (current.isLeaf()) {
					// If ever reached a leaf, check for PEOF
					if (current.getValue() == PSEUDO_EOF) {
						reading = false;
					} else {
						// else write out the word
						writer.writeBits(BITS_PER_WORD, current.getValue());
						bitsWritten += BITS_PER_WORD;
						current = hfTree.root; // reset to tree root
					}
				}
			}
		}
		showString("Finished uncompressing data");
		// close reader and writer
		reader.close();
		writer.close();
		showString("Wrote " + bitsWritten + " bits");
		return bitsWritten;
	}

	public void setViewer(IHuffViewer viewer) {
		myViewer = viewer;
	}

	private void showString(String s) {
		if (myViewer != null)
			myViewer.update(s);
	}
}
