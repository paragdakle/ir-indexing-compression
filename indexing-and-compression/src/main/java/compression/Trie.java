package main.java.compression;

public class Trie {

    private Trie[] children = null;

    private boolean isWord;

    private char value;

    private final byte ALPHABET_SIZE = 26;

    private byte size = 0;

    public Trie() {
        this.children = new Trie[ALPHABET_SIZE];
        initializeChildren();
        this.isWord = false;
    }

    public Trie(char value) {
        this.children = new Trie[ALPHABET_SIZE];
        initializeChildren();
        this.isWord = false;
        this.value = value;
    }

    public Trie(char value, boolean isWord) {
        this.value = value;
        this.isWord = isWord;
    }

    private void initializeChildren() {
        for(int counter = 0; counter < ALPHABET_SIZE; counter++) {
            this.children[counter] = null;
        }
    }

    public Trie addChild(Trie child) {
        int intVal = child.getValue() - 97;
        if(this.children == null && isWord) {
            this.children = new Trie[ALPHABET_SIZE];
            initializeChildren();
        }
        if(this.children[intVal] == null) {
            this.children[intVal] = child;
            this.size++;
        }
        return this.children[intVal];
    }

    public char getValue() {
        return this.value;
    }

    public boolean isWord() {
        return this.isWord;
    }

    public Trie[] getChildren() {
        return this.children;
    }

    public byte getSize() {
        return this.size;
    }
}
