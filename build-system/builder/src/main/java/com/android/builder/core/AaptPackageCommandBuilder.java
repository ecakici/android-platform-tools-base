/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.builder.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.dependency.SymbolFileProvider;
import com.android.builder.model.AaptOptions;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.utils.ILogger;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builds the command line necessary for an aapt package invocation
 */
public class AaptPackageCommandBuilder {

    @NonNull private final File mManifestFile;
    @NonNull private final AaptOptions mOptions;
    @Nullable private File mResFolder;
    @Nullable private File mAssetsFolder;
    private boolean mVerboseExec = false;
    @Nullable private String mSourceOutputDir;
    @Nullable private String mSymbolOutputDir;
    @Nullable private List<? extends SymbolFileProvider> mLibraries;
    @Nullable private String mResPackageOutput;
    @Nullable private String mProguardOutput;
    @Nullable private VariantConfiguration.Type mType;
    private boolean mDebuggable = false;
    private boolean mPseudoLocalesEnabled = false;
    @Nullable private Collection<String> mResourceConfigs;
    @Nullable Collection<String> mSplits;
    @Nullable String mPackageForR;

    /**
     *
     * @param manifestFile the location of the manifest file
     * @param options the {@link com.android.builder.model.AaptOptions}
     */
    public AaptPackageCommandBuilder(
            @NonNull File manifestFile,
            @NonNull AaptOptions options) {
        checkNotNull(manifestFile, "manifestFile cannot be null.");
        checkNotNull(options, "options cannot be null.");
        mManifestFile = manifestFile;
        mOptions = options;
    }

    public File getManifestFile() {
        return mManifestFile;
    }

    /**
     * @param resFolder the merged res folder
     * @return itself
     */
    public AaptPackageCommandBuilder setResFolder(@NonNull File resFolder) {
        if (!resFolder.isDirectory()) {
            throw new RuntimeException("resFolder parameter is not a directory");
        }
        mResFolder = resFolder;
        return this;
    }

    /**
     * @param assetsFolder the merged asset folder
     * @return itself
     */
    public AaptPackageCommandBuilder setAssetsFolder(@NonNull File assetsFolder) {
        if (!assetsFolder.isDirectory()) {
            throw new RuntimeException("assetsFolder parameter is not a directory");
        }
        mAssetsFolder = assetsFolder;
        return this;
    }

    /**
     * @param sourceOutputDir optional source folder to generate R.java
     * @return itself
     */
    public AaptPackageCommandBuilder setSourceOutputDir(@Nullable String sourceOutputDir) {
        mSourceOutputDir = sourceOutputDir;
        return this;
    }

    @Nullable
    public String getSourceOutputDir() {
        return mSourceOutputDir;
    }

    /**
     * @param symbolOutputDir the folder to write symbols into
     * @ itself
     */
    public AaptPackageCommandBuilder setSymbolOutputDir(@Nullable String symbolOutputDir) {
        mSymbolOutputDir = symbolOutputDir;
        return this;
    }

    @Nullable
    public String getSymbolOutputDir() {
        return mSymbolOutputDir;
    }

    /**
     * @param libraries the flat list of libraries
     * @return itself
     */
    public AaptPackageCommandBuilder setLibraries(
            @NonNull List<? extends SymbolFileProvider> libraries) {
        mLibraries = libraries;
        return this;
    }

    @NonNull
    public List<? extends SymbolFileProvider> getLibraries() {
        return mLibraries == null ? ImmutableList.<SymbolFileProvider>of() : mLibraries;
    }

    /**
     * @param resPackageOutput optional filepath for packaged resources
     * @return itself
     */
    public AaptPackageCommandBuilder setResPackageOutput(@Nullable String resPackageOutput) {
        mResPackageOutput = resPackageOutput;
        return this;
    }

    /**
     * @param proguardOutput optional filepath for proguard file to generate
     * @return itself
     */
    public AaptPackageCommandBuilder setProguardOutput(@Nullable String proguardOutput) {
        mProguardOutput = proguardOutput;
        return this;
    }

    /**
     * @param type the type of the variant being built
     * @return itself
     */
    public AaptPackageCommandBuilder setType(@NonNull VariantConfiguration.Type type) {
        this.mType = type;
        return this;
    }

    @Nullable
    public VariantConfiguration.Type getType() {
        return mType;
    }

    /**
     * @param debuggable whether the app is debuggable
     * @return itself
     */
    public AaptPackageCommandBuilder setDebuggable(boolean debuggable) {
        this.mDebuggable = debuggable;
        return this;
    }

    /**
     * @param resourceConfigs a list of resource config filters to pass to aapt.
     * @return itself
     */
    public AaptPackageCommandBuilder setResourceConfigs(@NonNull Collection<String> resourceConfigs) {
        this.mResourceConfigs = resourceConfigs;
        return this;
    }

    /**
     * @param splits optional list of split dimensions values (like a density or an abi). This
     *               will be used by aapt to generate the corresponding pure split apks.
     * @return itself
     */
    public AaptPackageCommandBuilder setSplits(@NonNull Collection<String> splits) {
        this.mSplits = splits;
        return this;
    }


    public AaptPackageCommandBuilder setVerbose() {
        mVerboseExec = true;
        return this;
    }

    /**
     * @param packageForR Package override to generate the R class in a different package.
     * @return itself
     */
    public AaptPackageCommandBuilder setPackageForR(@NonNull String packageForR) {
        this.mPackageForR = packageForR;
        return this;
    }

    public AaptPackageCommandBuilder setPseudoLocalesEnabled(boolean pseudoLocalesEnabled) {
        mPseudoLocalesEnabled = pseudoLocalesEnabled;
        return this;
    }

    @Nullable
    String getPackageForR() {
        return mPackageForR;
    }

    public List<String> build(@NonNull BuildToolInfo buildToolInfo, @NonNull IAndroidTarget target, @NonNull ILogger logger) {

        // if both output types are empty, then there's nothing to do and this is an error
        checkArgument(mSourceOutputDir != null || mResPackageOutput != null,
                "No output provided for aapt task");
        if (mSymbolOutputDir != null || mSourceOutputDir != null) {
            checkNotNull(mLibraries, "libraries cannot be null if symbolOutputDir or sourceOutputDir is non-null");
        }

        // launch aapt: create the command line
        ArrayList<String> command = Lists.newArrayList();

        String aapt = buildToolInfo.getPath(BuildToolInfo.PathId.AAPT);
        if (aapt == null || !new File(aapt).isFile()) {
            throw new IllegalStateException("aapt is missing");
        }

        command.add(aapt);
        command.add("package");

        if (mVerboseExec) {
            command.add("-v");
        }

        command.add("-f");
        command.add("--no-crunch");

        // inputs
        command.add("-I");
        command.add(target.getPath(IAndroidTarget.ANDROID_JAR));

        command.add("-M");
        command.add(mManifestFile.getAbsolutePath());

        if (mResFolder != null) {
            command.add("-S");
            command.add(mResFolder.getAbsolutePath());
        }

        if (mAssetsFolder != null) {
            command.add("-A");
            command.add(mAssetsFolder.getAbsolutePath());
        }

        // outputs

        if (mSourceOutputDir != null) {
            command.add("-m");
            command.add("-J");
            command.add(mSourceOutputDir);
        }

        if (mResPackageOutput != null) {
            command.add("-F");
            command.add(mResPackageOutput);
        }

        if (mProguardOutput != null) {
            command.add("-G");
            command.add(mProguardOutput);
        }

        if (mSplits != null) {
            for (String split : mSplits) {

                command.add("--split");
                command.add(split);
            }
        }

        // options controlled by build variants

        if (mDebuggable) {
            command.add("--debug-mode");
        }


        if (mType != VariantConfiguration.Type.TEST) {
            if (mPackageForR != null) {
                command.add("--custom-package");
                command.add(mPackageForR);
                logger.verbose("Custom package for R class: '%s'", mPackageForR);
            }
        }

        if (mPseudoLocalesEnabled) {
            if (buildToolInfo.getRevision().getMajor() >= 21) {
                command.add("--pseudo-localize");
            } else {
                throw new RuntimeException(
                        "Pseudolocalization is only available since Build Tools version 21.0.0,"
                                + " please upgrade or turn it off.");
            }
        }

        // library specific options
        if (mType == VariantConfiguration.Type.LIBRARY) {
            command.add("--non-constant-id");
        }

        // AAPT options
        String ignoreAssets = mOptions.getIgnoreAssets();
        if (ignoreAssets != null) {
            command.add("--ignore-assets");
            command.add(ignoreAssets);
        }

        if (mOptions.getFailOnMissingConfigEntry()) {
            if (buildToolInfo.getRevision().getMajor() > 20) {
                command.add("--error-on-missing-config-entry");
            } else {
                throw new IllegalStateException("aaptOptions:failOnMissingConfigEntry cannot be used"
                        + " with SDK Build Tools revision earlier than 21.0.0");
            }
        }

        // never compress apks.
        command.add("-0");
        command.add("apk");

        // add custom no-compress extensions
        Collection<String> noCompressList = mOptions.getNoCompress();
        if (noCompressList != null) {
            for (String noCompress : noCompressList) {
                command.add("-0");
                command.add(noCompress);
            }
        }

        if (mResourceConfigs!=null && !mResourceConfigs.isEmpty()) {
            command.add("-c");

            Joiner joiner = Joiner.on(',');
            command.add(joiner.join(mResourceConfigs));
        }

        if (mSymbolOutputDir != null &&
                (mType == VariantConfiguration.Type.LIBRARY || !mLibraries.isEmpty())) {
            command.add("--output-text-symbols");
            command.add(mSymbolOutputDir);
        }
        return command;
    }

}