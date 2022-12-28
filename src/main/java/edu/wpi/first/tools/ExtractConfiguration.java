package edu.wpi.first.tools;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.ArtifactView.ViewConfiguration;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.os.OperatingSystem;

public class ExtractConfiguration extends DefaultTask {
    private final ArtifactViewAction viewAction = new ArtifactViewAction();

    private class AttributeContainerAction implements Action<AttributeContainer> {

        @Override
        public void execute(AttributeContainer attribute) {
            attribute.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.DIRECTORY_TYPE);
        }

    }

    private class ArtifactViewAction implements Action<ViewConfiguration> {
        private final AttributeContainerAction containerAction = new AttributeContainerAction();

        @Override
        public void execute(ViewConfiguration view) {
            view.attributes(containerAction);
        }
    }

    private final DirectoryProperty outputDirectory;
    private final RegularFileProperty versionsFile;

    private final ConfigurableFileCollection configurations;
    private final List<ArtifactView> views;

    @OutputDirectory
    public DirectoryProperty getOutputDirectory() {
        return outputDirectory;
    }

    private final Property<Boolean> skipWindowsHelperLibrary;

    @Input
    public Property<Boolean> getSkipWindowsHelperLibrary() {
        return skipWindowsHelperLibrary;
    }

    @InputFiles
    public ConfigurableFileCollection getConfigurationFiles() {
        return configurations;
    }

    @Internal
    public List<ArtifactView> getViews() {
        return views;
    }

    @OutputFile
    public RegularFileProperty getVersionsFile() {
        return versionsFile;
    }

    private TaskProvider<Task> extractTask;

    @Inject
    public ExtractConfiguration() {
        outputDirectory = getProject().getObjects().directoryProperty();

        skipWindowsHelperLibrary = getProject().getObjects().property(Boolean.class);

        skipWindowsHelperLibrary.set(false);

        versionsFile = getProject().getObjects().fileProperty();

        configurations = getProject().getObjects().fileCollection();

        views = new ArrayList<>();

        if (OperatingSystem.current().isWindows()) {
            TaskProvider<Task> extractTask = getProject().getRootProject().getTasks()
                    .named("extractEmbeddedWindowsHelpers");

            dependsOn(extractTask);
            this.extractTask = extractTask;
        }

    }

    public void addConfigurationToView(String configurationName) {
        var cfgs = getProject().getConfigurations();
        var view = cfgs.getByName(configurationName).getIncoming().artifactView(viewAction);
        getViews().add(view);
        Callable<FileCollection> cbl = () -> view.getFiles();
        getConfigurationFiles().from(getProject().files(cbl));
    }

    @TaskAction
    public void execute() throws IOException {

        getProject().copy(spec -> {
            spec.into(outputDirectory);
            spec.from(configurations);

            spec.include("**/*.so");
            spec.include("**/*.so.*");
            spec.include("**/*.dll");
            spec.include("**/*.dylib");

            spec.exclude("**/*.so.debug");
        });

        if (OperatingSystem.current().isWindows() && !getSkipWindowsHelperLibrary().getOrElse(false)) {

            ExtractEmbeddedWindowsHelpers resolvedExtractTask = (ExtractEmbeddedWindowsHelpers) extractTask.get();
            getProject().copy(spec -> {
                spec.from(resolvedExtractTask.getOutputFile(), copy -> {
                    String arch = "x86-64";
                    if (ExtractEmbeddedWindowsHelpers.is32BitIntel()) {
                        arch = "x86";
                    }
                    copy.into("/windows/" + arch + "/shared");
                });
                spec.into(outputDirectory);
            });
        }

        var versionFile = versionsFile.get().getAsFile();
        List<String> versions = new ArrayList<>();

        for (var view : views) {
            var artifacts = view.getArtifacts();
            for (var artifact : artifacts) {
                versions.add(artifact.toString());
            }
        }

        Files.write(versionFile.toPath(), versions, Charset.defaultCharset(), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }
}
