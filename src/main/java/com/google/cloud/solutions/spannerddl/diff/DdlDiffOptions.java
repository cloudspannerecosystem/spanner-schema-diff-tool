/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.solutions.spannerddl.diff;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/** Wrapper for command line options parsing and validation. */
@AutoValue
public abstract class DdlDiffOptions {

  public abstract Path originalDdlPath();

  public abstract Path newDdlPath();

  public abstract Path outputDdlPath();

  public abstract ImmutableMap<String, Boolean> args();

  @VisibleForTesting
  static Options buildOptions() {
    Options options = new Options();
    options.addOption(
        Option.builder()
            .longOpt(DdlDiff.ORIGINAL_DDL_FILE_OPT)
            .desc("File path to the original DDL definition.")
            .hasArg()
            .argName("FILE")
            .type(File.class)
            .required()
            .build());
    options.addOption(
        Option.builder()
            .longOpt(DdlDiff.NEW_DDL_FILE_OPT)
            .desc("File path to the new DDL definition.")
            .hasArg()
            .argName("FILE")
            .type(File.class)
            .required()
            .build());
    options.addOption(
        Option.builder()
            .longOpt(DdlDiff.OUTPUT_DDL_FILE_OPT)
            .desc("File path to the output DDL to write.")
            .hasArg()
            .argName("FILE")
            .required()
            .build());
    options.addOption(
        Option.builder()
            .longOpt(DdlDiff.ALLOW_RECREATE_INDEXES_OPT)
            .desc("Allows dropping and recreating secondary Indexes to apply changes.")
            .build());
    options.addOption(
        Option.builder()
            .longOpt(DdlDiff.ALLOW_RECREATE_CONSTRAINTS_OPT)
            .desc(
                "Allows dropping and recreating Check and Foreign Key constraints (and their "
                    + "backing Indexes) to apply changes.")
            .build());
    options.addOption(
        Option.builder()
            .longOpt(DdlDiff.ALLOW_DROP_STATEMENTS_OPT)
            .desc("Enables output of DROP commands to delete objects not used in the new DDL file.")
            .build());
    options.addOption(Option.builder().longOpt(DdlDiff.HELP_OPT).desc("Show help").build());
    return options;
  }

  static void printHelpAndExit(int exitStatus) {
    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)));
    new HelpFormatter()
        .printHelp(
            pw,
            132,
            "DdlDiff",
            "Compares original and new DDL files and creates a DDL file with DROP, CREATE and"
                + " ALTER statements which convert the original Schema to the new Schema.\n"
                + "\n"
                + "Incompatible table changes (table hierarchy changes. column type changes) are"
                + " not supported and will cause this tool to fail.\n"
                + "\n"
                + "To prevent accidental data loss, and to make it easier to apply DDL changes,"
                + " DROP statements are not generated for removed tables, columns, indexes and"
                + " change streams. This can be overridden using the "
                + DdlDiff.ALLOW_DROP_STATEMENTS_OPT
                + " command line argument.\n"
                + "\n"
                + "By default, changes to indexes will also cause a failure. The "
                + DdlDiff.ALLOW_RECREATE_INDEXES_OPT
                + " command line option enables index changes by"
                + " generating statements to drop and recreate the index.\n"
                + "\n"
                + "By default, changes to foreign key constraints will also cause a failure. The "
                + DdlDiff.ALLOW_RECREATE_CONSTRAINTS_OPT
                + " command line option enables foreign key changes by"
                + " generating statements to drop and recreate the constraint.\n"
                + "\n",
            buildOptions(),
            1,
            4,
            "",
            true);
    pw.flush();
    System.exit(exitStatus);
  }

  /** Parse the command line according to defined options. */
  public static DdlDiffOptions parseCommandLine(String[] args) {
    try {
      CommandLine commandLine = new DefaultParser().parse(buildOptions(), args);
      if (commandLine.hasOption(DdlDiff.HELP_OPT)) {
        printHelpAndExit(0);
      }

      Path originalDdlPath =
          new File(commandLine.getOptionValue(DdlDiff.ORIGINAL_DDL_FILE_OPT)).toPath();
      Path newDdlPath = new File(commandLine.getOptionValue(DdlDiff.NEW_DDL_FILE_OPT)).toPath();
      Path outputDdlPath =
          new File(commandLine.getOptionValue(DdlDiff.OUTPUT_DDL_FILE_OPT)).toPath();

      ImmutableMap<String, Boolean> argsMap =
          ImmutableMap.of(
              DdlDiff.ALLOW_RECREATE_INDEXES_OPT,
                  commandLine.hasOption(DdlDiff.ALLOW_RECREATE_INDEXES_OPT),
              DdlDiff.ALLOW_DROP_STATEMENTS_OPT,
                  commandLine.hasOption(DdlDiff.ALLOW_DROP_STATEMENTS_OPT),
              DdlDiff.ALLOW_RECREATE_CONSTRAINTS_OPT,
                  commandLine.hasOption(DdlDiff.ALLOW_RECREATE_CONSTRAINTS_OPT));

      return new AutoValue_DdlDiffOptions(originalDdlPath, newDdlPath, outputDdlPath, argsMap);
    } catch (InvalidPathException e) {
      System.err.println("Invalid file path: " + e.getInput() + "\n" + e.getReason());
      printHelpAndExit(0);
    } catch (ParseException e) {
      System.err.println("Failed parsing command line: " + e.getMessage());
      printHelpAndExit(0);
    }
    // unreachable
    return null;
  }
}
