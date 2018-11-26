package com.eastwood.tools.idea;

import java.io.*;

public class Utils {

    public static boolean isAndroidModule(File buildFile) {
        if (!buildFile.exists()) {
            return false;
        }
        String buildContent = read(buildFile);
        if (buildContent.contains("'com.android.application'") || buildContent.contains("'com.android.library'")) {
            return true;
        } else {
            return false;
        }
    }

    public static void createMicroModule(File moduleDir, String microModuleName, String packageName) {
        File microModuleDir = new File(moduleDir, microModuleName);
        microModuleDir.mkdirs();

        File buildFile = new File(microModuleDir, "build.gradle");
        Utils.addMicroModuleBuildScript(buildFile);

        File libs = new File(microModuleDir, "libs");
        libs.mkdirs();

        File srcDir = new File(microModuleDir, "src");
        srcDir.mkdirs();
        String packagePath = packageName.replace(".", "/");
        String[] types = new String[]{"androidTest", "main", "test"};
        for (String type : types) {
            new File(srcDir, type + File.separator + "java" + File.separator + packagePath).mkdirs();
        }

        File resDir = new File(srcDir, "main/res");
        resDir.mkdir();

        String[] resDirs = new String[]{"drawable", "drawable-hdpi", "drawable-xhdpi", "drawable-xxhdpi", "layout", "values"};
        for (String type : resDirs) {
            new File(resDir, type).mkdirs();
        }

        File manifestFile = new File(srcDir, "main/AndroidManifest.xml");
        String content = "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n    package=\"" + packageName + "\">\n        <application>\n            \n        </application>\n</manifest>";
        Utils.write(manifestFile, content);
    }

    public static void includeMicroModule(File buildFile, String microModuleName) {
        StringBuilder result = new StringBuilder();
        boolean include = false;
        try {
            BufferedReader br = new BufferedReader(new FileReader(buildFile));
            String s = null;
            while ((s = br.readLine()) != null) {
                if (s.contains("microModule")) {
                    String content = s.trim();
                    if (content.startsWith("microModule") && content.endsWith("{") && !content.startsWith("//")) {
                        include = true;
                        s = s + System.lineSeparator() + "    include ':" + microModuleName + "'";
                    }
                }
                result.append(s + System.lineSeparator());
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (include) {
            Utils.write(buildFile, result.toString());
        } else {
            addMicroModuleExtension(buildFile, microModuleName);
        }
    }

    public static void addMicroModuleClasspath(File buildFile) {
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(buildFile));
            String s = null;
            while ((s = br.readLine()) != null) {
                if (s.contains("classpath 'com.android.tools.build")) {
                    s = s + System.lineSeparator() + "        classpath 'com.eastwood.tools.plugins:micro-module:1.2.0'";
                }
                result.append(s + System.lineSeparator());
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Utils.write(buildFile, result.toString());
    }

    public static void applyMicroModulePlugin(File buildFile) {
        if (!buildFile.exists()) {
            return;
        }
        add(buildFile, "apply plugin: 'micro-module'", true);
    }

    public static void moveSrcDir(File moduleDir) {
        File targetDir = new File(moduleDir, "main/src");
        File sourceDir = new File(moduleDir, "src");
        copy(sourceDir, sourceDir.getAbsolutePath(), targetDir);
        delete(sourceDir);

        File buildFile = new File(moduleDir, "main/build.gradle");
        addMicroModuleBuildScript(buildFile);
    }

    public static void addMicroModuleBuildScript(File buildFile) {
        Utils.write(buildFile, "// MicroModule build file where you can add configuration options to publish MicroModule(aar) to Maven \n" +
                "// and declare MicroModule dependencies.\n" +
                "\n" +
                "microModule {\n" +
                "\n" +
                "}\n" +
                "\n" +
                "dependencies {\n" +
                "    implementation fileTree(dir: '" + buildFile.getParentFile().getName() + "/libs', include: ['*.jar'])\n" +
                "}\n");
    }

    public static void addMicroModuleExtension(File buildFile) {
        addMicroModuleExtension(buildFile, null);
    }

    public static void addMicroModuleExtension(File buildFile, String microModuleName) {
        String extension = "\nmicroModule {\n" +
                (microModuleName == null ? "\n" : "    include ':" + microModuleName + "'\n") +
                "}\n";
        add(buildFile, extension, false);
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

    public static void add(File target, String content, boolean before) {
        String targetContent = read(target);
        write(target, before ? (content + "\n" + targetContent) : (targetContent + "\n" + content));
    }

}
