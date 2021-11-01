// Run this in the same directory where the example folder is.

 package edu.berkeley.cs.jqf.fuzz.junit.javabytecode;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class runDifferentJVM {
    private static String examplePath = "/home/mingyuanwu/JDK/testExamples/example";
    private static String[] JVMPath = new String[]{
            "/home/mingyuanwu/JDK/openjdk7/bin/java",
            "/home/mingyuanwu/JDK/openjdk8/build/linux-x86_64-normal-server-slowdebug/jdk/bin/java",
            "/home/mingyuanwu/JDK/openjdk9/build/linux-x86_64-normal-server-release/jdk/bin/java",
    };
    private static String[] outputMessagePattern = new String[]{
            "[\\s\\S]*ClassFormatError[\\s\\S]*",
            "[\\s\\S]*ClassNotFoundException[\\s\\S]*",
            "[\\s\\S]*NoClassDefFoundError[\\s\\S]*",
            "[\\s\\S]*VerifyError[\\s\\S]*",
            "[\\s\\S]*Main method not found[\\s\\S]*",};
    private static String[] outputType = new String[]{
            "ClassFormatError",
            "ClassNotFoundException",
            "NoClassDefFoundError",
            "VerifyError",
            "Main method not found",
    };
    public static void main(String[] args) {
//        runJVM("/home/instein/Instein98/JVM_Coverage/openjdk9/build/linux-x86_64-normal-server-release/jdk/bin/java",
//                "/home/instein/Instein98/JVM_Coverage/example",
//                "/home/instein/Instein98/JVM_Coverage/example/result.log");
        String pattern = ".+/JDK/(.+?)/.+";
        Pattern r = Pattern.compile(pattern);
        for(String jvmPath:JVMPath){
            Matcher m = r.matcher(jvmPath);
            m.find();
            System.out.println("Running " + m.group(1));
            runJVM(jvmPath,examplePath, examplePath+"/"+m.group(1)+".log");
        }
    }

    // The argument inputDir should be a "example" directory with class files in it.
    public static void runJVM(String jvmPath, String inputDir, String outputPath){
        createFile(outputPath);
        String originInputName = null;
        File file = new File(inputDir);
        File[] fs = file.listFiles();
        for(File f:fs) {
            if (f.getName().equals("A.class") ||
                f.getName().substring(f.getName().lastIndexOf(".") + 1).equals("log"))
                continue;
            if (!f.isDirectory()){
                originInputName = f.getName();
                System.out.println("Processing " + originInputName);
                f.renameTo(new File(inputDir + "/A.class"));
                try{
                    String command = jvmPath + " example.A";
                    Process process = Runtime.getRuntime().exec(command);
                    int status = process.waitFor();
                    BufferedReader bufferIn = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
                    BufferedReader bufferError = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
                    StringBuilder result = new StringBuilder();
                    String line = null;
                    while ((line = bufferIn.readLine()) != null) {
                        result.append(line).append('\n');
                    }
                    while ((line = bufferError.readLine()) != null) {
                        result.append(line).append('\n');
                    }
                    appendFile(outputPath,originInputName + ":");
                    appendFile(outputPath,result.toString());

                    boolean foundType = false;
                    for(int i = 0; i < outputMessagePattern.length; i++){
                        if (Pattern.matches(outputMessagePattern[i], result)){
                            appendFile(outputPath,originInputName + "-Result:" + outputType[i] + "\n\n");
                            foundType = true;
                        }
                    }
                    if (!foundType){
                        appendFile(outputPath,originInputName + "-Result:Unknown" + "\n\n");
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                new File(inputDir + "/A.class").renameTo(new File(inputDir + "/" + originInputName));
            }
        }
    }

    public static void createFile(String filePath){
        try{
            File file = new File(filePath);
            file.createNewFile();
            FileWriter fw = new FileWriter(filePath,false);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("");
            bw.close();
            fw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void appendFile(String filePath, String content){
        try{
            FileWriter fw = new FileWriter(filePath,true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
            fw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
