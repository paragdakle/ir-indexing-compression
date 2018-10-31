package main.java.compression;

import main.java.model.PostingFileItem;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CompressionManager {

    public final byte GAMMA_ENCODING = 1;
    public final byte DELTA_ENCODING = 2;
    private String termString;
    private int[] termPointers;
    private LinkedList<LinkedList<PostingFileItem>> compressedPostingsList;

    public LinkedList<PostingFileItem> computeGapPostingList(LinkedList<PostingFileItem> postingList, byte encodingType) {
        int firstElement = -1;
        int tempValueHolder;
        for(int counter = 0; counter < postingList.size(); counter++) {
            PostingFileItem postingFileItem = postingList.get(counter);
            if(firstElement == -1) {
                firstElement = postingFileItem.getDocId();
            }
            else {
                tempValueHolder = postingFileItem.getDocId();
                postingFileItem.computeGap(firstElement);
                firstElement = tempValueHolder;
            }
            switch (encodingType) {
                case GAMMA_ENCODING:
                    postingFileItem.setCode(getGammaCode(postingFileItem.getDocId()));
                    break;

                case DELTA_ENCODING:
                    postingFileItem.setCode(getDeltaCode(postingFileItem.getDocId()));
                    break;
            }
        }
        return postingList;
    }

    public void compressWithBlockCompression(Map<String, LinkedList<PostingFileItem>> index, byte encodingType, short blockSize) {
        StringBuilder termStringBuilder = new StringBuilder();
        int counter = 0;
        termPointers = new int[(index.size() / blockSize) + 1];
        compressedPostingsList = new LinkedList<>();
        termPointers[0] = 0;
        for (String term: index.keySet()) {
            termStringBuilder.append(term.length()).append(term);
            counter++;
            if(counter % blockSize == 0) {
                termPointers[counter / blockSize] = termStringBuilder.length();
            }
            compressedPostingsList.add(computeGapPostingList(index.get(term), encodingType));
        }
        this.termString = termStringBuilder.toString();
    }

    public void compressWithBlkCmprsnAndFrontEncoding(Map<String, LinkedList<PostingFileItem>> index, byte encodingType, short blockSize) {
        this.compressWithBlockCompression(index, encodingType, blockSize);
        performFrontEncoding(blockSize);
    }

    private void performFrontEncoding(short blockSize) {
        StringBuilder termStringBuilder = new StringBuilder();
        int[] newTermPointers = new int[termPointers.length];
        newTermPointers[0] = 0;
        for(int counter = 0; counter < termPointers.length; counter++) {
            Trie root = new Trie();
            String blockTermString;
            if(counter == termPointers.length - 1) {
                blockTermString = termString.substring(termPointers[counter]);
            }
            else {
                blockTermString = termString.substring(termPointers[counter], termPointers[counter + 1]);
            }
            char[] termCharArray = blockTermString.toCharArray();
            int charCounter = 0;
            int sPointer = -1;
            boolean isTerm = false;
            int[] indexPointers = new int[2 * blockSize];
            for(short indexCounter = 0; indexCounter < indexPointers.length; indexCounter++) {
                indexPointers[indexCounter] = -1;
            }
            short pointerCounter = 0;
            for(char item: termCharArray) {
                if(!Character.isDigit(item) && !isTerm) {
                    sPointer = charCounter;
                    isTerm = true;
                }
                else if(Character.isDigit(item) && isTerm) {
                    indexPointers[pointerCounter++] = sPointer;
                    indexPointers[pointerCounter++] = charCounter - 1;
                    addNodeToTrie(root, termCharArray, sPointer, charCounter - 1);
                    isTerm = false;
                }
                if(charCounter == termCharArray.length - 1) {
                    indexPointers[pointerCounter++] = sPointer;
                    indexPointers[pointerCounter++] = charCounter;
                    addNodeToTrie(root, termCharArray, sPointer, charCounter);
                }
                charCounter++;
            }

            termStringBuilder.append(encodeBlockTermString(root, termCharArray, indexPointers));
            if(counter < termPointers.length - 1) {
                newTermPointers[counter + 1] = termStringBuilder.length();
            }
        }
        termString = termStringBuilder.toString();
        termPointers = newTermPointers;
    }

    private void addNodeToTrie(Trie node, char[] term, int startIndex, int endIndex) {
        if(startIndex == endIndex) {
            Trie trie = new Trie(term[startIndex], true);
            node.addChild(trie);
        }
        else {
            Trie trie = new Trie(term[startIndex]);
            addNodeToTrie(node.addChild(trie), term, startIndex + 1, endIndex);
        }
    }

    private String encodeBlockTermString(Trie root, char[] blockTermCharArray, int[] indexPointers) {
        List<String> prefixes = getCommonPrefixes(root);
        if(prefixes == null || prefixes.size() == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        short prefixCounter = 0;
        char[] prefixCharArray = prefixes.get(prefixCounter).toCharArray();
        boolean isFirst = true;
        for(short counter = 0; counter < indexPointers.length; counter += 2) {
            if(indexPointers[counter] == -1) {
                break;
            }
            if(prefixCharArray[0] != blockTermCharArray[indexPointers[counter]]) {
                prefixCounter++;
                prefixCharArray = prefixes.get(prefixCounter).toCharArray();
                isFirst = true;
            }
            builder.append(frontEncode(prefixCharArray, blockTermCharArray, indexPointers[counter], indexPointers[counter + 1], isFirst));
            isFirst = false;
        }
        return builder.toString();
    }

    private String frontEncode(char[] prefixArray, char[] blockTermArray, int start, int end, boolean isFirst) {
        StringBuilder builder = new StringBuilder();
        if(prefixArray[0] == blockTermArray[start]) {
            if (prefixArray.length > 1) {
                if(!isFirst) {
                    builder.append(end - start + 1 - prefixArray.length);
                    builder.append('$');
                }
                else {
                    builder.append(prefixArray.length);
                    for (char aPrefixArray : prefixArray) {
                        builder.append(aPrefixArray);
                    }
                    builder.append('*');
                }
                start = start + prefixArray.length;
            } else {
                builder.append(end - start + 1);
            }
            for(int counter = start; counter <= end; counter++) {
                builder.append(blockTermArray[counter]);
            }
        }
        return builder.toString();
    }

    private List<String> getCommonPrefixes(Trie root) {
        if(root.getSize() == 0) {
            return null;
        }
        List<String> prefixList = new ArrayList<>(1);
        for(Trie child: root.getChildren()) {
            if(child != null) {
                String prefix = encodeSubtree(child);
                if(prefix.isEmpty()) {
                    prefix = child.getValue() + "";
                }
                prefixList.add(prefix);
            }
        }
        return prefixList;
    }

    private String encodeSubtree(Trie node) {
        if(node.getSize() == 1) {
            if(node.isWord()) {
                return "" + node.getValue();
            }
            for (Trie child: node.getChildren()) {
                if(child != null) {
                    String prefix = encodeSubtree(child);
                    if(prefix.isEmpty()) {
                        return "";
                    }
                    return node.getValue() + prefix;
                }
            }
        }
        if(node.getSize() == 0) {
            return "";
        }
        return "" + node.getValue();
    }

    private boolean[] getDeltaCode(int number) {
        if(number == 1) {
            return new boolean[] {false};
        }
        boolean[] binaryRepresentation = getBinaryRepresentation(number);
        boolean[] gammaCode = getGammaCode(binaryRepresentation.length);
        boolean[] deltaCode = new boolean[gammaCode.length + binaryRepresentation.length - 1];
        System.arraycopy(gammaCode, 0, deltaCode, 0, gammaCode.length);
        System.arraycopy(binaryRepresentation, 1, deltaCode, gammaCode.length, binaryRepresentation.length - 1);
        return deltaCode;
    }

    private boolean[] getGammaCode(int number) {
        if(number == 1) {
            return new boolean[] {false};
        }
        boolean[] binaryRepresentation = getBinaryRepresentation(number);
        boolean[] gammaCode = new boolean[binaryRepresentation.length + binaryRepresentation.length - 1];
        boolean[] unaryCode = getUnaryCode(binaryRepresentation.length - 1);
        System.arraycopy(unaryCode, 0, gammaCode, 0, unaryCode.length);
        System.arraycopy(binaryRepresentation, 1, gammaCode, unaryCode.length, binaryRepresentation.length - 1);
        return gammaCode;
    }

    public boolean[] getBinaryRepresentation(int number) {
        int base = ((Double)(Math.floor(Math.log(number) / Math.log(2)))).intValue();
        boolean[] binaryRepresentation = new boolean[base + 1];
        int counter = base;
        while (number > 0) {
            binaryRepresentation[counter] = (number % 2) == 1;
            number = number / 2;
            counter--;
        }
        return binaryRepresentation;
    }

    public boolean[] getUnaryCode(int number) {
        boolean[] unaryCode = new boolean[number + 1];
        for (int counter = 0; counter < unaryCode.length - 1; counter++) {
            unaryCode[counter] = true;
        }
        unaryCode[unaryCode.length - 1] = false;
        return unaryCode;
    }

    public void printBooleanArray(boolean[] con) {
        for (boolean element: con) {
            if(element) {
                System.out.print("1");
            }
            else {
                System.out.print("0");
            }
        }
    }

    public String getTermString() {
        return this.termString;
    }

    public int[] getTermPointers() {
        return this.termPointers;
    }

    public LinkedList<LinkedList<PostingFileItem>> getCompressedPostingsList() {
        return this.compressedPostingsList;
    }
}
