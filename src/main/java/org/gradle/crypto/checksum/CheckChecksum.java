/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.crypto.checksum;

import com.google.common.io.Files;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class CheckChecksum extends ChecksumBase {
    private File checksumDir;
    private File rootDir;
    private boolean fatal;

    public CheckChecksum() {
        checksumDir = new File(getProject().getBuildDir(), "checksums");
        rootDir = getProject().getProjectDir();
        fatal = true;
    }

    @OutputDirectory
    public File getChecksumDir() {
        return checksumDir;
    }

    public void setChecksumDir(File value) {
        if (!value.exists() || !value.isDirectory()) {
            throw new IllegalArgumentException("Checksum directory" + value + " must be an existing directory.");
        }
        checksumDir = value;
    }

    @InputFile
    public File getRootDir() {
        return rootDir;
    }

    public void setRootDir(File value) {
        if (!value.exists() || !value.isDirectory()) {
            throw new IllegalArgumentException("Root directory " + value + " must be an existing directory.");
        }
        rootDir = value;
    }

    @TaskAction
    public void checkChecksumFiles() {
        int checksumExtLen = getAlgorithm().toString().length() + 1;
        List<String> errors = new ArrayList<>();
        for (File checksumFile : allPossibleChecksumFiles(checksumDir)) {
            File inputFile = inputFileFor(checksumFile, checksumExtLen);
            try {
                if (!inputFile.exists()) {
                    errors.add(inputFile + ": no longer exists");
                } else {
                    String prevChecksum = Files.toString(checksumFile, Charset.defaultCharset()).trim();
                    String currCheckSum = generateHashCode(inputFile);
                    if (!prevChecksum.equals(currCheckSum.trim())) {
                        errors.add(inputFile + ": has changed");
                    }
                }
            } catch (IOException e) {
                errors.add(inputFile + ": error " + e.toString());
            }
        }
        if (!errors.isEmpty()) {
            getProject().getLogger().warn(errors.toString());
            if (fatal) {
                throw new GradleException(errors.toString());
            }
        }
    }

    private File inputFileFor(File checksumFile, int extLen) {
        File parent = getRootDir().toPath().resolve(getChecksumDir().toPath().relativize(checksumFile.toPath().getParent())).toFile();
        String name = checksumFile.getName();
        return new File(parent, name.substring(0, name.length() - extLen));
    }
}
