package org.motovs.pubmed;

import org.elasticsearch.common.cli.CliTool;
import org.elasticsearch.common.cli.Terminal;
import org.elasticsearch.common.cli.commons.CommandLine;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;

import java.io.File;
import java.io.FileNotFoundException;

/**
 */
public class LoaderCommand extends CliTool.Command {

    final File impactFactorFile;

    final File pubmedFilesDir;

    final int skipCount;

    protected LoaderCommand(Terminal terminal, CommandLine commandLine) {
        super(terminal);
        if (commandLine.hasOption('i')) {
            impactFactorFile = new File(commandLine.getOptionValue("i", "Journal_ISI.csv"));
        } else {
            impactFactorFile = null;
        }
        pubmedFilesDir = new File(commandLine.getOptionValue("d", "pubmedfiles"));
        skipCount = Integer.parseInt(commandLine.getOptionValue("b", "0"));
    }

    @Override
    public CliTool.ExitStatus execute(Settings settings, Environment env) throws Exception {
        if (impactFactorFile != null && !impactFactorFile.exists()) {
            throw new FileNotFoundException("Impact factor " + impactFactorFile.getAbsolutePath() + " doesn't exist");
        }
        if (!pubmedFilesDir.exists() || !pubmedFilesDir.isDirectory()) {
            throw new FileNotFoundException("Directory " + pubmedFilesDir.getAbsolutePath() + " doesn't exist");
        }
        MedlineLoader loader = new MedlineLoader(impactFactorFile, pubmedFilesDir, settings);
        loader.process(skipCount);
        return CliTool.ExitStatus.OK;
    }

}
