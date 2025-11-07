package dsa;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Question:
 * Given a string containing just the characters '(', ')', '{', '}', '[' and ']', determine if the input
 * string is valid.
 * The brackets must close in the correct order, "()" and "()[]{}" are all valid but "(]" and
 * "([)]" are not.
 * Example Questions Candidate Might Ask:
 * Q: Is the empty string valid?
 * A: Yes.
 */
public class ValidParentheses {
    static final Map<Character, Character> map = new HashMap<>(){
        {
            put('(', ')');
            put('[', ']');
            put('{', '}');
        }
    };

    static boolean isValidParentheses(String str){
        Stack<Character> stack = new Stack<>();
        for(char c : str.toCharArray()){
            if(map.containsKey(c)){
                stack.push(c);
            }
            else if(map.get(stack.pop()) != c){
                return false;
            }
        }
        return stack.isEmpty();
    }

    static void main(String[] args) {
        String input = "()[]{}";
        System.out.println(isValidParentheses(input));
    }
}
