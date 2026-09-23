package interview_prep_mastery._07_trie;

import java.util.*;

/**
 * ============================================================================
 * MODULE 07: TRIE - PREFIX TREE CHALLENGES
 * Topics:
 * - Trie Structure, Insertion, Search, Deletion
 * - Challenge 1: Total Number of Words in a Trie
 * - Challenge 2: Find All Words in a Trie
 * - Challenge 3: Sort Elements of an Array using a Trie
 * - Challenge 4: Word Formation from Given Dictionary
 * ============================================================================
 */
public class TrieChallenges {

    // TrieNode Definition
    public static class TrieNode {
        public TrieNode[] children = new TrieNode[26];
        public boolean isEndOfWord = false;

        public void markAsLeaf() {
            this.isEndOfWord = true;
        }

        public void unmarkAsLeaf() {
            this.isEndOfWord = false;
        }

        public boolean hasNoChildren() {
            for (TrieNode child : children) {
                if (child != null) return false;
            }
            return true;
        }
    }

    public static class Trie {
        public TrieNode root;

        public Trie() {
            this.root = new TrieNode();
        }

        // --------------------------------------------------------------------
        // Core: Insertion in a Trie
        // --------------------------------------------------------------------
        /**
         * Complexity:
         * - Time: O(L) where L is string length
         * - Space: O(L) in worst case for new nodes
         */
        public void insert(String key) {
            if (key == null) return;
            key = key.toLowerCase();
            TrieNode curr = root;

            for (int i = 0; i < key.length(); i++) {
                int index = key.charAt(i) - 'a';
                if (curr.children[index] == null) {
                    curr.children[index] = new TrieNode();
                }
                curr = curr.children[index];
            }
            curr.markAsLeaf();
        }

        // --------------------------------------------------------------------
        // Core: Search in a Trie
        // --------------------------------------------------------------------
        public boolean search(String key) {
            if (key == null) return false;
            key = key.toLowerCase();
            TrieNode curr = root;

            for (int i = 0; i < key.length(); i++) {
                int index = key.charAt(i) - 'a';
                if (curr.children[index] == null) return false;
                curr = curr.children[index];
            }
            return curr != null && curr.isEndOfWord;
        }

        public boolean startsWith(String prefix) {
            if (prefix == null) return false;
            prefix = prefix.toLowerCase();
            TrieNode curr = root;

            for (int i = 0; i < prefix.length(); i++) {
                int index = prefix.charAt(i) - 'a';
                if (curr.children[index] == null) return false;
                curr = curr.children[index];
            }
            return curr != null;
        }

        // --------------------------------------------------------------------
        // Core: Deletion in a Trie
        // --------------------------------------------------------------------
        public boolean delete(String key) {
            if (key == null || root == null) return false;
            return deleteHelper(key.toLowerCase(), root, 0);
        }

        private boolean deleteHelper(String key, TrieNode curr, int length) {
            if (curr == null) return false;

            if (length == key.length()) {
                if (!curr.isEndOfWord) return false;
                curr.unmarkAsLeaf();
                return curr.hasNoChildren();
            }

            int index = key.charAt(length) - 'a';
            TrieNode child = curr.children[index];
            if (child == null) return false;

            boolean shouldDeleteChild = deleteHelper(key, child, length + 1);

            if (shouldDeleteChild) {
                curr.children[index] = null;
                return !curr.isEndOfWord && curr.hasNoChildren();
            }
            return false;
        }

        // --------------------------------------------------------------------
        // Challenge 1: Total Number of Words in a Trie
        // --------------------------------------------------------------------
        public int totalWords() {
            return countWordsHelper(root);
        }

        private int countWordsHelper(TrieNode node) {
            if (node == null) return 0;
            int count = node.isEndOfWord ? 1 : 0;
            for (TrieNode child : node.children) {
                if (child != null) {
                    count += countWordsHelper(child);
                }
            }
            return count;
        }

        // --------------------------------------------------------------------
        // Challenge 2: Find All Words in a Trie
        // --------------------------------------------------------------------
        public List<String> findAllWords() {
            List<String> result = new ArrayList<>();
            findAllWordsHelper(root, new StringBuilder(), result);
            return result;
        }

        private void findAllWordsHelper(TrieNode node, StringBuilder currentWord, List<String> result) {
            if (node == null) return;
            if (node.isEndOfWord) {
                result.add(currentWord.toString());
            }
            for (int i = 0; i < 26; i++) {
                if (node.children[i] != null) {
                    currentWord.append((char) ('a' + i));
                    findAllWordsHelper(node.children[i], currentWord, result);
                    currentWord.deleteCharAt(currentWord.length() - 1);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Challenge 3: Sort the Elements of an Array Using a Trie
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(N * L) where N is number of words, L is max word length
     * - Space: O(N * L)
     */
    public static List<String> sortArrayUsingTrie(String[] arr) {
        if (arr == null || arr.length == 0) return new ArrayList<>();
        Trie trie = new Trie();
        for (String word : arr) {
            trie.insert(word);
        }
        return trie.findAllWords();
    }

    // ------------------------------------------------------------------------
    // Challenge 4: Word Formation from a Given Dictionary using Trie
    // ------------------------------------------------------------------------
    /**
     * Checks if target word can be formed by concatenating two dictionary words.
     *
     * Complexity:
     * - Time: O(L) where L is length of word
     * - Space: O(DictSize * L)
     */
    public static boolean isWordFormedFromTwo(String[] dict, String target) {
        if (dict == null || target == null || target.length() < 2) return false;
        Trie trie = new Trie();
        for (String word : dict) {
            trie.insert(word);
        }

        for (int i = 1; i < target.length(); i++) {
            String prefix = target.substring(0, i);
            String suffix = target.substring(i);
            if (trie.search(prefix) && trie.search(suffix)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // Test Suite for Trie Challenges
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING TRIE CHALLENGES TEST SUITE ");
        System.out.println("=================================================");

        Trie trie = new Trie();
        trie.insert("the");
        trie.insert("a");
        trie.insert("there");
        trie.insert("answer");
        trie.insert("any");
        trie.insert("by");
        trie.insert("bye");
        trie.insert("their");

        // Search & StartsWith
        assert trie.search("the");
        assert trie.search("these") == false;
        assert trie.startsWith("th");

        // Challenge 1: Total Words
        assert trie.totalWords() == 8;

        // Challenge 2: Find All Words (Alphabetically sorted)
        List<String> allWords = trie.findAllWords();
        assert allWords.size() == 8;
        assert allWords.get(0).equals("a");

        // Deletion
        trie.delete("the");
        assert !trie.search("the");
        assert trie.search("there"); // "there" preserved!

        // Challenge 3: Sort Array using Trie
        String[] unsorted = {"zebra", "apple", "banana", "cat", "dog"};
        List<String> sorted = sortArrayUsingTrie(unsorted);
        assert sorted.equals(Arrays.asList("apple", "banana", "cat", "dog", "zebra"));

        // Challenge 4: Word Formation from two dictionary words
        String[] dictionary = {"news", "paper", "feed", "back", "sun", "flower"};
        assert isWordFormedFromTwo(dictionary, "newspaper");
        assert isWordFormedFromTwo(dictionary, "feedback");
        assert !isWordFormedFromTwo(dictionary, "sunshine");

        System.out.println(" TRIE CHALLENGES ALL PASSED!");
        System.out.println("=================================================");
    }
}
