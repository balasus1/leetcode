package interview_prep_mastery._04_strings;

import java.util.*;

/**
 * ============================================================================
 * MODULE 04: STRINGS - PART 1: CORE OPERATIONS & PALINDROMES
 * Topics: Common Methods, Reverse Words, Remove Dups, Remove Spaces,
 * Palindrome Substrings, Longest Palindromic Substring/Subsequence,
 * Count Palindromes, Min Deletions for Palindrome, Palindromic Partitioning.
 * ============================================================================
 */
public class StringCoreAndPalindromes {

    // ------------------------------------------------------------------------
    // 1. Reverse Words in a Sentence
    // ------------------------------------------------------------------------
    /**
     * Probing Questions / Assumptions:
     * - Are multiple leading, trailing, or middle spaces possible? (Trim & single space)
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(n)
     */
    public static String reverseWords(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            sb.append(words[i]);
            if (i > 0) sb.append(" ");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // 2. Remove Duplicates from a String
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n)
     * - Space: O(k) where k <= 256 for ASCII character set
     */
    public static String removeDuplicates(String s) {
        if (s == null || s.length() <= 1) return s;
        boolean[] seen = new boolean[256];
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (!seen[c]) {
                seen[c] = true;
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // 3. Remove White Spaces from a String (In-place on char[])
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n)
     * - Space: O(1) auxiliary (operating directly on char array)
     */
    public static String removeWhiteSpaces(String s) {
        if (s == null) return null;
        char[] chars = s.toCharArray();
        int writeIdx = 0;
        for (int readIdx = 0; readIdx < chars.length; readIdx++) {
            if (!Character.isWhitespace(chars[readIdx])) {
                chars[writeIdx++] = chars[readIdx];
            }
        }
        return new String(chars, 0, writeIdx);
    }

    // ------------------------------------------------------------------------
    // 4. Find All Palindrome Substrings
    // ------------------------------------------------------------------------
    /**
     * Expand around center approach:
     * Time: O(n^2), Space: O(1) auxiliary
     */
    public static List<String> findAllPalindromeSubstrings(String s) {
        List<String> result = new ArrayList<>();
        if (s == null || s.isEmpty()) return result;
        for (int i = 0; i < s.length(); i++) {
            // Odd length palindromes
            expandAndCollect(s, i, i, result);
            // Even length palindromes
            expandAndCollect(s, i, i + 1, result);
        }
        return result;
    }

    private static void expandAndCollect(String s, int left, int right, List<String> result) {
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            result.add(s.substring(left, right + 1));
            left--;
            right++;
        }
    }

    // ------------------------------------------------------------------------
    // 5. Longest Palindromic Substring
    // ------------------------------------------------------------------------
    /**
     * Time: O(n^2), Space: O(1) auxiliary
     */
    public static String longestPalindromicSubstring(String s) {
        if (s == null || s.length() <= 1) return s;
        int start = 0, end = 0;
        for (int i = 0; i < s.length(); i++) {
            int len1 = expandAroundCenter(s, i, i);
            int len2 = expandAroundCenter(s, i, i + 1);
            int len = Math.max(len1, len2);
            if (len > end - start) {
                start = i - (len - 1) / 2;
                end = i + len / 2;
            }
        }
        return s.substring(start, end + 1);
    }

    private static int expandAroundCenter(String s, int left, int right) {
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            left--;
            right++;
        }
        return right - left - 1;
    }

    // ------------------------------------------------------------------------
    // 6. Longest Palindromic Subsequence (DP)
    // ------------------------------------------------------------------------
    /**
     * DP State: dp[i][j] = length of LPS in substring s[i...j]
     * If s[i] == s[j] => dp[i][j] = dp[i+1][j-1] + 2
     * Else => dp[i][j] = max(dp[i+1][j], dp[i][j-1])
     *
     * Complexity:
     * - Time: O(n^2)
     * - Space: O(n^2)
     */
    public static int longestPalindromicSubsequence(String s) {
        if (s == null || s.isEmpty()) return 0;
        int n = s.length();
        int[][] dp = new int[n][n];

        for (int i = 0; i < n; i++) dp[i][i] = 1;

        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                if (s.charAt(i) == s.charAt(j)) {
                    dp[i][j] = (len == 2) ? 2 : dp[i + 1][j - 1] + 2;
                } else {
                    dp[i][j] = Math.max(dp[i + 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[0][n - 1];
    }

    // ------------------------------------------------------------------------
    // 7. Count of Palindromic Substrings
    // ------------------------------------------------------------------------
    /**
     * Time: O(n^2), Space: O(1)
     */
    public static int countPalindromicSubstrings(String s) {
        if (s == null || s.isEmpty()) return 0;
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            count += countAroundCenter(s, i, i);     // Odd
            count += countAroundCenter(s, i, i + 1); // Even
        }
        return count;
    }

    private static int countAroundCenter(String s, int left, int right) {
        int count = 0;
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            count++;
            left--;
            right++;
        }
        return count;
    }

    // ------------------------------------------------------------------------
    // 8. Minimum Deletions in a String to Make it a Palindrome
    // ------------------------------------------------------------------------
    /**
     * Min Deletions = String Length - Longest Palindromic Subsequence Length
     *
     * Time: O(n^2), Space: O(n^2)
     */
    public static int minDeletionsToMakePalindrome(String s) {
        if (s == null || s.length() <= 1) return 0;
        return s.length() - longestPalindromicSubsequence(s);
    }

    // ------------------------------------------------------------------------
    // 9. Palindromic Partitioning (Minimum Cuts)
    // ------------------------------------------------------------------------
    /**
     * DP: cuts[i] = min cuts for prefix s[0...i].
     *
     * Time: O(n^2), Space: O(n^2)
     */
    public static int minCutPalindromicPartitioning(String s) {
        if (s == null || s.length() <= 1) return 0;
        int n = s.length();
        boolean[][] isPal = new boolean[n][n];
        int[] cuts = new int[n];

        for (int i = 0; i < n; i++) {
            int minCuts = i; // Max cuts is i (each char is individual cut)
            for (int j = 0; j <= i; j++) {
                if (s.charAt(i) == s.charAt(j) && (i - j <= 2 || isPal[j + 1][i - 1])) {
                    isPal[j][i] = true;
                    minCuts = (j == 0) ? 0 : Math.min(minCuts, cuts[j - 1] + 1);
                }
            }
            cuts[i] = minCuts;
        }
        return cuts[n - 1];
    }

    // ------------------------------------------------------------------------
    // Test Suite for Part 1
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING STRING PART 1 (PALINDROMES) TEST SUITE ");
        System.out.println("=================================================");

        // 1. Reverse Words
        assert reverseWords("the sky is blue").equals("blue is sky the");
        assert reverseWords("  hello world  ").equals("world hello");

        // 2. Remove Duplicates
        assert removeDuplicates("tree traversal").equals("tre avsl");

        // 3. Remove White Spaces
        assert removeWhiteSpaces("a b  c   d").equals("abcd");

        // 4. Find All Palindrome Substrings
        List<String> palindromes = findAllPalindromeSubstrings("aba");
        assert palindromes.contains("a") && palindromes.contains("b") && palindromes.contains("aba");

        // 5. Longest Palindromic Substring
        assert longestPalindromicSubstring("babad").equals("bab") || longestPalindromicSubstring("babad").equals("aba");
        assert longestPalindromicSubstring("cbbd").equals("bb");

        // 6. Longest Palindromic Subsequence
        assert longestPalindromicSubsequence("bbbab") == 4; // "bbbb"

        // 7. Count Palindromic Substrings
        assert countPalindromicSubstrings("aaa") == 6; // 'a', 'a', 'a', 'aa', 'aa', 'aaa'

        // 8. Min Deletions for Palindrome
        assert minDeletionsToMakePalindrome("agbcba") == 1; // Delete 'g'

        // 9. Palindromic Partitioning Min Cuts
        assert minCutPalindromicPartitioning("aab") == 1; // "aa" | "b"

        System.out.println(" STRING PART 1 ALL TESTS PASSED!");
        System.out.println("=================================================");
    }
}
