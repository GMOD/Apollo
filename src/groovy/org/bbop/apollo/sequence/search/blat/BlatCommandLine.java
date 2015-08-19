package org.bbop.apollo.sequence.search.blat;

import org.bbop.apollo.sequence.search.AlignmentParsingException;
import org.bbop.apollo.sequence.search.SequenceSearchTool;
import org.bbop.apollo.sequence.search.SequenceSearchToolException;
import org.bbop.apollo.sequence.search.blast.BlastAlignment;
import org.bbop.apollo.sequence.search.blast.TabDelimittedAlignment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.codehaus.groovy.grails.web.json.JSONObject;

public class BlatCommandLine extends SequenceSearchTool {

    private String blatBin;
    private String database;
    private String blatUserOptions;
    private String tmpDir;
    private boolean removeTmpDir;
    protected String [] blatOptions;
    
    @Override
    public void parseConfiguration(JSONObject config) throws SequenceSearchToolException {
        try {
            if(config.has("search_exe")) { blatBin = config.getString("search_exe"); }
            else { throw new SequenceSearchToolException("No blat exe specified"); }
            if(config.has("database")&&config.getString("database")!="") {database = config.getString("database"); }
            else { throw new SequenceSearchToolException("No database configured"); }
            if(config.has("params")) {blatUserOptions = config.getString("params");}
            else { /* no extra params needed */ }
            if(config.has("removeTmpDir")) {removeTmpDir=config.getBoolean("removeTmpDir"); }
            else { removeTmpDir=true; }
            if(config.has("tmp_dir")) {tmpDir=config.getString("tmp_dir"); }
        } catch (Exception e) {
            throw new SequenceSearchToolException("Error parsing configuration: " + e.getMessage(), e);
        }
    }


    @Override
    public Collection<BlastAlignment> search(String uniqueToken, String query, String databaseId) throws SequenceSearchToolException {
        File dir = null;
        Path p = null;
        try {
            if(tmpDir==null) {
                p = Files.createTempDirectory("blat_tmp");
            }
            else {
                p = Files.createTempDirectory(new File(tmpDir).toPath(),"blat_tmp");
            }
            dir = p.toFile();

            return runSearch(dir, query, databaseId);
        }
        catch (IOException e) {
            throw new SequenceSearchToolException("Error running search: " + e.getMessage(), e);
        }
        catch (AlignmentParsingException e) {
            throw new SequenceSearchToolException("Alignment parsing error: " + e.getMessage(), e);
        }
        catch (InterruptedException e) {
            throw new SequenceSearchToolException("Error running search: " + e.getMessage(), e);
        }
        finally {
            if (removeTmpDir && dir!=null) {
                deleteTmpDir(dir);
            }
        }
    }
    
    private Collection<BlastAlignment> runSearch(File dir, String query, String databaseId)
            throws IOException, AlignmentParsingException, InterruptedException {
        PrintWriter log = new PrintWriter(new BufferedWriter(new FileWriter(dir + "/search.log")));
        String queryArg = createQueryFasta(dir, query);
        String databaseArg = database + (databaseId != null ? ":" + databaseId : "");
        String outputArg = dir.getAbsolutePath() + "/results.tab";
        List<String> commands = new ArrayList<String>();
        commands.add(blatBin);
        if (blatOptions != null) {
            for (String option : blatOptions) {
                commands.add(option);
            }
        }
        commands.add(databaseArg);
        commands.add(queryArg);
        commands.add(outputArg);
        commands.add("-out=blast8");
        if (blatUserOptions != null && blatUserOptions.length() > 0) {
            for (String option : blatUserOptions.split("\\s+")) {
                commands.add(option);
            }
        }
        runCommand(commands,log);

        Collection<BlastAlignment> matches = new ArrayList<BlastAlignment>();
        BufferedReader in = new BufferedReader(new FileReader(outputArg));
        String line;
        while ((line = in.readLine()) != null) {
            matches.add(new TabDelimittedAlignment(line));
        }
        in.close();
        return matches;
    }


    private void runCommand(List<String> commands, PrintWriter log) throws IOException, InterruptedException {
        log.println("Command:");
        for (String arg : commands) {
            log.print(arg + " ");
        }
        ProcessBuilder pb = new ProcessBuilder(commands);
        Process p = pb.start();
        p.waitFor();
        String line;
        BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
        log.println("stdout:");
        while ((line = stdout.readLine()) != null) {
            log.println(line);
        }
        log.println();
        BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        log.println("stderr:");
        while ((line = stderr.readLine()) != null) {
            log.println(line);
        }
        log.close();
        p.destroy();
    }

    private void deleteTmpDir(File dir) {
        if (!dir.exists()) {
            return;
        }
        for (File f : dir.listFiles()) {
            f.delete();
        }
        dir.delete();
    }


    private String createQueryFasta(File dir, String query) throws IOException {
        String queryFileName = dir.getAbsolutePath() + "/query.fa";
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(queryFileName)));
        out.println(">query");
        out.println(query);
        out.close();
        return queryFileName;
    }
    
}
