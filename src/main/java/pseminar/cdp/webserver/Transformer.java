package pseminar.cdp.webserver;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Arrays;
import java.util.LinkedList;

@SuppressWarnings("unchecked call to")
class Transformer {

    private JSONParser parser = new JSONParser();
    private JSONArray items = new JSONArray();
    private String path = "";

    JSONArray transformForFrontend(JSONObject rawJson) throws ParseException {

        JSONObject parsedJson = (JSONObject) parser.parse(rawJson.toJSONString());
        JSONArray fileList = (JSONArray) parsedJson.get("file_list");

        System.out.println("Transform the Response Object from List to Tree to be usable for Frontend");
        for (Object o : fileList) {

            JSONObject jObj = (JSONObject) o;

            //remove not needed keys to save some ram
            jObj.remove("hash_list");
            jObj.remove("atime");
            jObj.remove("ctime");
            jObj.remove("owner");
            jObj.remove("group");
            jObj.remove("uid");
            jObj.remove("gid");
            jObj.remove("hostname");

            String filename = (String) jObj.get("name");
            LinkedList<String> pathList = new LinkedList<>(Arrays.asList(filename.split("/")));
            pathList.removeFirst();

            //transform from file list to file tree directory object
            transformRecursive(jObj, items, pathList);
        }
        //System.out.println(items);
        return items;
    }

    private void transformRecursive(JSONObject serverObject, JSONArray items, LinkedList<String> pathList) {

        boolean exists = false;
        long fileType = (long) serverObject.get("filetype");
        String filename = pathList.removeFirst();
        path = path + "/" + filename;

        for (Object o : items) {

            JSONObject jObj = (JSONObject) o;

            if (filename.equals(jObj.get("name"))) {
                exists = true;

                if (pathList.isEmpty() && fileType != 2) {

                    //File does already Exist, so its a new Version!
                    System.out.println("Add Version to File " + filename + " at " + path);
                    ((JSONArray) jObj.get("versions")).add(generateVersionObject(serverObject));
                    //System.out.println(serverObject);

                } else {
                    //System.out.println("Dir " + path + " does already Exist!");
                }
                if (!pathList.isEmpty()) {
                    transformRecursive(serverObject, (JSONArray) jObj.get("items"), pathList);
                }
                break;
            }
        }

        if (!exists) {

            //create Directory and Continue if Path is not empty
            if (fileType == 2 || (!pathList.isEmpty() && fileType != 2)) {

                items.add(0, generateDirObject(serverObject, filename, path));
                //System.out.println("Dir " + path + " does not Exist, created!");

                if (!pathList.isEmpty()) {
                    transformRecursive(serverObject, (JSONArray) ((JSONObject) items.get(0)).get("items"), pathList);
                }
                //create File
            } else {
                System.out.println("Add File " + filename + " at " + path);
                items.add(generateFileObject(serverObject, filename, path));
            }
        }
        path = "";
    }

    private JSONObject generateDirObject(JSONObject serverObject, String filename, String path) {
        JSONObject jObj = new JSONObject();

        jObj.put("name", filename);
        jObj.put("type", "folder");
        jObj.put("items", new JSONArray());
        jObj.put("path", path);
        jObj.put("mtime", serverObject.get("mtime"));
        jObj.put("fsize", serverObject.get("fsize"));

        return jObj;
    }

    private JSONObject generateFileObject(JSONObject serverObject, String filename, String path) {
        JSONObject jObj = new JSONObject();

        jObj.put("name", filename);
        jObj.put("type", "file");
        jObj.put("path", path);
        JSONArray vArr = new JSONArray();
        vArr.add(generateVersionObject(serverObject));
        jObj.put("versions", vArr);

        return jObj;
    }

    private JSONObject generateVersionObject(JSONObject serverObject) {
        JSONObject jObj = new JSONObject();

        jObj.put("mtime", serverObject.get("mtime"));
        jObj.put("fsize", serverObject.get("fsize"));

        return jObj;
    }
}