package ch.szclsb.rkb.cmake;

import org.apache.maven.plugins.annotations.Mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Mojo(name = "cmake")
public class CMakeMojo extends AbstractMojo {
    @Parameter(property = "outputDirectory", required = true, readonly = true)
    private File workingDirectory;
    @Parameter(property = "nativePath", defaultValue = "native")
    private String nativePath;
    @Parameter(property = "nativeBuildPath", defaultValue = "native-build")
    private String nativeBuildPath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Compiling native dll");
        try {
            var exitCode = runCommand("cmake", "-S", nativePath, "-B", nativeBuildPath, ".");
            if (exitCode != 0) {
                throw new MojoExecutionException("cmake make finished with exit code " + exitCode);
            }
            exitCode = runCommand("cmake", "--build", "./" + nativeBuildPath);
            if (exitCode != 0) {
                throw new MojoExecutionException("cmake build finished with exit code " + exitCode);
            }
            getLog().info("Finished building dll");
        } catch (Exception e) {
            throw new MojoExecutionException(e);
        }
    }

    private int runCommand(String ...command) throws IOException, InterruptedException {
        var builder = new ProcessBuilder();
        builder.command(command);
        builder.directory(workingDirectory);
        var process = builder.start();
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            reader.lines().forEach(line -> getLog().info(line));
        }
        try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            reader.lines().forEach(line -> getLog().error(line));
        }
        return process.waitFor();
    }
}
