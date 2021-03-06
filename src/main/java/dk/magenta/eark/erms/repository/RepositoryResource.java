package dk.magenta.eark.erms.repository;

import dk.magenta.eark.erms.Constants;
import dk.magenta.eark.erms.exceptions.ErmsRuntimeException;
import dk.magenta.eark.erms.json.JsonUtils;
import dk.magenta.eark.erms.repository.profiles.Profile;
import dk.magenta.eark.erms.xml.XmlHandler;
import dk.magenta.eark.erms.xml.XmlHandlerImpl;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;


/**
 * @author lanre.
 */


@Path("repository")
public class RepositoryResource {
    private final Logger logger = LoggerFactory.getLogger(RepositoryResource.class);
    private static Map<String, CmisSessionWorker> connectionPool = new HashMap<>();

    public static final String FOLDER_OBJECT_ID = "folderObjectId";
    public static final String DOCUMENT_OBJECT_ID = "documentObjectId";
    public static final String MAP_NAME = "mapName";
    public static final String SELECTED = "selected";

    public RepositoryResource() {}

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("connect")
    public JsonObject connect(JsonObject json) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        JsonObject response;
        if (json.containsKey(Profile.NAME) && json.containsKey(MAP_NAME) ) {
            String profileName = json.getString(Profile.NAME);
            String mapName = json.getString(MAP_NAME);

            try {
                //Get a session worker
                CmisSessionWorker sessionWorker = this.getSessionWorker(profileName, mapName);

                //Build the json for the repository info
                response = sessionWorker.getRepositoryInfo();
                builder.add("repositoryInfo", response);
                builder.add("rootFolder", sessionWorker.getRootFolder());

            } catch (Exception e) {
                builder.add(Constants.SUCCESS, false);
                builder.add(Constants.ERRORMSG, e.getMessage());
            }

            builder.add(Constants.SUCCESS, true);

        } else {
            builder.add(Constants.SUCCESS, false);
            builder.add(Constants.ERRORMSG, "The connection profile does not have a name!");
        }

        return builder.build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("getDocument")
    public JsonObject Document(JsonObject json) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (json.containsKey(DOCUMENT_OBJECT_ID) && json.containsKey(Profile.NAME) && json.containsKey(MAP_NAME)) {
            String profileName = json.getString(Profile.NAME);
            String documentObjectId = json.getString(DOCUMENT_OBJECT_ID);
            String mapName = json.getString(MAP_NAME);
            boolean includeContentStream = json.getBoolean("includeContentStream", false);

            try {
                //Get a session worker
                CmisSessionWorker sessionWorker = this.getSessionWorker(profileName, mapName);

                //Build the json for the repository info
                builder.add("document", sessionWorker.getDocument(documentObjectId, includeContentStream));

            } catch (Exception e) {
                builder.add(Constants.SUCCESS, false);
                builder.add(Constants.ERRORMSG, e.getMessage());
            }

            builder.add(Constants.SUCCESS, true);

        } else {
            builder.add(Constants.SUCCESS, false);
            builder.add(Constants.ERRORMSG, "The connection profile does not have a name!");
        }

        return builder.build();
    }

    /**
     * Just returns a folder object
     * @param json
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("getFolder")
    public JsonObject getFolder(JsonObject json) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (json.containsKey(FOLDER_OBJECT_ID) && json.containsKey(Profile.NAME) && json.containsKey(MAP_NAME)) {

            String profileName = json.getString(Profile.NAME);
            String folderObjectId = json.getString(FOLDER_OBJECT_ID);
            String mapName = json.getString(MAP_NAME);

            try {
                CmisSessionWorker cmisSessionWorker = this.getSessionWorker(profileName, mapName);

                //Build the json for the repository info
                builder.add("folder", cmisSessionWorker.getFolder(folderObjectId));

            } catch (Exception e) {
                builder.add(Constants.SUCCESS, false);
                builder.add(Constants.ERRORMSG, e.getMessage());
            }

            builder.add(Constants.SUCCESS, true);

        } else {
            builder.add(Constants.SUCCESS, false);
            builder.add(Constants.ERRORMSG, "The connection profile does not have a name!");
        }

        if (json.containsKey(SELECTED) )
            builder.add("selected", json.getBoolean("selected"));

        return builder.build();
    }

    /**
     *
     * @param profileName
     * @param objectId
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("isroot/{objectId}/in/{profileName}")
    public JsonObject isROOT(@PathParam("objectId") String objectId, @PathParam("profileName") String profileName) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (StringUtils.isNotBlank(objectId) && StringUtils.isNotBlank(profileName)) {
            try {
                profileName = URLDecoder.decode(profileName, "UTF-8");
                objectId = URLDecoder.decode(objectId, "UTF-8");
                CmisSessionWorker cmisSessionWorker = this.getSessionWorker(profileName,null);
                JsonObject rootFolder = cmisSessionWorker.getRootFolder();
                String repoRoot =  rootFolder.getJsonObject("properties").getString("objectId") ;

                //Build the json for the repository info
                builder.add("isRoot", objectId.equalsIgnoreCase(repoRoot));

            } catch (Exception e) {
                builder.add(Constants.SUCCESS, false);
                builder.add(Constants.ERRORMSG, e.getMessage());
            }

            builder.add(Constants.SUCCESS, true);

        } else {
            builder.add(Constants.SUCCESS, false);
            builder.add(Constants.ERRORMSG, "One or more parameters missing or malformed");
        }

        return builder.build();
    }


    /**
     * Returns a cmis session worker instance given a profile name
     * @param profileName
     * @return
     */
    private CmisSessionWorker getSessionWorker(String profileName, String mapName){
        String connectionKey = profileName +"_"+mapName;
        try{
            if(!connectionPool.containsKey(connectionKey))
                connectionPool.put(connectionKey, new CmisSessionWorkerImpl(profileName, mapName));
        }
        catch(Exception ge){
            logger.error("Unable to create session worker due to: " + ge.getMessage());
            throw new ErmsRuntimeException(ge.getMessage());
        }
        return connectionPool.get(connectionKey);
    }

}
