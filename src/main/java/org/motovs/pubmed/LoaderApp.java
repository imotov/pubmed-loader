package org.motovs.pubmed;

import org.elasticsearch.common.cli.CliTool;
import org.elasticsearch.common.cli.CliToolConfig;
import org.elasticsearch.common.cli.commons.CommandLine;

import static org.elasticsearch.common.cli.CliToolConfig.Builder.cmd;
import static org.elasticsearch.common.cli.CliToolConfig.Builder.option;
import static org.elasticsearch.common.cli.CliToolConfig.config;

/**
 */
public class LoaderApp extends CliTool {


    public LoaderApp() {
        super(config("pmloader", LoaderApp.class)
                .cmds(cmd("load", LoaderCommand.class)
                        .options(
                                option("d", "data").hasArg(true).required(true),
                                option("i", "impact-factor").hasArg(true),
                                option("s", "skip").hasArg(true)
                        ))
                .build());
    }

    @Override
    protected Command parse(String s, CommandLine commandLine) throws Exception {
        return new LoaderCommand(terminal, commandLine);
    }

    public static void main(String[] args) {
        LoaderApp app = new LoaderApp();
        app.execute(args);
    }
}
