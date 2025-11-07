package brocode.dsa;

import java.util.Stack;

public class StackExample {
    static void main(String[] args) {
        Stack<String> stack = new Stack<>();
        stack.push("Minecraft\n");
        stack.push("Skyrim\n");
        stack.push("Doom\n");
        stack.push("Borderlands\n");
        stack.push("FFVII");
        System.out.println(stack);
        stack.peek();
        System.out.println("Search position: " + stack.search("FFVII"));
    }
}
