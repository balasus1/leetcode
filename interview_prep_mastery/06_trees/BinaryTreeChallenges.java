package interview_prep_mastery._06_trees;

import java.util.*;

/**
 * ============================================================================
 * MODULE 06: TREES - COMPREHENSIVE BINARY TREE & BST CHALLENGES
 * ============================================================================
 */
public class BinaryTreeChallenges {

    // TreeNode Definition
    public static class TreeNode {
        public int val;
        public TreeNode left;
        public TreeNode right;
        public TreeNode next;   // For sibling connection
        public TreeNode parent; // For parent pointer challenges

        public TreeNode(int val) {
            this.val = val;
            this.left = null;
            this.right = null;
            this.next = null;
            this.parent = null;
        }
    }

    // ------------------------------------------------------------------------
    // 1. Check if Two Binary Trees are Identical
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n)
     * - Space: O(h) recursion stack
     */
    public static boolean isIdentical(TreeNode root1, TreeNode root2) {
        if (root1 == null && root2 == null) return true;
        if (root1 == null || root2 == null) return false;
        return (root1.val == root2.val)
                && isIdentical(root1.left, root2.left)
                && isIdentical(root1.right, root2.right);
    }

    // ------------------------------------------------------------------------
    // 2. In-Order Iterator for a Binary Tree
    // ------------------------------------------------------------------------
    /**
     * Space: O(h) where h is tree height
     * Time: O(1) amortized for next()
     */
    public static class BSTIterator {
        private Stack<TreeNode> stack = new Stack<>();

        public BSTIterator(TreeNode root) {
            pushAllLeft(root);
        }

        private void pushAllLeft(TreeNode node) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
        }

        public boolean hasNext() {
            return !stack.isEmpty();
        }

        public int next() {
            TreeNode curr = stack.pop();
            pushAllLeft(curr.right);
            return curr.val;
        }
    }

    // ------------------------------------------------------------------------
    // 3. Iterative In-order Traversal of Binary Tree
    // ------------------------------------------------------------------------
    public static List<Integer> inorderTraversalIterative(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        Stack<TreeNode> stack = new Stack<>();
        TreeNode curr = root;

        while (curr != null || !stack.isEmpty()) {
            while (curr != null) {
                stack.push(curr);
                curr = curr.left;
            }
            curr = stack.pop();
            result.add(curr.val);
            curr = curr.right;
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // 4. In-order Successor of Binary Search Tree (No Parent Pointer)
    // ------------------------------------------------------------------------
    /**
     * In BST: If right subtree exists, successor is min in right subtree.
     * Otherwise, successor is the lowest ancestor whose left child contains target.
     *
     * Complexity:
     * - Time: O(h)
     * - Space: O(1)
     */
    public static TreeNode inorderSuccessorBST(TreeNode root, TreeNode p) {
        if (root == null || p == null) return null;
        TreeNode successor = null;
        TreeNode curr = root;

        while (curr != null) {
            if (p.val < curr.val) {
                successor = curr;
                curr = curr.left;
            } else {
                curr = curr.right;
            }
        }
        return successor;
    }

    // ------------------------------------------------------------------------
    // 5. In-order Successor with Parent Pointers
    // ------------------------------------------------------------------------
    public static TreeNode inorderSuccessorWithParent(TreeNode node) {
        if (node == null) return null;

        // Case 1: Right subtree exists -> min in right subtree
        if (node.right != null) {
            TreeNode curr = node.right;
            while (curr.left != null) {
                curr = curr.left;
            }
            return curr;
        }

        // Case 2: No right subtree -> traverse up to first ancestor for which node is in left subtree
        TreeNode curr = node;
        TreeNode p = node.parent;
        while (p != null && p.right == curr) {
            curr = p;
            p = p.parent;
        }
        return p;
    }

    // ------------------------------------------------------------------------
    // 6. Level Order Traversal of Binary Tree (BFS)
    // ------------------------------------------------------------------------
    public static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<Integer> currentLevel = new ArrayList<>();
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                currentLevel.add(node.val);
                if (node.left != null) queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
            result.add(currentLevel);
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // 7. Validate Binary Search Tree (Is BST Valid?)
    // ------------------------------------------------------------------------
    public static boolean isValidBST(TreeNode root) {
        return validateBST(root, null, null);
    }

    private static boolean validateBST(TreeNode node, Integer min, Integer max) {
        if (node == null) return true;
        if ((min != null && node.val <= min) || (max != null && node.val >= max)) {
            return false;
        }
        return validateBST(node.left, min, node.val) && validateBST(node.right, node.val, max);
    }

    // ------------------------------------------------------------------------
    // 8. Convert Binary Tree to Doubly Linked List (In-Place In-Order)
    // ------------------------------------------------------------------------
    private static TreeNode prevDLL = null;
    private static TreeNode headDLL = null;

    public static TreeNode convertToDLL(TreeNode root) {
        prevDLL = null;
        headDLL = null;
        dllHelper(root);
        return headDLL;
    }

    private static void dllHelper(TreeNode node) {
        if (node == null) return;
        dllHelper(node.left);
        if (prevDLL == null) {
            headDLL = node;
        } else {
            node.left = prevDLL;
            prevDLL.right = node;
        }
        prevDLL = node;
        dllHelper(node.right);
    }

    // ------------------------------------------------------------------------
    // 9. Print Tree Perimeter (Boundary Traversal)
    // ------------------------------------------------------------------------
    public static List<Integer> treePerimeter(TreeNode root) {
        List<Integer> perimeter = new ArrayList<>();
        if (root == null) return perimeter;
        perimeter.add(root.val);

        // 1. Left boundary (excluding leaves)
        collectLeftBoundary(root.left, perimeter);
        // 2. Leaf nodes
        collectLeaves(root.left, perimeter);
        collectLeaves(root.right, perimeter);
        // 3. Right boundary (bottom-up, excluding leaves)
        collectRightBoundary(root.right, perimeter);
        return perimeter;
    }

    private static void collectLeftBoundary(TreeNode node, List<Integer> res) {
        while (node != null) {
            if (node.left != null || node.right != null) res.add(node.val);
            node = (node.left != null) ? node.left : node.right;
        }
    }

    private static void collectLeaves(TreeNode node, List<Integer> res) {
        if (node == null) return;
        if (node.left == null && node.right == null) {
            res.add(node.val);
            return;
        }
        collectLeaves(node.left, res);
        collectLeaves(node.right, res);
    }

    private static void collectRightBoundary(TreeNode node, List<Integer> res) {
        List<Integer> temp = new ArrayList<>();
        while (node != null) {
            if (node.left != null || node.right != null) temp.add(node.val);
            node = (node.right != null) ? node.right : node.left;
        }
        for (int i = temp.size() - 1; i >= 0; i--) {
            res.add(temp.get(i));
        }
    }

    // ------------------------------------------------------------------------
    // 10. Connect Same Level Siblings & All Siblings
    // ------------------------------------------------------------------------
    public static void connectSameLevelSiblings(TreeNode root) {
        if (root == null) return;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            TreeNode prev = null;
            for (int i = 0; i < levelSize; i++) {
                TreeNode curr = queue.poll();
                if (prev != null) {
                    prev.next = curr;
                }
                prev = curr;
                if (curr.left != null) queue.offer(curr.left);
                if (curr.right != null) queue.offer(curr.right);
            }
        }
    }

    public static void connectAllSiblings(TreeNode root) {
        if (root == null) return;
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        TreeNode prev = null;

        while (!queue.isEmpty()) {
            TreeNode curr = queue.poll();
            if (prev != null) {
                prev.next = curr;
            }
            prev = curr;
            if (curr.left != null) queue.offer(curr.left);
            if (curr.right != null) queue.offer(curr.right);
        }
    }

    // ------------------------------------------------------------------------
    // 11. Serialize & Deserialize Binary Tree
    // ------------------------------------------------------------------------
    public static String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        serializeHelper(root, sb);
        return sb.toString();
    }

    private static void serializeHelper(TreeNode node, StringBuilder sb) {
        if (node == null) {
            sb.append("#,");
            return;
        }
        sb.append(node.val).append(",");
        serializeHelper(node.left, sb);
        serializeHelper(node.right, sb);
    }

    public static TreeNode deserialize(String data) {
        Queue<String> nodes = new LinkedList<>(Arrays.asList(data.split(",")));
        return deserializeHelper(nodes);
    }

    private static TreeNode deserializeHelper(Queue<String> nodes) {
        String val = nodes.poll();
        if (val == null || val.equals("#") || val.isEmpty()) return null;
        TreeNode node = new TreeNode(Integer.parseInt(val));
        node.left = deserializeHelper(nodes);
        node.right = deserializeHelper(nodes);
        return node;
    }

    // ------------------------------------------------------------------------
    // 12. Nth Highest Number in BST (Reverse In-order: Right -> Root -> Left)
    // ------------------------------------------------------------------------
    private static int nthCount = 0;
    private static int nthResult = -1;

    public static int findNthHighestInBST(TreeNode root, int n) {
        nthCount = 0;
        nthResult = -1;
        nthHighestHelper(root, n);
        return nthResult;
    }

    private static void nthHighestHelper(TreeNode node, int n) {
        if (node == null || nthCount >= n) return;
        nthHighestHelper(node.right, n);
        nthCount++;
        if (nthCount == n) {
            nthResult = node.val;
            return;
        }
        nthHighestHelper(node.left, n);
    }

    // ------------------------------------------------------------------------
    // 13. Mirror Binary Tree Nodes
    // ------------------------------------------------------------------------
    public static TreeNode mirrorBinaryTree(TreeNode root) {
        if (root == null) return null;
        TreeNode left = mirrorBinaryTree(root.left);
        TreeNode right = mirrorBinaryTree(root.right);
        root.left = right;
        root.right = left;
        return root;
    }

    // ------------------------------------------------------------------------
    // 14. Delete Zero Sum Sub-Trees (Post-order evaluation)
    // ------------------------------------------------------------------------
    public static TreeNode deleteZeroSumSubtree(TreeNode root) {
        if (root == null) return null;
        int sum = deleteZeroSumHelper(root);
        if (sum == 0) return null;
        return root;
    }

    private static int deleteZeroSumHelper(TreeNode node) {
        if (node == null) return 0;
        int leftSum = deleteZeroSumHelper(node.left);
        int rightSum = deleteZeroSumHelper(node.right);
        if (leftSum == 0 && node.left != null) node.left = null;
        if (rightSum == 0 && node.right != null) node.right = null;
        return node.val + (node.left == null ? 0 : leftSum) + (node.right == null ? 0 : rightSum);
    }

    // ------------------------------------------------------------------------
    // 15. Convert N-ary Tree to Binary Tree (Left-Child Right-Sibling)
    // ------------------------------------------------------------------------
    public static class NaryTreeNode {
        public int val;
        public List<NaryTreeNode> children = new ArrayList<>();
        public NaryTreeNode(int val) {
            this.val = val;
        }
    }

    public static TreeNode convertNaryToBinary(NaryTreeNode root) {
        if (root == null) return null;
        TreeNode bNode = new TreeNode(root.val);
        if (!root.children.isEmpty()) {
            bNode.left = convertNaryToBinary(root.children.get(0));
            TreeNode curr = bNode.left;
            for (int i = 1; i < root.children.size(); i++) {
                curr.right = convertNaryToBinary(root.children.get(i));
                curr = curr.right;
            }
        }
        return bNode;
    }

    // ------------------------------------------------------------------------
    // Test Suite for Tree Challenges
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING TREE CHALLENGES TEST SUITE ");
        System.out.println("=================================================");

        // Build sample BST:
        //        10
        //       /  \
        //      5    15
        //     / \     \
        //    2   7     20
        TreeNode root = new TreeNode(10);
        root.left = new TreeNode(5);
        root.right = new TreeNode(15);
        root.left.left = new TreeNode(2);
        root.left.right = new TreeNode(7);
        root.right.right = new TreeNode(20);

        // 1. Identical Check
        assert isIdentical(root, deserialize(serialize(root)));

        // 2. BST Iterator
        BSTIterator it = new BSTIterator(root);
        List<Integer> iterVals = new ArrayList<>();
        while (it.hasNext()) iterVals.add(it.next());
        assert iterVals.equals(Arrays.asList(2, 5, 7, 10, 15, 20));

        // 3. Iterative Inorder
        assert inorderTraversalIterative(root).equals(Arrays.asList(2, 5, 7, 10, 15, 20));

        // 4. Inorder Successor
        TreeNode succ = inorderSuccessorBST(root, root.left.right); // successor of 7 -> 10
        assert succ != null && succ.val == 10;

        // 5. Level Order
        List<List<Integer>> levels = levelOrder(root);
        assert levels.size() == 3;
        assert levels.get(0).equals(Collections.singletonList(10));

        // 6. Valid BST
        assert isValidBST(root);

        // 7. Tree Perimeter
        List<Integer> perim = treePerimeter(root);
        assert perim.get(0) == 10;

        // 8. Sibling Connection
        connectSameLevelSiblings(root);
        assert root.left.next == root.right;
        assert root.left.left.next == root.left.right;

        // 9. Nth Highest in BST
        assert findNthHighestInBST(root, 2) == 15; // 2nd highest is 15

        // 10. Mirror Tree
        TreeNode mirrored = mirrorBinaryTree(deserialize(serialize(root)));
        assert mirrored.left.val == 15 && mirrored.right.val == 5;

        // 11. Zero Sum Subtree deletion
        TreeNode zTree = new TreeNode(8);
        zTree.left = new TreeNode(5);
        zTree.right = new TreeNode(6);
        zTree.left.left = new TreeNode(-2);
        zTree.left.right = new TreeNode(-3); // Subtree rooted at zTree.left has sum 5 + (-2) + (-3) = 0
        zTree = deleteZeroSumSubtree(zTree);
        assert zTree.left == null && zTree.right != null;

        // 12. N-ary to Binary
        NaryTreeNode nRoot = new NaryTreeNode(1);
        nRoot.children.add(new NaryTreeNode(2));
        nRoot.children.add(new NaryTreeNode(3));
        nRoot.children.add(new NaryTreeNode(4));
        TreeNode bConv = convertNaryToBinary(nRoot);
        assert bConv.val == 1 && bConv.left.val == 2 && bConv.left.right.val == 3;

        System.out.println(" TREE CHALLENGES ALL PASSED!");
        System.out.println("=================================================");
    }
}
