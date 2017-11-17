import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.List;


/**
 * Created by zangetsu on 1/31/17.
 */
public class Cloner {

    private static final Logger log = LogManager.getLogger(Cloner.class);

    String BIT_BUCKET_SERVER = "http://psdgit.sicpa-net.ads";
    String REST_API_SUFFIX ="/rest/api/1.0";
    String API_PROJECTS = "/projects";
    String API_REPOSITORIES ="/repos";
    String DEFAULT_DIR = "/tmp/clone";

    String USER = "svcPsdGit";
    String PASSWORD = "6MmAie2015";

    public static JSONArray projects = null;
    public static JSONArray repositories = null;
    private List projectLists = Arrays.asList("DCMBM","AIR2","POTDS","PUIG","SM","GITSNDBX","POTMYSO","PMYSOW","ESWORK","PUIGIP","NIP","PTI","CW","CS","MBDRET","MBDENG","POTDA","TECHSVC","SMPL","VDA","INTIJ","GZ","CIVB","IOSPODS","CC","CSRV","NPPOC","PTIDT","PP","GZINSP","GZINSPQC","PTT","ASA","WATRIB","EDEN","WCC","WDT","GZTT","GZINV","METIS","LCSAT","TRTLP","ISSDEMO","ISS","IPS","GZITEM","PTF","INUS","DEMOSSAVP","HD20H","EPRDEMO","CDO","NM","CCUSTO","POTMD","SVCE","DQ","AQ","BASEI","SSAVP","NESCHI","SCOR","POTDAV","PHBR","SESPE","EXTENS","GT","POTT","NS","POTDOV","POTDO","POTL","OC","POTPDS","POTDAS","POTPRU","POTPFER","S11N","POTPCC","TECS","CORE_PLC","POTDSR");

    private void obtainProjects() {
        log.info("Going to obtain projects from: " + BIT_BUCKET_SERVER +REST_API_SUFFIX + API_PROJECTS);

        try {
            projects = Unirest.get(BIT_BUCKET_SERVER +REST_API_SUFFIX + API_PROJECTS + "?limit=1000")
                    .basicAuth(USER, PASSWORD)
                    .header("accept", "application/json")
                    .asJson()
                    .getBody()
                    .getObject().getJSONArray("values");
        } catch (UnirestException e) {
            log.error("Cannot obtain list of projects from " + BIT_BUCKET_SERVER +REST_API_SUFFIX + API_PROJECTS);
        }
    }

    private void obtainRepositories(String projectKey) {
        String projectReposURL = BIT_BUCKET_SERVER +REST_API_SUFFIX + API_PROJECTS + "/" + projectKey + API_REPOSITORIES;
        //log.info("Obtaining repos from " + projectReposURL);
        try {
            repositories = Unirest.get(projectReposURL)
                    .basicAuth(USER, PASSWORD)
                    .header("accept", "application/json")
                    .asJson()
                    .getBody()
                    .getObject().getJSONArray("values");
        } catch (UnirestException e) {
            log.error("Cannot obtain project repositories from " + projectReposURL);
        }
        //log.debug("Repositories obtained: " + repositories);

    }
    private void cloneRepository(String gitURL,String repoDir){
        log.info("Going to clone repo " + gitURL);
        log.info("Repository will be stored at " + repoDir);
        try {
            Git git = Git.cloneRepository()
               .setURI(gitURL)
               .setDirectory(new File(repoDir))
               .setCloneAllBranches(true)
               .setCredentialsProvider(new UsernamePasswordCredentialsProvider(USER, PASSWORD))
               .call();

        } catch (GitAPIException e) {
            log.error("Error on cloning repository from " + gitURL + " into local directory " + repoDir + ". Check the path.");
        }
    }
    public void cloner(){
        // Obtain projects
        obtainProjects();
        if (projects.length()<1){
            log.info("There are no projects available to process.");
        } else{
            for (int i = 0; i < projects.length();i++) {
                JSONObject project = (JSONObject) projects.get(i);
                //log.debug("Project data: " + project);
                String projectName = project.getString("name").toLowerCase().replace(" ","_");
                log.debug("Project name: " + projectName);
                String projectKey = project.get("key").toString();
                if (!projectLists.contains(projectKey)) {
                    continue;
                }
                log.info("Project key: " + projectKey);
                //System.out.println("\"" + projectKey + "\"," + project.get("name") + ".");


                // Obtain repos
                obtainRepositories(projectKey);
                for (int k = 0; k < repositories.length();k++){
                    JSONObject repository = (JSONObject) repositories.get(k);
                    //log.debug("DEBUG Repository " + repository);
                    String repoName = repository.getString("name");
                    log.debug("Repository name: " + repoName);
                    String repoDir = DEFAULT_DIR + "/" + projectName + "/" + repoName;
                    log.debug("Repository local directory where clone to: " + repoDir);
                    final JSONArray cloneURLs = (JSONArray) ((JSONObject) repository.get("links")).get("clone");
                    for (int r = 0; r < cloneURLs.length();r++){
                        if (((JSONObject)cloneURLs.get(r)).get("name").toString().equals("http")){
                         log.debug("HTTP repository link for clone found.");
                         String repoURL = ((JSONObject) cloneURLs.get(r)).getString("href");
                         //cloneRepository(repoURL,repoDir);
                        } else{
                            log.debug(((JSONObject)cloneURLs.get(r)).get("name").toString());
                        }

                    }

                }
            }
        }

    }

    public static void main(String[] args) throws UnirestException {
        Cloner c = new Cloner();
        c.cloner();
    }
}
