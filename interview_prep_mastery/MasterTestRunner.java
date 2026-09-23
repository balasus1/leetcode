package interview_prep_mastery;

/**
 * ============================================================================
 * MASTER TEST RUNNER: ALL DSA & COMPLEXITY MODULES
 * ============================================================================
 * Executes and verifies 100% of all interview preparation modules.
 */
public class MasterTestRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("           DSA & COMPLEXITY CODING INTERVIEW PREPARATION SUITE                  ");
        System.out.println("================================================================================");

        try {
            // Module 1: Complexity
            System.out.println("\n>> Running Module 01: Complexity Analysis & Big-O...");
            interview_prep_mastery._01_complexity_analysis.ComplexityChallenges.main(new String[0]);

            // Module 2: Arrays
            System.out.println("\n>> Running Module 02: Arrays (Part 1, 2, 3)...");
            interview_prep_mastery._02_arrays.ArrayChallengesPart1.main(new String[0]);
            interview_prep_mastery._02_arrays.ArrayChallengesPart2.main(new String[0]);
            interview_prep_mastery._02_arrays.ArrayChallengesPart3.main(new String[0]);

            // Module 3: Linked Lists
            System.out.println("\n>> Running Module 03: Linked Lists (Singly, Doubly, Advanced)...");
            interview_prep_mastery._03_linked_lists.SinglyLinkedListChallenges.main(new String[0]);
            interview_prep_mastery._03_linked_lists.DoublyLinkedListChallenges.main(new String[0]);
            interview_prep_mastery._03_linked_lists.AdvancedLinkedListChallenges.main(new String[0]);

            // Module 4: Strings
            System.out.println("\n>> Running Module 04: Strings & DP (Part 1, 2)...");
            interview_prep_mastery._04_strings.StringCoreAndPalindromes.main(new String[0]);
            interview_prep_mastery._04_strings.StringAdvancedAndDP.main(new String[0]);

            // Module 5: Stacks and Queues
            System.out.println("\n>> Running Module 05: Stacks & Queues...");
            interview_prep_mastery._05_stacks_and_queues.StackQueueImplementations.main(new String[0]);
            interview_prep_mastery._05_stacks_and_queues.StackQueueChallenges.main(new String[0]);

            // Module 6: Trees
            System.out.println("\n>> Running Module 06: Trees & BST...");
            interview_prep_mastery._06_trees.BinaryTreeChallenges.main(new String[0]);

            // Module 7: Trie
            System.out.println("\n>> Running Module 07: Trie (Prefix Tree)...");
            interview_prep_mastery._07_trie.TrieChallenges.main(new String[0]);

            // Module 8: Heaps
            System.out.println("\n>> Running Module 08: Heaps & Priority Queues...");
            interview_prep_mastery._08_heaps.HeapChallenges.main(new String[0]);

            // Module 9: Hash Tables
            System.out.println("\n>> Running Module 09: Hash Tables...");
            interview_prep_mastery._09_hash_tables.HashTableChallenges.main(new String[0]);

            // Module 10: Graphs
            System.out.println("\n>> Running Module 10: Graphs...");
            interview_prep_mastery._10_graphs.GraphChallenges.main(new String[0]);

            // Module 11: Concurrency & Distributed Systems
            System.out.println("\n>> Running Module 11: Concurrency & Distributed Systems...");
            interview_prep_mastery._11_concurrency_and_distributed.ConcurrencyPrimitivesAndPatterns.main(new String[0]);
            interview_prep_mastery._11_concurrency_and_distributed.DistributedSystemsCore.main(new String[0]);

            System.out.println("\n================================================================================");
            System.out.println("  CONGRATULATIONS: 100% OF ALL MODULES & TEST SUITES PASSED SUCCESSFULLY!       ");
            System.out.println("================================================================================");

        } catch (Throwable t) {
            System.err.println("\n❌ TEST SUITE FAILED WITH ERROR:");
            t.printStackTrace();
            System.exit(1);
        }
    }
}
