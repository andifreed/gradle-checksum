package org.gradle.crypto.checksum;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;

import java.io.File;
import java.io.IOException;

public class ChecksumBase extends DefaultTask {

  private Algorithm algorithm;

  public enum Algorithm {
    MD5(Hashing.md5()),
    SHA256(Hashing.sha256()),
    SHA384(Hashing.sha384()),
    SHA512(Hashing.sha512());

    private final HashFunction hashFunction;

    Algorithm(HashFunction hashFunction) {
      this.hashFunction = hashFunction;
    }
  }

  public ChecksumBase() {
    algorithm = Algorithm.SHA256;
  }

  @Input
  public Algorithm getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(Algorithm algorithm) {
    this.algorithm = algorithm;
  }

  protected FileCollection allPossibleChecksumFiles(File checksumDir) {
    FileCollection possibleFiles = null;
    for (Algorithm algo : Algorithm.values()) {
      if (possibleFiles == null) {
        possibleFiles = filesFor(algo, checksumDir);
      } else {
        possibleFiles = possibleFiles.plus(filesFor(algo, checksumDir));
      }
    }
    return possibleFiles;
  }

  protected FileCollection filesFor(final Algorithm algo, File checksumDir) {
    return getProject().fileTree(checksumDir, new Action<ConfigurableFileTree>() {
      @Override
      public void execute(ConfigurableFileTree files) {
        files.include("**/*." + algo.toString().toLowerCase());
      }
    });
  }

  protected String generateHashCode(File input) {
    try {
      HashCode hashCode = Files.asByteSource(input).hash(algorithm.hashFunction);
      return hashCode.toString();
    } catch (IOException e) {
      throw new GradleException("Trouble creating checksum for " + input, e);
    }
  }
}


