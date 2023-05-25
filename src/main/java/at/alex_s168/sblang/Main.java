package at.alex_s168.sblang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static Stack<Float> stack;
    public static Stack<Float> dataStack;
    public static HashMap<String, Map.Entry<Integer, List<String>>> blocks;
    public static List<String> tointerpret;
    public static boolean flag_condition = true;
    public static HashMap<Integer, Map.Entry<String, String>> ioFiles; // id, name, path

    private static int nextId;
    private static int nextFileId;

    public static void blockify(String file) {
        Path filePath = Path.of(file);
        String fileContent = "";

        try {
            final byte[] bytes = Files.readAllBytes(filePath);
            fileContent = new String(bytes);
        } catch (IOException e) {
            error("File does not exist!");
        }

        String current = "";
        for(String line : fileContent.split("\n")) {
            if(line.trim().equals("") || line.trim().startsWith(";"))
                continue;
            if(!line.startsWith("    ") && line.contains(":")) {
                current = line.replace(":","").trim();
                blocks.put(current, Map.entry(nextId, new ArrayList<>()));
                nextId++;
            } else {
                if(current.equals("")) {
                    final String c = line.trim().split(";")[0];
                    if(c.startsWith("link ")) {
                        final String tolink = c.substring("link ".length());
                        blockify(tolink);
                        continue;
                    } else if(c.startsWith("io ")) {
                        final String[] sp = c.substring("io ".length()).split(" ");
                        if(sp[0].equals("file")) {
                            ioFiles.put(nextFileId, Map.entry(sp[1], sp[2]));
                            nextFileId++;
                            continue;
                        } else {
                            error("Invalid IO operation!");
                        }
                    } else
                        error("Code has to be INSIDE a block!");
                }
                final String c = line.substring(3).split(";")[0];
                if(!c.contains("\"")) {
                    for(String x : c.trim().split(" ")) {
                        blocks.get(current).getValue().add(x);
                    }
                } else {
                    String[] sp = c.split("\"");
                    int it = 0;
                    for(String s : sp) {
                        if(it % 2 == 1)
                            blocks.get(current).getValue().add("\""+s+"\"");
                        else {
                            for (String x : s.trim().split(" ")) {
                                if(!x.equals(""))
                                    blocks.get(current).getValue().add(x);
                            }
                        }
                        it++;
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        if(args.length != 1)
            error("Invalid arguments!");

        ioFiles = new HashMap<>();

        blocks = new HashMap<>();
        blocks.put("EXIT", Map.entry(0, List.of("SYSTEM" ,"EXIT"))); // exits the program
        blocks.put("DELAY", Map.entry(1, List.of("SYSTEM" ,"DELAY"))); // waits x ms (x is popped from the stack)
        blocks.put("FILE_SIZE", Map.entry(2, List.of("SYSTEM" ,"FILE_SIZE"))); // pushes the size of the content of the file with id x onto the stack (amount of chars) (x is popped from the stack)
        blocks.put("FILE_READ", Map.entry(3, List.of("SYSTEM" ,"FILE_READ"))); // gets the content of the file with id x (in chars) and pushes the content one-by-one from left to right onto the data stack (x is popped from the stack)
        blocks.put("FILE_WRITE", Map.entry(4, List.of("SYSTEM" ,"FILE_WRITE"))); // sets the content of the file with id x (in chars). content is popped from the data stack, converted to ascii and written from left to right. It writes y chars (x is popped from the stack; y is popped from the stack)

        nextId = blocks.size();
        nextFileId = ioFiles.size();

        blockify(args[0]);

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
        dataStack = new Stack<>();

        tointerpret = blocks.get(runBlock).getValue();
        while (tointerpret != null) {
            interpret(blocks.get(runBlock).getKey(), tointerpret);
        }
    }

    public static void interpret(int cbid, List<String> codeblock) {
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
                case "FILE_SIZE" -> {
                    Path filePath = Path.of(ioFiles.get((int)(float)stack.pop()).getValue());
                    try {
                        final byte[] bytes = Files.readAllBytes(filePath);
                        char[] cont = new String(bytes).toCharArray();
                        stack.push((float) cont.length);
                    } catch (IOException e) {
                        stack.push(0f);
                    }
                }
                case "FILE_READ" -> {
                    Path filePath = Path.of(ioFiles.get((int)(float)stack.pop()).getValue());
                    try {
                        final byte[] bytes = Files.readAllBytes(filePath);
                        char[] cont = new String(bytes).toCharArray();
                        for(char c : cont)
                            dataStack.push((float)c);
                    } catch (IOException ignored) {}
                }
                case "FILE_WRITE" -> {
                    Path filePath = Path.of(ioFiles.get((int)(float)stack.pop()).getValue());
                    int a = (int)(float)stack.pop();
                    StringBuilder c = new StringBuilder();
                    for (int i = 0; i < a; i++) {
                        c.append((char) (float) dataStack.pop());
                    }
                    System.out.println("towrite: "+c.toString());
                    try {
                        if (!Files.exists(filePath)) {
                            Files.createFile(filePath);
                        }
                        Files.writeString(filePath, c.toString());
                    } catch (IOException e) {
                        error("File does not exist!");
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
                for (int i = stack.size()-1; i > -1; i--) {
                    System.out.println(stack.elementAt(i));
                }
            } else if (code.equals("d_")) { // dump data stack (peek all)
                for (int i = dataStack.size()-1; i > -1; i--) {
                    System.out.println(dataStack.elementAt(i));
                }
            } else if (code.equals(">d")) { // pops element from the stack and pushes it onto the data stack
                dataStack.push(stack.pop());
            } else if (code.equals("<d")) { // pops element from the data stack and pushes it onto the stack
                stack.push(dataStack.pop());
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
            } else if (code.equals("id")) { // pushes the id of the current code block onto the stack
                stack.push(stack.peek());
            } else if (code.equals("p")) { // gets the first element from stack without popping it (peek) and puts the value on the stack
                stack.push(stack.peek());
            } else if (code.equals("p2")) { // gets the second element from stack without popping it (peek) and puts the value on the stack
                stack.push(stack.elementAt(stack.size() - 2));
            } else if (code.equals("c")) { // clears the stack
                stack.clear();
            } else if (code.equals("c-")) { // clears x elements from the top of the stack (x is stack[-1] popped)
                int amount = (int) (float) stack.pop();
                for (int i = 0; i < amount; i++) {
                    stack.pop();
                }
            } else if (code.equals("s")) { // pushes the size of the stack onto the stack
                stack.push((float) stack.size());
            } else if (code.equals("sd")) { // pushes the size of the data stack onto the stack
                stack.push((float) dataStack.size());
            } else if (code.equals("sw")) { // swaps the top 2 elements on the stack
                float first = stack.pop();
                float second = stack.pop();
                stack.push(first);
                stack.push(second);
            } else if (code.equals("|")) { // arg1=pop()  arg2=pop()  push(arg1 | arg2) bitwise or
                int a = (int) (float) stack.pop();
                int b = (int) (float) stack.pop();
                stack.push((float) (a | b));
            } else if (code.equals("&")) { // arg1=pop()  arg2=pop()  push(arg1 & arg2) bitwise and
                int a = (int) (float) stack.pop();
                int b = (int) (float) stack.pop();
                stack.push((float) (a & b));
            } else if (code.equals("!")) { // arg1=pop() push(~arg1) bitwise not
                int a = (int) (float) stack.pop();
                stack.push((float) (~a));
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

                return;
            } else if (code.equals("<*")) { // jump to the code block with the id x (x is stack[-1] popped) as subroutine IF the condition flag is true
                if(!flag_condition)
                    continue;
                int id = (int) (float) stack.pop();
                List<String> c = null;
                int i = 0;
                for (Map.Entry<String, Map.Entry<Integer, List<String>>> e : blocks.entrySet()) {
                    if (e.getValue().getKey() == id) {
                        c = e.getValue().getValue();
                        i = e.getValue().getKey();
                    }
                }
                if (c == null)
                    error("Code block with id " + id + " does not exist!");
                assert c != null;
                interpret(i, c);
            } else if (isNum(code)) {
                stack.push(Float.valueOf(code));
            } else if (blocks.containsKey(code)) {
                stack.push(Float.valueOf(blocks.get(code).getKey()));
            } else if (ioFilesContains(code)) {
                String finalCode = code;
                ioFiles.forEach((k, v) -> {
                    if(v.getKey().equals(finalCode))
                        stack.push(Float.valueOf(k));
                });
            } else {
                error("Command " + code + " not found!");
            }
        }

    }

    private static boolean ioFilesContains(String name) {
        AtomicBoolean ret = new AtomicBoolean(false);
        ioFiles.forEach((k, v) -> {
            if(v.getKey().equals(name))
                ret.set(true);
        });
        return ret.get();
    }

    private static boolean isNum(String s) {
        if(s.startsWith("-")) {
            return s.substring(1).chars().allMatch(Character::isDigit) || (
                    s.substring(1).split("\\.").length == 2 &&
                            s.substring(1).split("\\.")[0].chars().allMatch(Character::isDigit) &&
                            s.substring(1).split("\\.")[1].chars().allMatch(Character::isDigit)
            );
        }
        return s.chars().allMatch(Character::isDigit) || (
                        s.split("\\.").length == 2 &&
                        s.split("\\.")[0].chars().allMatch(Character::isDigit) &&
                        s.split("\\.")[1].chars().allMatch(Character::isDigit)
        );
    }

    public static void error(String m) {
        System.out.println("\nERROR: "+m);
        System.exit(-1);
    }

}