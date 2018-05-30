package com.eastwood.tools.idea;

import java.io.*;

public class Utils {

    public static void applyMicroModulePlugin(File buildFile) {
        if (!buildFile.exists()) {
            return;
        }
        String content = read(buildFile);
        content = "apply plugin: 'micro-module'\n//apply plugin: 'micro-module-code-check'\n" + content;
        write(buildFile, content);
    }

    public static void moveSrcDir(File moduleDir) {
        File targetDir = new File(moduleDir, "main/src");
        File sourceDir = new File(moduleDir, "src");
        copy(sourceDir, sourceDir.getAbsolutePath(), targetDir);
        delete(sourceDir);

        File buildFile = new File(moduleDir, "main/build.gradle");
        Utils.write(buildFile, "dependencies {\n//    implementation microModule(':you-created-micro-module-name')\n}");
    }

    public static void copy(File file, String prefix, File targetDir) {
        if (file.isDirectory()) {

            String packageName = file.getAbsolutePath().replace(prefix, "");
            File target = new File(targetDir, packageName);
            if (!target.exists()) {
                target.mkdir();
            }

            for (File childFile : file.listFiles()) {
                copy(childFile, prefix, targetDir);
            }
        } else {
            String packageName = file.getParent().replace(prefix, "");
            File targetParent = new File(targetDir, packageName);
            if (!targetParent.exists()) targetParent.mkdirs();

            File target = new File(targetParent, file.getName());

            InputStream input = null;
            OutputStream output = null;
            try {
                input = new FileInputStream(file);
                output = new FileOutputStream(target);
                byte[] buf = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buf)) > 0) {
                    output.write(buf, 0, bytesRead);
                }
                input.close();
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void delete(File file) {
        if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                delete(childFile);
            }
        }
        file.delete();
    }

    public static void write(File target, String content) {
        try {
            Writer writer = null;
            writer = new FileWriter(target);
            BufferedWriter bw = new BufferedWriter(writer);
            bw.write(content);
            bw.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String read(File target) {
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(target));
            String s = null;
            while ((s = br.readLine()) != null) {
                result.append(s + System.lineSeparator());
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

}
