package at.alex_s168.sblang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {

    public static Stack<Float> stack;
    public static HashMap<String, Map.Entry<Integer, List<String>>> blocks;
    public static List<String> tointerpret;
    public static boolean flag_condition = true;

    public static void main(String[] args) {
        if(args.length != 1)
            error("Invalid arguments!");

        Path filePath = Path.of(args[0]);
        String fileContent = "";

        try {
            final byte[] bytes = Files.readAllBytes(filePath);
            fileContent = new String(bytes);
        } catch (IOException e) {
            error("File does not exist!");
        }

        blocks = new HashMap<>();
        blocks.put("EXIT", Map.entry(0, List.of("SYSTEM" ,"EXIT")));
        blocks.put("DELAY", Map.entry(1, List.of("SYSTEM" ,"DELAY"))); // waits x ms (x is popped from the stack)

        String current = "";
        int id = blocks.size();
        for(String line : fileContent.split("\n")) {
            if(line.trim().equals("") || line.trim().startsWith(";"))
                continue;
            if(!line.startsWith("    ") && line.contains(":")) {
                current = line.replace(":","").trim();
                blocks.put(current, Map.entry(id, new ArrayList<>()));
                id++;
            } else {
                if(current.equals(""))
                    error("Code has to be INSIDE a block!");
                final String c = line.substring(3).split(";")[0];
                if(!c.contains("\"")) {
                    for(String x : c.trim().split(" ")) {
                        blocks.get(current).getValue().add(x);
                    }
                } else
                    blocks.get(current).getValue().add(c);
            }
        }

        String runBlock = "";

        final boolean containsStart = blocks.containsKey("start");
        final boolean containsMain = blocks.containsKey("main");
        final boolean containsRun = blocks.containsKey("run");
        if(!(containsStart || containsMain || containsRun))
            error("Program has to contain 'main', 'start' or 'run' block!");

        if(containsRun)
            runBlock = "run";
        if(containsStart)
            runBlock = "start";
        if(containsMain)
            runBlock = "main";

        stack = new Stack<>();

        tointerpret = blocks.get(runBlock).getValue();
        while (tointerpret != null) {
            interpret(tointerpret);
        }
    }

    static int calls = 0;

    public static void interpret(List<String> codeblock) {
        if (tointerpret == codeblock)
            tointerpret = null;

        if(codeblock.get(0).equals("SYSTEM")) {
            switch (codeblock.get(1)) {
                case "EXIT" -> System.exit(0);
                case "DELAY" -> { // delay in ms
                    float delay = stack.pop();
                    long start = System.currentTimeMillis();
                    while(System.currentTimeMillis() - start < delay) {
                        int x = 33333*3333; // idk why but just why not
                    }
                }
            }
            return;
        }

        for (String code : codeblock) {
            code = code.trim();

            if (code.startsWith("\"")) {
                for (char c : code.replace("\"", "").toCharArray())
                    stack.push((float) c);
            } else if (code.startsWith("'")) {
                stack.push((float) code.charAt(1));
            } else if (code.equals("r")) { // reverses the whole stack
                Queue<Float> queue = new LinkedList<>();
                while (!stack.isEmpty()) {
                    queue.add(stack.pop());
                }
                while (!queue.isEmpty()) {
                    stack.add(queue.remove());
                }
            } else if (code.equals("r-")) { // reverses the top x elements of the stack (x is stack[-1] popped)
                Queue<Float> queue = new LinkedList<>();
                int amount = (int) (float) stack.pop();
                for (int i = 0; i < amount; i++) {
                    queue.add(stack.pop());
                }
                while (!queue.isEmpty()) {
                    stack.add(queue.remove());
                }
            } else if (code.equals(".")) { // print and pop one element from stack
                System.out.print(stack.pop());
            } else if (code.equals(".c")) { // print and pop one element from stack as string
                System.out.print((char) (float) stack.pop());
            } else if (code.equals("__")) { // dump stack (peek all)
                while (!stack.isEmpty()) {
                    System.out.println(stack.peek());
                }
            } else if (code.equals("_")) { // print and pop stack[-1] elements from the stack
                int amount = (int) (float) stack.pop();
                for (int i = 0; i < amount; i++) {
                    System.out.print(stack.pop());
                }
            } else if (code.equals("_s")) { // print (as string) and pop stack[-1] elements from the stack
                int amount = (int) (float) stack.pop();
                for (int i = 0; i < amount; i++) {
                    System.out.print((char) (int) (float) stack.pop());
                }
            } else if (code.equals("\\")) { // new line
                System.out.println();
            } else if (code.equals("p")) { // gets the first element from stack without popping it (peek) and puts the value on the stack
                stack.push(stack.peek());
            } else if (code.equals("p2")) { // gets the second element from stack without popping it (peek) and puts the value on the stack
                stack.push(stack.elementAt(stack.size() - 2));
            } else if (code.equals("c")) { // clears the stack
                stack.clear();
            } else if (code.equals("c-")) { // clears x elements from the top of the stack (x is stack[-1] popped)
                int amount = (int) (float) stack.pop();
                for (int i = 0; i < amount; i++) {
                    if (stack.size() - 1 >= amount)
                        stack.pop();
                }
            } else if (code.equals("s")) { // pushes the size of the stack onto the stack
                stack.push((float) stack.size());
            } else if (code.equals("sw")) { // swaps the top 2 elements on the stack
                float first = stack.pop();
                float second = stack.pop();
                stack.push(first);
                stack.push(second);
            } else if (code.equals("+")) { // arg1=pop()  arg2=pop()  push(arg1+arg2)
                float a = stack.pop();
                float b = stack.pop();
                stack.push(a + b);
            } else if (code.equals("-")) { // arg1=pop()  arg2=pop()  push(arg2-arg1)
                float a = stack.pop();
                float b = stack.pop();
                stack.push(b - a);
            } else if (code.equals("/")) { // arg1=pop()  arg2=pop()  push(arg2/arg1)
                float a = stack.pop();
                float b = stack.pop();
                stack.push(b / a);
            } else if (code.equals("%")) { // arg1=pop()  arg2=pop()  push(arg1%arg2)
                float a = stack.pop();
                float b = stack.pop();
                stack.push(a % b);
            } else if (code.equals("*")) { // arg1=pop()  arg2=pop()  push(arg1*arg2)
                float a = stack.pop();
                float b = stack.pop();
                stack.push(a * b);
            } else if (code.equals(">")) { // reads the input and pushes the char(s) to the stack
                Scanner sc = new Scanner(System.in);
                String input = sc.nextLine();
                for (char c : input.toCharArray())
                    stack.push((float) (int) c);
            } else if (code.equals(">.")) { // reads one char from the input
                Scanner sc = new Scanner(System.in);
                String input = sc.nextLine();
                stack.push((float) (int) input.charAt(0));
            } else if (code.equals("sc")) { // sets the condition flag
                flag_condition = true;
            } else if (code.equals("cc")) { // clears the condition flag
                flag_condition = false;
            } else if (code.equals("ic")) { // inverts the condition flag
                flag_condition = !flag_condition;
            } else if (code.equals("cp")) { // condition flag = ( pop() == pop() )
                flag_condition = stack.pop().equals(stack.pop());
            } else if (code.equals("ls")) { // condition flag = ( pop() < pop() )
                flag_condition = stack.pop() < stack.pop();
            } else if (code.equals("gr")) { // condition flag = ( pop() > pop() )
                flag_condition = stack.pop() > stack.pop();
            } else if (code.equals("c>")) { // pushes the condition flag on the stack
                stack.push(flag_condition ? 1f : 0f);
            } else if (code.equals("c<")) { // pops the condition flag from the stack
                flag_condition = stack.pop() != 0;
            } else if (code.equals("<")) { // jump to the code block with the id x (x is stack[-1] popped) IF the condition flag is true
                if(!flag_condition)
                    continue;
                int id = (int) (float) stack.pop();
                List<String> c = null;
                for (Map.Entry<String, Map.Entry<Integer, List<String>>> e : blocks.entrySet()) {
                    if (e.getValue().getKey() == id) {
                        c = e.getValue().getValue();
                    }
                }
                if (c == null)
                    error("Code block with id " + id + " does not exist!");
                assert c != null;
                tointerpret = c;
            } else if (code.equals("<*")) { // jump to the code block with the id x (x is stack[-1] popped) as subroutine IF the condition flag is true
                if(!flag_condition)
                    continue;
                int id = (int) (float) stack.pop();
                List<String> c = null;
                for (Map.Entry<String, Map.Entry<Integer, List<String>>> e : blocks.entrySet()) {
                    if (e.getValue().getKey() == id) {
                        c = e.getValue().getValue();
                    }
                }
                if (c == null)
                    error("Code block with id " + id + " does not exist!");
                assert c != null;
                interpret(c);
            } else if (code.chars().allMatch(Character::isDigit) || (
                    code.split("\\.").length == 2 &&
                            code.split("\\.")[0].chars().allMatch(Character::isDigit) &&
                            code.split("\\.")[1].chars().allMatch(Character::isDigit)
            )) {
                stack.push(Float.valueOf(code));
            } else if (blocks.containsKey(code)) {
                stack.push(Float.valueOf(blocks.get(code).getKey()));
            } else {
                error("Command " + code + " not found!");
            }
        }

    }

    public static void error(String m) {
        System.out.println("ERROR: "+m);
        System.exit(-1);
    }

}