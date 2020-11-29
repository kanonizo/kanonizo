package org.kanonizo.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecursiveFileFinder
{
    private final File folderToSearch;
    private final String fileExtension;

    public RecursiveFileFinder(File folderToSearch, String fileExtension)
    {
        this.folderToSearch = folderToSearch;
        this.fileExtension = fileExtension;
    }

    public List<File> getAllFiles()
    {
        return getAllFiles(folderToSearch);
    }

    private List<File> getAllFiles(File folder)
    {
        List<File> classes = new ArrayList<>();
        if (folder.isDirectory())
        {
            File[] files = folder
                    .listFiles(file -> file.getName().endsWith(fileExtension) || file.isDirectory());
            for (File file : files)
            {
                if (file.isDirectory())
                {
                    classes.addAll(getAllFiles(file));
                }
                else
                {
                    classes.add(file);
                }
            }
        }
        else if (folder.isFile())
        {
            classes.add(folder);
        }
        /* the default behaviour of listFiles will return files as ordered according to File#compareTo,
         * which in turn delegates to String#compareTo. This returns internal classes before the class
         * that defines them, because (int) '.' < (int) '$' - to solve this we sort the files using our
         * own comparator */
        classes.sort((o1, o2) -> compareFileNames(o1.getPath(), o2.getPath()));
        return classes;
    }

    private int compareFileNames(String fileName1, String fileName2)
    {
        // strip the file extension to ensure we are only comparing the actual file name characters
        int length1 = fileName1.length() - fileExtension.length();
        int length2 = fileName2.length() - fileExtension.length();
        int minLength = Math.min(length1, length2);
        char[] chars1 = new char[length1];
        char[] chars2 = new char[length2];
        // grab chars from filename
        fileName1.getChars(0, length1, chars1, 0);
        fileName2.getChars(0, length2, chars2, 0);
        for (int i = 0; i < minLength; i++)
        {
            char one = chars1[i];
            char two = chars2[i];
            // alphabetically orders classes
            if (one != two)
            {
                return one - two;
            }
        }
        // the *shorter* fileName should be placed first
        return length1 - length2;
    }
}
