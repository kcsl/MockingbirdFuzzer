package afl;

import instrumentor.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Derrick Lockwood
 * @created 6/25/18.
 */
public class InstrumentLoader {

    private InstrumentLoader() {}

    private static final Logger LOGGER = Logger.getLogger(InstrumentLoader.class.getName());

    static {
        LOGGER.setParent(Logger.getLogger(Kelinci.class.getName()));
    }

    private static void clearFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    clearFolder(f);
                    if (!f.delete()) {
                        LOGGER.log(Level.WARNING, "Can't delete folder " + folder.getPath());
                    }
                } else {
                    if (!f.delete()) {
                        LOGGER.log(Level.WARNING, "Can't delete file " + f.getPath());
                    }

                }
            }
        }
    }

    public static void addToClassPath(File file) throws
            NoSuchMethodException,
            MalformedURLException,
            InvocationTargetException,
            IllegalAccessException {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), file.toURI().toURL());
    }

    private static boolean instrumentClass(String cls, InputStream classInputStream, OutputStream classOutputStream) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassTransformer ct = new ClassTransformer(cw);
        ClassReader cr;
        try {
            cr = new ClassReader(classInputStream);
        } catch (IOException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, "Error loading class " + cls, e);
            return false;
        }

        try {
            cr.accept(ct, 0);
            byte[] bytes = cw.toByteArray();
            classOutputStream.write(bytes);
        } catch (RuntimeException rte) {
            if (rte.getMessage().contains("JSR/RET")) {
					/*
					  This is an exception related to a particular construct in the bytecode that
					  is not supported by ASM. It is deprecated and should not be present in bytecode
					  generated by a recent compiler. However, the JDK contains it and it may occur elsewhere.
					  This catch simply skips the class and warns the user.
					 */
                LOGGER.log(Level.WARNING, "RuntimeException thrown during instrumentation: " + rte.getMessage());
                LOGGER.log(Level.WARNING, "Skipping instrumentation of class " + cls + "\n");
                // include original, uninstrumented class in output
                return writeClass(cls, classInputStream, classOutputStream);
            } else {
                LOGGER.log(Level.SEVERE, "Can't instrument class " + cls, rte);
                return false;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can't write instrumented class " + cls, e);
            return false;
        }
        return true;
    }

    private static boolean instrumentClass(String cls, InputStream classInputStream, Path instrumentedDir) {
        LOGGER.log(Level.INFO, "Found class: " + cls);
        String instrumentedPath = instrumentedDir.resolve(cls).toString();
        File instrumentedClazz = new File(instrumentedPath);
        LOGGER.log(Level.INFO, "Instrumenting into " + instrumentedClazz.getPath());
        try {
            Files.createDirectories(instrumentedClazz.getParentFile().toPath());
            if (!instrumentedClazz.createNewFile()) {
                LOGGER.log(Level.SEVERE, "Can't create instrumented file " + instrumentedClazz.getAbsolutePath());
                return false;
            }
            OutputStream classOutputStream = new FileOutputStream(instrumentedClazz);
            if (!instrumentClass(cls, classInputStream, classOutputStream)) {
                return false;
            }
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Can't find file ", e);
            return false;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can't create instrumented class directories " + instrumentedClazz.getPath(), e);
            return false;
        }
        return true;
    }

    private static boolean instrumentClass(File clazz, Path instrumentedDir) {
        LOGGER.log(Level.INFO, "Found class: " + clazz.getPath());
        String instrumentedPath = instrumentedDir.resolve(clazz.toPath()).toString();
        File instrumentedClazz = new File(instrumentedPath);
        LOGGER.log(Level.INFO, "Instrumenting into " + instrumentedClazz.getPath());
        try {
            Files.createDirectories(instrumentedClazz.getParentFile().toPath());
            if (!instrumentedClazz.createNewFile()) {
                LOGGER.log(Level.SEVERE, "Can't create instrumented file " + instrumentedClazz.getAbsolutePath());
                return false;
            }
            InputStream classInputStream = new FileInputStream(clazz);
            OutputStream classOutputStream = new FileOutputStream(instrumentedClazz);
            if (!instrumentClass(clazz.getName(), classInputStream, classOutputStream)) {
                return false;
            }
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Can't find file ", e);
            return false;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can't create instrumented class directories " + instrumentedClazz.getPath(), e);
            return false;
        }
        return true;
    }

    private static boolean writeClass(String cls, InputStream inputStream, OutputStream outputStream) {
        byte[] buffer = new byte[1024];
        try {
            while (inputStream.read(buffer, 0, buffer.length) != -1) {
                outputStream.write(buffer);
            }
            outputStream.flush();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can't write class " + cls, e);
            return false;
        }
        return true;
    }

    private static boolean instrumentJar(File file, Path instrumentedDir) {
        try {
            // open JAR file
            JarFile jarFile = new JarFile(file);
            Enumeration<JarEntry> entries = jarFile.entries();

            // iterate JAR entries
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                File entryFile = new File(entryName);

                if (entryName.endsWith(".class")) {
                    if (!instrumentClass(entryName, jarFile.getInputStream(entry), instrumentedDir)) {
                        return false;
                    }
                } else if (entryName.endsWith(".jar")) {
                    // load nested JAR
                    LOGGER.log(Level.INFO, "Found jar: " + entryName);
                    if (!instrumentJar(entryFile, instrumentedDir)) {
                        return false;
                    }
                }
            }

            // close JAR file
            jarFile.close();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading from JAR file: " + file, e);
            return false;
        }
        return true;
    }

    private static boolean instrument(File inputSource, Path instrumentedDir) {
        if (inputSource.isFile()) {
            String name = inputSource.getName();
            if (name.endsWith(".jar")) {
                LOGGER.log(Level.INFO, "Started instrumenting jar " + inputSource.getPath());
                if (instrumentJar(inputSource, instrumentedDir)) {
                    LOGGER.log(Level.INFO, "Finished instrumenting jar " + inputSource.getPath());
                }
            } else if (name.endsWith(".class")) {
                LOGGER.log(Level.INFO, "Started instrumenting class " + inputSource.getPath());
                if (instrumentClass(inputSource, instrumentedDir)){
                    LOGGER.log(Level.INFO, "Finished instrumenting jar " + inputSource.getPath());
                }
            } else {
                LOGGER.log(Level.SEVERE, "Can't read file as source " + inputSource.getPath());
                return false;
            }
        } else {
            LOGGER.log(Level.INFO, "Started instrumenting directory " + inputSource.getPath());
            File[] classes = inputSource.listFiles(pathname -> pathname.getName().contains(".class"));
            if (classes == null) {
                LOGGER.log(Level.SEVERE, "No files found in " + inputSource.getPath());
                return false;
            }
            for (File file : classes) {
                if (!instrumentClass(file, instrumentedDir)) {
                    return false;
                }
            }
            LOGGER.log(Level.INFO, "Finished instrumenting directory " + inputSource.getPath());
        }
        return true;
    }

    static boolean loadInstrumentedClasses(File inputSource, File instrumentedDir) {
        if (inputSource != null) {
            //Instrument and place in instrumented dir
            if (inputSource.exists()) {
                if (!instrumentedDir.exists()) {
                    LOGGER.log(Level.INFO, "Instrumented directory not there creating " + instrumentedDir.getName());
                    if (!instrumentedDir.mkdir()) {
                        LOGGER.log(Level.SEVERE, "Can't create instrumented directory " + instrumentedDir.getName());
                        return false;
                    }
                } else if (instrumentedDir.isFile()) {
                    LOGGER.log(Level.SEVERE, "Instrumented directory isn't a directory " + instrumentedDir.getName());
                    return false;
                } else {
                    LOGGER.log(Level.INFO,"Clearing directory " + instrumentedDir.getPath());
                    clearFolder(instrumentedDir);
                }
                LOGGER.log(Level.INFO, "Instrumenting " + inputSource.getName() + " to " + instrumentedDir.getPath());
                if (!instrument(inputSource, instrumentedDir.toPath())) {
                    return false;
                }
            } else {
                LOGGER.log(Level.WARNING, "Input Source doesn't exist continuing without instrumenting");
                if (!instrumentedDir.exists()) {
                    LOGGER.log(Level.SEVERE, "Instrumented directory doesn't exist " + instrumentedDir.getName());
                    return false;
                } else if (instrumentedDir.isFile()) {
                    LOGGER.log(Level.SEVERE, "Instrumented directory isn't a directory " + instrumentedDir.getName());
                    return false;
                }
            }
        } else {
            if (!instrumentedDir.exists()) {
                LOGGER.log(Level.SEVERE, "Instrumented directory doesn't exist " + instrumentedDir.getName());
                return false;
            } else if (instrumentedDir.isFile()) {
                LOGGER.log(Level.SEVERE, "Instrumented directory isn't a directory " + instrumentedDir.getName());
                return false;
            }
        }
        try {
            LOGGER.log(Level.INFO,"Adding to classpath " + instrumentedDir.getPath());
            addToClassPath(instrumentedDir);
        } catch (NoSuchMethodException | MalformedURLException | InvocationTargetException | IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "Error with adding instrumented directory to class path", e);
            return false;
        }
        return true;
    }
}