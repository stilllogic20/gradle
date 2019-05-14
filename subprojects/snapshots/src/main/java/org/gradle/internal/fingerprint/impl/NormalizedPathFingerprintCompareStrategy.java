/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.internal.fingerprint.impl;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MultimapBuilder;
import org.gradle.internal.change.ChangeVisitor;
import org.gradle.internal.change.DefaultFileChange;
import org.gradle.internal.file.FileType;
import org.gradle.internal.fingerprint.FileSystemLocationFingerprint;
import org.gradle.internal.fingerprint.FingerprintCompareStrategy;
import org.gradle.internal.hash.Hasher;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Compares by normalized path (relative/name only) and file contents. Order does not matter.
 */
public class NormalizedPathFingerprintCompareStrategy extends AbstractFingerprintCompareStrategy {
    public static final FingerprintCompareStrategy INSTANCE = new NormalizedPathFingerprintCompareStrategy();

    private static final Comparator<Entry<FileSystemLocationFingerprint, ?>> ENTRY_COMPARATOR = new Comparator<Entry<FileSystemLocationFingerprint, ?>>() {
        @Override
        public int compare(Entry<FileSystemLocationFingerprint, ?> o1, Entry<FileSystemLocationFingerprint, ?> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    };

    private NormalizedPathFingerprintCompareStrategy() {
    }

    /**
     * Determines changes by:
     *
     * <ul>
     *     <li>Determining which {@link FileSystemLocationFingerprint}s are only in the previous or current fingerprint collection.</li>
     *     <li>
     *         For those only in the previous fingerprint collection it checks if some entry with the same normalized path is in the current collection.
     *         If it is, file is reported as modified, if not as removed.
     *     </li>
     *     <li>Finally, if {@code shouldIncludeAdded} is {@code true}, the remaining fingerprints which are only in the current collection are reported as added.</li>
     * </ul>
     */
    @Override
    protected boolean doVisitChangesSince(
        ChangeVisitor visitor,
        Map<String, FileSystemLocationFingerprint> currentFingerprints,
        Map<String, FileSystemLocationFingerprint> previousFingerprints,
        String propertyTitle,
        boolean shouldIncludeAdded
    ) {
        ListMultimap<FileSystemLocationFingerprint, FilePathWithType> missingPreviousFiles = getMissingPreviousFingerprints(previousFingerprints, currentFingerprints);
        List<Entry<FileSystemLocationFingerprint, FilePathWithType>> missingPreviousEntries = Lists.newArrayList(missingPreviousFiles.entries());
        Collections.sort(missingPreviousEntries, ENTRY_COMPARATOR);

        ListMultimap<String, FilePathWithType> addedFilesByNormalizedPath = getAddedFilesByNormalizedPath(currentFingerprints, missingPreviousFiles, previousFingerprints);

        for (Entry<FileSystemLocationFingerprint, FilePathWithType> entry : missingPreviousEntries) {
            FileSystemLocationFingerprint previousFingerprint = entry.getKey();
            String normalizedPath = previousFingerprint.getNormalizedPath();
            FileType previousFingerprintType = previousFingerprint.getType();
            FilePathWithType pathWithType = entry.getValue();

            if (wasModified(addedFilesByNormalizedPath, normalizedPath, pathWithType)) {
                if (wasModifiedAndMessageCountSaturated(visitor, propertyTitle, previousFingerprintType, normalizedPath, pathWithType)) {
                    return false; // TODO
                }
            } else if (wasRemovedAndMessageCountSaturated(visitor, propertyTitle, normalizedPath, pathWithType)) {
                return false; // TODO
            }
        }

        if (shouldIncludeAdded) {
            for (Entry<String, FilePathWithType> entry : addedFilesByNormalizedPath.entries()) {
                if (wasAddedAndMessageCountSaturated(visitor, propertyTitle, entry)) {
                    return false; // TODO
                }
            }
        }
        return true;
    }

    // There might be multiple files with the same normalized path, here we choose one of them
    private static boolean wasModified(ListMultimap<String, FilePathWithType> addedFilesByNormalizedPath, String normalizedPath, FilePathWithType pathWithType) {
        List<FilePathWithType> addedFilesForNormalizedPath = addedFilesByNormalizedPath.get(normalizedPath);
        return !addedFilesForNormalizedPath.isEmpty() && addedFilesForNormalizedPath.remove(0) != null;
    }

    private static ListMultimap<FileSystemLocationFingerprint, FilePathWithType> getMissingPreviousFingerprints(
        Map<String, FileSystemLocationFingerprint> previousFingerprints,
        Map<String, FileSystemLocationFingerprint> currentFingerprints
    ) {
        ListMultimap<FileSystemLocationFingerprint, FilePathWithType> results = MultimapBuilder
            .hashKeys(previousFingerprints.size())
            .linkedListValues()
            .build();
        for (Entry<String, FileSystemLocationFingerprint> entry : previousFingerprints.entrySet()) {
            String absolutePath = entry.getKey();
            FileSystemLocationFingerprint previousFingerprint = entry.getValue();
            FileType previousFingerprintType = previousFingerprint.getType();

            results.put(previousFingerprint, new FilePathWithType(absolutePath, previousFingerprintType));
        }
        return results;
    }

    private static ListMultimap<String, FilePathWithType> getAddedFilesByNormalizedPath(
        Map<String, FileSystemLocationFingerprint> currentFingerprints,
        ListMultimap<FileSystemLocationFingerprint, FilePathWithType> missingPreviousFiles,
        Map<String, FileSystemLocationFingerprint> previousFingerprints
    ) {
        ListMultimap<String, FilePathWithType> results = MultimapBuilder
            .linkedHashKeys()
            .linkedListValues()
            .build();
        for (Entry<String, FileSystemLocationFingerprint> entry : currentFingerprints.entrySet()) {
            String absolutePath = entry.getKey();
            FileSystemLocationFingerprint currentFingerprint = entry.getValue();
            List<FilePathWithType> previousFilesForFingerprint = missingPreviousFiles.get(currentFingerprint);
            FileType fingerprintType = currentFingerprint.getType();

            if (previousFilesForFingerprint.isEmpty()) {
                results.put(currentFingerprint.getNormalizedPath(), new FilePathWithType(absolutePath, fingerprintType));
            } else {
                previousFilesForFingerprint.remove(0);
            }
        }
        return results;
    }

    private static boolean wasModifiedAndMessageCountSaturated(
        ChangeVisitor visitor,
        String propertyTitle,
        FileType previousFingerprintType,
        String normalizedPath,
        FilePathWithType modifiedFile
    ) {
        String absolutePath = modifiedFile.getAbsolutePath();
        FileType fileType = modifiedFile.getFileType();
        DefaultFileChange modified = DefaultFileChange.modified(absolutePath, propertyTitle, previousFingerprintType, fileType, normalizedPath);
        return !visitor.visitChange(modified);
    }

    private static boolean wasRemovedAndMessageCountSaturated(
        ChangeVisitor visitor,
        String propertyTitle,
        String normalizedPath,
        FilePathWithType removedFile
    ) {
        String absolutePath = removedFile.getAbsolutePath();
        FileType fileType = removedFile.getFileType();
        DefaultFileChange removed = DefaultFileChange.removed(absolutePath, propertyTitle, fileType, normalizedPath);
        return !visitor.visitChange(removed);
    }

    private static boolean wasAddedAndMessageCountSaturated(
        ChangeVisitor visitor,
        String propertyTitle,
        Entry<String, FilePathWithType> addedFilesByNormalizedPathEntries
    ) {
        FilePathWithType addedFile = addedFilesByNormalizedPathEntries.getValue();
        String absolutePath = addedFile.getAbsolutePath();
        FileType fileType = addedFile.getFileType();
        String normalizedPath = addedFilesByNormalizedPathEntries.getKey();
        DefaultFileChange added = DefaultFileChange.added(absolutePath, propertyTitle, fileType, normalizedPath);
        return !visitor.visitChange(added);
    }

    @Override
    public void appendToHasher(Hasher hasher, Collection<FileSystemLocationFingerprint> fingerprints) {
        appendSortedToHasher(hasher, fingerprints);
    }

    public static void appendSortedToHasher(Hasher hasher, Collection<FileSystemLocationFingerprint> fingerprints) {
        List<FileSystemLocationFingerprint> sortedFingerprints = Lists.newArrayList(fingerprints);
        Collections.sort(sortedFingerprints);
        for (FileSystemLocationFingerprint normalizedSnapshot : sortedFingerprints) {
            normalizedSnapshot.appendToHasher(hasher);
        }
    }
}
