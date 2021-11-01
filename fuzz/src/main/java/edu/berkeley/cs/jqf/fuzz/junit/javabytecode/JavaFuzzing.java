package edu.berkeley.cs.jqf.fuzz.junit.javabytecode;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.NonTrackingGenerationStatus;
import org.apache.bcel.classfile.JavaClass;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

public class JavaFuzzing {

    public static String JDK_PATH = "/home/instein/Instein98/JVM_Coverage/openjdk9";
    private static String[] invalidMessagePattern = new String[]{
            "[\\s\\S]*ClassFormatError[\\s\\S]*",
            "[\\s\\S]*ClassNotFoundException[\\s\\S]*",
            "[\\s\\S]*NoClassDefFoundError[\\s\\S]*",
            "[\\s\\S]*VerifyError[\\s\\S]*",};
    private static JavaClassGenerator generator = new JavaClassGenerator();
    private static final int ONE_ROUND_LIMIT = 10;
    private static Set<String> totalCoverage = new HashSet<>();
    private static Set<String> validCoverage = new HashSet<>();
    private static List<RandomInputStream> failures = new ArrayList<>();

    public static JavaClass generateJavaClass(RandomInputStream inputStream) {
        StreamBackedRandom randomFile = new StreamBackedRandom(inputStream.getRawRandomStream(), Long.BYTES);
        SourceOfRandomness random = new FastSourceOfRandomness(randomFile);
        GenerationStatus genStatus = new NonTrackingGenerationStatus(random);
//        for (int i = 0; i < 10; i ++) {
//            System.out.println(random.nextInt(200));
//        }
        return generator.generate(random, genStatus);
    }

    public static List<RandomInputStream> zest(long timeBudget, List<RandomInputStream> initial) {
        long startTime = System.currentTimeMillis();
        int totalCount = 0;  // as file name
        int validCount = 0;
        while (System.currentTimeMillis() - startTime <= timeBudget) {
            int currentSize = initial.size();
            System.out.println("************One Run Start************");
            for (int i = 0; i < currentSize; i ++) {
                System.out.printf("******Mutate NO.%d Random Stream in Initial Set****** \n", i+1);
                for (int count = 0; count < ONE_ROUND_LIMIT; count ++) {
                    System.out.printf("****Round No.%d**** \n", count+1);
                    JavaClass targetClass = null;
                    RandomInputStream currentRandom = initial.get(i);
                    RandomInputStream newRandom = null;
                    while(targetClass == null){
                        try{
                            newRandom = currentRandom.fuzz(new Random());
                            targetClass = generateJavaClass(newRandom);
                        }catch (Exception e){
//                            e.printStackTrace();
                        }
                    }
                    try {
                        String[] coverageStrings = runJVM(targetClass, "classfile");
                        String result = coverageStrings[0];
                        String coverage = coverageStrings[1];
                        if (!totalCoverage.contains(coverage)) {
                            totalCount ++;
                            totalCoverage.add(coverage);
                            System.out.println("Added to Total:" + totalCount + "\n");
                            initial.add(newRandom);
                            saveRandomSequence(newRandom, "./totalCoverage", "" + totalCount);
                        }
                        if (result.equals("valid") && !validCoverage.contains(coverage)) {
                            validCount ++;
                            validCoverage.add(coverage);
                            System.out.println("Added to Valid:" + validCount + "\n");
                            initial.add(newRandom);
                            saveRandomSequence(newRandom, "./validCoverage", "" + validCount);
                        }
                    } catch (FailException e) {
                        failures.add(newRandom);
                    }
                    if (System.currentTimeMillis() - startTime >= timeBudget){
                        saveStaticsCount("./statics.log");
                        return initial;
                    }
                }
                saveStaticsCount("./statics.log");
            }
        }
        saveStaticsCount("./statics.log");
        return initial;
    }

    public static Map<String, Integer> staticsCount = new HashMap<>();
    public static String[] runJVM(JavaClass clazz, String coverageDir) throws FailException {
        String covStatics = null;
        try {
            String[] packageAndClass = clazz.getClassName().split("[.]");
            String dirPath = "./" +  packageAndClass[0];
            String className = packageAndClass[1] + ".class";
            mkdir(dirPath);
            clazz.dump(dirPath + "/" + className);

            String classDirPath = "./";
//            String classDirPath = JDK_PATH + "/build/linux-x86_64-normal-server-release/jdk/bin/";
            String command = JDK_PATH + "/build/linux-x86_64-normal-server-release/jdk/bin/java " + clazz.getClassName();

            // Run the class file in JVM, check if it is valid
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
            System.out.println(result);

            command = "bash  " + JDK_PATH + "/collect_coverage.sh " + clazz.getClassName();
            process = Runtime.getRuntime().exec(command);
            status = process.waitFor();

            covStatics = getCoverageStatics(clazz.getClassName(), coverageDir);
            if (staticsCount.containsKey(covStatics)) {
                Integer old = staticsCount.get(covStatics);
                staticsCount.put(covStatics, old + 1);
            } else {
                staticsCount.put(covStatics, 1);
            }

            if(!isClassFileValid(result.toString())){
                return new String[]{"invalid", covStatics + getPathLabel(clazz.getClassName(), coverageDir)};
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
//        System.out.println("It is Valid!");
        return new String[]{"valid", covStatics + getPathLabel(clazz.getClassName(), coverageDir)};
    }

    public static String getPathLabel(String className, String dirPath){
        if (dirPath.equals(".") || dirPath.equals("./") || dirPath.equals("*")){
            StringBuilder pathLabel = new StringBuilder();
            String htmlPath = JDK_PATH + "/HTML" + className + "/index.html";
            File htmlFile = new File(htmlPath);
            StringBuilder htmlContent = new StringBuilder();
            try{
                BufferedReader reader = new BufferedReader(new FileReader(htmlFile));
                String line = null;
                while((line = reader.readLine()) != null){
                    htmlContent.append(line);
                }
            }catch (IOException e){
                e.printStackTrace();
            }
            String pattern = "\"coverFile\"><a href=\"([^<]*?)/index.html\"";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(htmlContent);
            while(m.find()){
                pathLabel.append(getPathLabelForDir(className, m.group(1)));
            }
            return pathLabel.toString();
        }else{
            return getPathLabelForDir(className, dirPath);
        }
    }

    public static String getPathLabelForDir(String className, String dirPath){
        StringBuilder pathLabel = new StringBuilder();
//        List<String> gcovPath = new ArrayList<String>();
        String htmlPath = JDK_PATH + "/HTML" + className + "/" + dirPath + "/index.html";
        File htmlFile = new File(htmlPath);
        StringBuilder htmlContent = new StringBuilder();
        try{
            BufferedReader reader = new BufferedReader(new FileReader(htmlFile));
            String line = null;
            while((line = reader.readLine()) != null){
                htmlContent.append(line);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        String pattern = "class=\"coverFile\"><a href=\"(([^<]*?.cpp).gcov.html)\"";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(htmlContent);
        while(m.find()){
            pathLabel.append(m.group(2) + "\n");
            String cppHtmlPath = JDK_PATH + "/HTML" + className + "/" + dirPath + "/" + m.group(1);
            File cppHtmlFile = new File(cppHtmlPath);
            StringBuilder cppHtmlContent = new StringBuilder();
            try{
                BufferedReader reader = new BufferedReader(new FileReader(cppHtmlFile));
                String line = null;
                while((line = reader.readLine()) != null){
                    cppHtmlContent.append(line);
                }
            }catch (IOException e){
                e.printStackTrace();
            }
            String lp = "class=\"lineNum\">([^:]*?)</span><span class=\"lineCov\">(.*?):";
            Pattern linePattern = Pattern.compile(lp);
            Matcher lineMatcher = linePattern.matcher(cppHtmlContent);
            while(lineMatcher.find()){
                pathLabel.append(lineMatcher.group(1) + "-" + lineMatcher.group(2) + "\n");
            }
        }
        return pathLabel.toString();
    }

    public static void saveRandomSequence(RandomInputStream target, String dir, String fileName){
        mkdir(dir);
        String filePath = dir + "/" + fileName;
        String content = target.randomList.toString();
        dumpFile(filePath, content);
//        try{
//            File file = new File(dir);
//            if (!file.exists()) {
//                file.mkdirs();
//            }
//            file = new File(dir + "/" + fileName);
//            file.createNewFile();
//
//            FileWriter fw = new FileWriter(dir + "/" + fileName);
//            BufferedWriter bw = new BufferedWriter(fw);
//            bw.write(target.randomList.toString());
//            bw.close();
//            fw.close();
//        }catch (Exception e){
//            e.printStackTrace();
//        }
    }

    public static void testZest(long timeBudget, int initialSize){  // timeBudget = minutes
        List<RandomInputStream> initialStream = new ArrayList<RandomInputStream>();
        for(int i = 0; i < initialSize; i++){
            initialStream.add(new RandomInputStream());
        }
        zest(timeBudget * 60000, initialStream);
    }

    public static void mkdir(String path){
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static void dumpFile(String filePath, String content){
        try{
            File file = new File(filePath);
            file.createNewFile();
            FileWriter fw = new FileWriter(filePath);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
            fw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static boolean isClassFileValid(String message){
        for (String pattern: invalidMessagePattern) {
            if(Pattern.matches(pattern, message))
                return false;
        }
        return true;
    }

    public static void main1(String[] args) throws IOException, FailException {
//        System.out.println(getPathLabel("hello", "."));
        JavaClass clazz = null;
        RandomInputStream newStream = null;
        while(clazz == null){
            try{
                RandomInputStream inputStream = new RandomInputStream();
                newStream = inputStream.fuzz(new Random());
                clazz = generateJavaClass(newStream);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        saveRandomSequence(newStream, "./test", "test");
        String[] coverageStrings = runJVM(clazz, "classfile");
        String result = coverageStrings[0];
        String coverage = coverageStrings[1];
        System.out.println(result + "\n" + coverage);
    }

    public static String getCoverageStatics(String className, String dirPath){
        StringBuilder result = new StringBuilder();
        String htmlPath = JDK_PATH + "/HTML" + className + "/" + dirPath + "/index.html";
        File htmlFile = new File(htmlPath);
        StringBuilder htmlContent = new StringBuilder();
        try{
            BufferedReader reader = new BufferedReader(new FileReader(htmlFile));
            String line = null;
            while((line = reader.readLine()) != null){
                htmlContent.append(line);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        String pattern = "<td class=\"headerCovTableEntryLo\">([\\d.]+) %</td>";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(htmlContent);
        if (m.find()){
            result.append("Line Coverage: " + m.group(1) + "%  ");
        }
        if (m.find()){
            result.append("Function Coverage: " + m.group(1) + "%  ");
        }
        return result.toString();
    }

    public static void saveStaticsCount(String filePath){
        StringBuilder content = new StringBuilder();
        for(String key: staticsCount.keySet()){
            content.append(key + ": " + staticsCount.get(key) + "\n");
        }
        SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");// a为am/pm的标记
        Date date = new Date();// 获取当前时间
        content.append(sdf.format(date));
        dumpFile(filePath, content.toString());
    }

    public static void main(String[] args) throws IOException, FailException {
        testZest(2, 10);
    }
}
