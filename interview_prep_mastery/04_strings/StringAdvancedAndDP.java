package interview_prep_mastery._04_strings;

import java.util.*;

/**
 * ============================================================================
 * MODULE 04: STRINGS - PART 2: ADVANCED DP, SLIDING WINDOW & COMBINATORICS
 * ============================================================================
 */
public class StringAdvancedAndDP {

    // ------------------------------------------------------------------------
    // 1. Word Break Problem (1D DP)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n^2 * L) where L is max word length in dictionary
     * - Space: O(n) for dp array
     */
    public static boolean wordBreak(String s, List<String> wordDict) {
        if (s == null || s.isEmpty()) return true;
        Set<String> dict = new HashSet<>(wordDict);
        boolean[] dp = new boolean[s.length() + 1];
        dp[0] = true;

        for (int i = 1; i <= s.length(); i++) {
            for (int j = 0; j < i; j++) {
                if (dp[j] && dict.contains(s.substring(j, i))) {
                    dp[i] = true;
                    break;
                }
            }
        }
        return dp[s.length()];
    }

    // ------------------------------------------------------------------------
    // 2. XML to Tree Parser
    // ------------------------------------------------------------------------
    public static class XmlNode {
        public String tag;
        public String textContent;
        public List<XmlNode> children = new ArrayList<>();

        public XmlNode(String tag) {
            this.tag = tag;
        }
    }

    public static XmlNode parseXmlToTree(String xml) {
        if (xml == null || xml.trim().isEmpty()) return null;
        Stack<XmlNode> stack = new Stack<>();
        XmlNode root = null;
        int i = 0;
        int n = xml.length();

        while (i < n) {
            if (xml.charAt(i) == '<') {
                int closeTagIdx = xml.indexOf('>', i);
                if (closeTagIdx == -1) break;
                String tagContent = xml.substring(i + 1, closeTagIdx).trim();

                if (tagContent.startsWith("/")) {
                    // Closing tag: pop from stack
                    if (!stack.isEmpty()) {
                        XmlNode closed = stack.pop();
                        if (stack.isEmpty()) {
                            root = closed;
                        }
                    }
                } else {
                    // Opening tag
                    XmlNode newNode = new XmlNode(tagContent);
                    if (!stack.isEmpty()) {
                        stack.peek().children.add(newNode);
                    }
                    stack.push(newNode);
                }
                i = closeTagIdx + 1;
            } else {
                int nextTagIdx = xml.indexOf('<', i);
                if (nextTagIdx == -1) nextTagIdx = n;
                String text = xml.substring(i, nextTagIdx).trim();
                if (!text.isEmpty() && !stack.isEmpty()) {
                    stack.peek().textContent = text;
                }
                i = nextTagIdx;
            }
        }
        return root;
    }

    // ------------------------------------------------------------------------
    // 3. Regular Expression Matching ('.' and '*')
    // ------------------------------------------------------------------------
    /**
     * DP State: dp[i][j] = matches s[0...i-1] with p[0...j-1]
     *
     * Complexity:
     * - Time: O(m * n)
     * - Space: O(m * n)
     */
    public static boolean isMatch(String s, String p) {
        if (s == null || p == null) return false;
        int m = s.length(), n = p.length();
        boolean[][] dp = new boolean[m + 1][n + 1];
        dp[0][0] = true;

        // Deals with patterns like a* or a*b* matching empty string
        for (int j = 1; j <= n; j++) {
            if (p.charAt(j - 1) == '*') {
                dp[0][j] = dp[0][j - 2];
            }
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char sc = s.charAt(i - 1);
                char pc = p.charAt(j - 1);

                if (pc == '.' || pc == sc) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else if (pc == '*') {
                    // 0 occurrences of preceding char
                    dp[i][j] = dp[i][j - 2];
                    // 1 or more occurrences if preceding char matches sc or '.'
                    char prevPatternChar = p.charAt(j - 2);
                    if (prevPatternChar == '.' || prevPatternChar == sc) {
                        dp[i][j] = dp[i][j] || dp[i - 1][j];
                    }
                }
            }
        }
        return dp[m][n];
    }

    // ------------------------------------------------------------------------
    // 4. Longest Common Substring & Subsequence
    // ------------------------------------------------------------------------
    public static int longestCommonSubstring(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        int maxLength = 0;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = 1 + dp[i - 1][j - 1];
                    maxLength = Math.max(maxLength, dp[i][j]);
                } else {
                    dp[i][j] = 0;
                }
            }
        }
        return maxLength;
    }

    public static int longestCommonSubsequence(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        int m = s1.length(), n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = 1 + dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[m][n];
    }

    public static int[] minDeletionsAndInsertions(String s1, String s2) {
        int lcs = longestCommonSubsequence(s1, s2);
        int deletions = s1.length() - lcs;
        int insertions = s2.length() - lcs;
        return new int[]{deletions, insertions};
    }

    // ------------------------------------------------------------------------
    // 5. Fruits into Baskets (Longest Substring with at most 2 Distinct Elements)
    // ------------------------------------------------------------------------
    public static int totalFruit(char[] tree) {
        if (tree == null || tree.length == 0) return 0;
        Map<Character, Integer> freqMap = new HashMap<>();
        int maxFruit = 0;
        int windowStart = 0;

        for (int windowEnd = 0; windowEnd < tree.length; windowEnd++) {
            freqMap.put(tree[windowEnd], freqMap.getOrDefault(tree[windowEnd], 0) + 1);
            while (freqMap.size() > 2) {
                char leftChar = tree[windowStart];
                freqMap.put(leftChar, freqMap.get(leftChar) - 1);
                if (freqMap.get(leftChar) == 0) {
                    freqMap.remove(leftChar);
                }
                windowStart++;
            }
            maxFruit = Math.max(maxFruit, windowEnd - windowStart + 1);
        }
        return maxFruit;
    }

    // ------------------------------------------------------------------------
    // 6. Longest Substring with Maximum K Distinct Characters
    // ------------------------------------------------------------------------
    public static int findLengthKDistinct(String str, int k) {
        if (str == null || str.isEmpty() || k <= 0) return 0;
        Map<Character, Integer> charFrequencyMap = new HashMap<>();
        int maxLength = 0;
        int windowStart = 0;

        for (int windowEnd = 0; windowEnd < str.length(); windowEnd++) {
            char rightChar = str.charAt(windowEnd);
            charFrequencyMap.put(rightChar, charFrequencyMap.getOrDefault(rightChar, 0) + 1);

            while (charFrequencyMap.size() > k) {
                char leftChar = str.charAt(windowStart);
                charFrequencyMap.put(leftChar, charFrequencyMap.get(leftChar) - 1);
                if (charFrequencyMap.get(leftChar) == 0) {
                    charFrequencyMap.remove(leftChar);
                }
                windowStart++;
            }
            maxLength = Math.max(maxLength, windowEnd - windowStart + 1);
        }
        return maxLength;
    }

    // ------------------------------------------------------------------------
    // 7. String Permutations by Changing Case
    // ------------------------------------------------------------------------
    public static List<String> letterCasePermutation(String s) {
        List<String> permutations = new ArrayList<>();
        if (s == null) return permutations;
        permutations.add(s);

        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) {
                int n = permutations.size();
                for (int j = 0; j < n; j++) {
                    char[] chs = permutations.get(j).toCharArray();
                    if (Character.isUpperCase(chs[i])) {
                        chs[i] = Character.toLowerCase(chs[i]);
                    } else {
                        chs[i] = Character.toUpperCase(chs[i]);
                    }
                    permutations.add(String.valueOf(chs));
                }
            }
        }
        return permutations;
    }

    // ------------------------------------------------------------------------
    // 8. Unique Generalized Abbreviations
    // ------------------------------------------------------------------------
    public static List<String> generateAbbreviations(String word) {
        List<String> result = new ArrayList<>();
        abbreviationHelper(word, 0, "", 0, result);
        return result;
    }

    private static void abbreviationHelper(String word, int pos, String current, int count, List<String> result) {
        if (pos == word.length()) {
            if (count > 0) current += count;
            result.add(current);
            return;
        }
        // Option 1: Abbreviate current character (increment count)
        abbreviationHelper(word, pos + 1, current, count + 1, result);

        // Option 2: Keep current character
        abbreviationHelper(word, pos + 1, current + (count > 0 ? count : "") + word.charAt(pos), 0, result);
    }

    // ------------------------------------------------------------------------
    // 9. Longest Substring with Distinct Characters (All Unique)
    // ------------------------------------------------------------------------
    public static int lengthOfLongestSubstringDistinct(String s) {
        if (s == null || s.isEmpty()) return 0;
        Map<Character, Integer> lastSeen = new HashMap<>();
        int maxLen = 0;
        int windowStart = 0;

        for (int windowEnd = 0; windowEnd < s.length(); windowEnd++) {
            char rightChar = s.charAt(windowEnd);
            if (lastSeen.containsKey(rightChar)) {
                windowStart = Math.max(windowStart, lastSeen.get(rightChar) + 1);
            }
            lastSeen.put(rightChar, windowEnd);
            maxLen = Math.max(maxLen, windowEnd - windowStart + 1);
        }
        return maxLen;
    }

    // ------------------------------------------------------------------------
    // 10. Longest Substring with Same Letters after Replacement
    // ------------------------------------------------------------------------
    public static int characterReplacement(String s, int k) {
        if (s == null || s.isEmpty()) return 0;
        int[] count = new int[26];
        int maxCount = 0;
        int maxLen = 0;
        int windowStart = 0;

        for (int windowEnd = 0; windowEnd < s.length(); windowEnd++) {
            maxCount = Math.max(maxCount, ++count[s.charAt(windowEnd) - 'A']);
            // If letters to replace > k, shrink window
            while ((windowEnd - windowStart + 1) - maxCount > k) {
                count[s.charAt(windowStart) - 'A']--;
                windowStart++;
            }
            maxLen = Math.max(maxLen, windowEnd - windowStart + 1);
        }
        return maxLen;
    }

    // ------------------------------------------------------------------------
    // 11. Boggle (Word Search in 2D Board)
    // ------------------------------------------------------------------------
    public static boolean existInBoggle(char[][] board, String word) {
        if (board == null || word == null || word.isEmpty()) return false;
        int rows = board.length, cols = board[0].length;
        boolean[][] visited = new boolean[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (boggleDfs(board, word, r, c, 0, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean boggleDfs(char[][] board, String word, int r, int c, int idx, boolean[][] visited) {
        if (idx == word.length()) return true;
        if (r < 0 || r >= board.length || c < 0 || c >= board[0].length || visited[r][c] || board[r][c] != word.charAt(idx)) {
            return false;
        }

        visited[r][c] = true;
        boolean found = boggleDfs(board, word, r + 1, c, idx + 1, visited)
                || boggleDfs(board, word, r - 1, c, idx + 1, visited)
                || boggleDfs(board, word, r, c + 1, idx + 1, visited)
                || boggleDfs(board, word, r, c - 1, idx + 1, visited);
        visited[r][c] = false; // backtrack
        return found;
    }

    // ------------------------------------------------------------------------
    // 12. Generate All Combinations of Balanced Parentheses
    // ------------------------------------------------------------------------
    public static List<String> generateParenthesis(int n) {
        List<String> result = new ArrayList<>();
        backtrackParens(result, new StringBuilder(), 0, 0, n);
        return result;
    }

    private static void backtrackParens(List<String> result, StringBuilder current, int open, int close, int max) {
        if (current.length() == max * 2) {
            result.add(current.toString());
            return;
        }
        if (open < max) {
            current.append('(');
            backtrackParens(result, current, open + 1, close, max);
            current.deleteCharAt(current.length() - 1);
        }
        if (close < open) {
            current.append(')');
            backtrackParens(result, current, open, close + 1, max);
            current.deleteCharAt(current.length() - 1);
        }
    }

    // ------------------------------------------------------------------------
    // Test Suite for Part 2
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING STRING PART 2 TEST SUITE ");
        System.out.println("=================================================");

        // 1. Word Break
        assert wordBreak("leetcode", Arrays.asList("leet", "code"));
        assert !wordBreak("catsandog", Arrays.asList("cats", "dog", "sand", "and", "cat"));

        // 2. XML to Tree
        XmlNode xmlRoot = parseXmlToTree("<note><to>Tove</to><from>Jani</from></note>");
        assert xmlRoot != null && xmlRoot.tag.equals("note") && xmlRoot.children.size() == 2;

        // 3. Regex Matching
        assert isMatch("aa", "a*");
        assert isMatch("ab", ".*");
        assert !isMatch("mississippi", "mis*is*p*.");

        // 4. LCS & Transformations
        assert longestCommonSubstring("passport", "ppsspt") == 3; // "ssp"
        assert longestCommonSubsequence("abcde", "ace") == 3;
        int[] delIns = minDeletionsAndInsertions("heap", "pea");
        assert delIns[0] == 2 && delIns[1] == 1; // Delete 'h','p', Insert 'p'

        // 5. Fruits into Baskets
        assert totalFruit(new char[]{'A', 'B', 'C', 'A', 'C'}) == 3;

        // 6. Max K Distinct
        assert findLengthKDistinct("araaci", 2) == 4;

        // 7. Case Permutations
        assert letterCasePermutation("a1b2").size() == 4;

        // 8. Generalized Abbreviations
        assert generateAbbreviations("word").contains("1o1d");

        // 9. Longest Distinct Chars
        assert lengthOfLongestSubstringDistinct("abcabcbb") == 3;

        // 10. Same Letters Replacement
        assert characterReplacement("AABABBA", 1) == 4;

        // 11. Boggle Word Search
        char[][] boggle = {
                {'A', 'B', 'C', 'E'},
                {'S', 'F', 'C', 'S'},
                {'A', 'D', 'E', 'E'}
        };
        assert existInBoggle(boggle, "ABCCED");
        assert !existInBoggle(boggle, "ABCB");

        // 12. Balanced Parentheses
        assert generateParenthesis(3).size() == 5;

        System.out.println(" STRING PART 2 ALL TESTS PASSED!");
        System.out.println("=================================================");
    }
}
