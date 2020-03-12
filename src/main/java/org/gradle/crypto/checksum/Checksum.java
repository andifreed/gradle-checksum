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
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

import java.io.File;
import java.io.IOException;

public class Checksum extends ChecksumBase {
    private FileCollection files;
    private File outputDir;

    public Checksum() {
        outputDir = new File(getProject().getBuildDir(), "checksums");
    }

    @InputFiles
    public FileCollection getFiles() {
        return files;
    }

    public void setFiles(FileCollection files) {
        this.files = files;
    }

    @OutputDirectory
    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        if (outputDir.exists() && !outputDir.isDirectory()) {
            throw new IllegalArgumentException("Output directory must be a directory.");
        }
        this.outputDir = outputDir;
    }

    @TaskAction
    public void generateChecksumFiles(IncrementalTaskInputs inputs) throws IOException {
        final String checksumExt = "." + Checksum.this.getAlgorithm().toString().toLowerCase();
        if (!getOutputDir().exists()) {
            if (!getOutputDir().mkdirs()) {
                throw new IOException("Could not create directory:" + getOutputDir());
            }
        }
        if (!inputs.isIncremental()) {
            getProject().delete(allPossibleChecksumFiles(getOutputDir()));
        }

        inputs.outOfDate(new Action<InputFileDetails>() {
            @Override
            public void execute(InputFileDetails inputFileDetails) {
                File input = inputFileDetails.getFile();
                if (input.isDirectory()) {
                    return;
                }
                File sumFile = outputFileFor(input, checksumExt);
                try {
                    String hashCodeStr = generateHashCode(input);
                    Files.write(hashCodeStr.getBytes(), sumFile);
                } catch (IOException e) {
                    throw new GradleException("Trouble writing checksum", e);
                }
            }
        });

        inputs.removed(new Action<InputFileDetails>() {
            @Override
            public void execute(InputFileDetails inputFileDetails) {
                File input = inputFileDetails.getFile();
                if (input.isDirectory()) {
                    return;
                }
                getProject().delete(outputFileFor(input, checksumExt));
            }
        });
    }

    private File outputFileFor(File inputFile, String checksumExt) {
        return new File(getOutputDir(), inputFile.getName() + checksumExt);
    }
}
