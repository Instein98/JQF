package edu.berkeley.cs.jqf.fuzz.junit.javabytecode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.berkeley.cs.jqf.fuzz.junit.javabytecode.JavaFuzzing.getPathLabel;
import static edu.berkeley.cs.jqf.fuzz.junit.javabytecode.JavaFuzzing.testZest;
public class jvmTest {
    public static void main(String[] args){
//        testZest(1, 10);
//        recoverClass("./totalCoverage/2", "./example/A.class");


//        String label1 = getPathLabel("back2", "classfile");
//        String label2 = getPathLabel("back1", "classfile");
//        System.out.println(label1.equals(label2));
//        for(int i = 0; i < label1.length(); i ++){
//            if (label1.charAt(i) != label2.charAt(i)){
//                System.out.println("adf");
//            }
//        }

        String path = "/home/instein/Instein98/JVM_Coverage/openjdk9/totalCoverage";		//要遍历的路径
        File file = new File(path);		//获取其file对象
        File[] fs = file.listFiles();	//遍历path下的文件和目录，放在File数组中
        for(File f:fs) {                    //遍历File[]数组
            if (!f.isDirectory())        //若非目录(即文件)，则打印
                recoverClass(f.getAbsolutePath(), "/home/instein/Instein98/JVM_Coverage/openjdk9/inputClass/" + f.getName() + ".class");
//                System.out.println(f.getName());
        }
    }

    public static void recoverClass(String sequencePath, String outputClassPath){
        List<Integer> sequence = new ArrayList<>();
        try {
            FileReader reader = new FileReader(sequencePath);
            BufferedReader br = new BufferedReader(reader);
            String line = br.readLine();
            String pattern = "\\d+";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(line);
            while (m.find()){
                sequence.add(Integer.parseInt(m.group(0)));
            }
//            System.out.println("hello");

            RandomInputStream temp = new RandomInputStream(sequence);
            JavaFuzzing.generateJavaClass(temp).dump(outputClassPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
