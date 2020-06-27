package za.ac.sun.cs.localizer.dynamic;

import java.io.*;
import java.util.*;

public class Logger {
    static Map<String, ArrayList<String>> failMap = new HashMap<String, ArrayList<String>>();
    static Map<String, ArrayList<String>> passMap = new HashMap<String, ArrayList<String>>();

     /* Failed tests */
    public static void  trackFailed(String testcase, ArrayList<String> trace) {
        failMap.put(testcase, trace);
    }
    /* passed tests*/
    public static void trackPassed(String testcase, ArrayList<String> trace) {
        passMap.put(testcase, trace);
    }

    /* spit out some json */
    public static void spitJson() {
        /* failed tests */
        BufferedWriter writer = null;
        /* store keys of map */
        ArrayList<String> keys = new ArrayList<String>(failMap.keySet());
        try {
            writer = new BufferedWriter(new FileWriter("fail.json"));
            /* traverse list */
            writer.append("{\n");
            for (int i=0; i<keys.size(); i++) {

                writer.append("  \""+keys.get(i)+"\" : [");
                /* get traces */
                ArrayList<String> traces  = failMap.get(keys.get(i));
                /* traverse traces */
                for (int k=0; k<traces.size();k++) {
                    writer.append("\""+traces.get(k)+"\"");
                    if (k == traces.size()-1) {
                        writer.append("]");
                    } else {
                        writer.append(", ");
                    }
                }
                if (i != keys.size()-1) {
                    writer.append(",\n");
                } else {
                    writer.append("\n");
                }
            }
            writer.append("}\n");
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /* passed tests */
        keys = new ArrayList<String>(passMap.keySet());
        try {
            writer = new BufferedWriter(new FileWriter("pass.json"));
            writer.append("{\n");
            for (int i = 0; i<keys.size(); i++) {
                /* spit node */
                writer.append("  \""+keys.get(i)+"\" : [");
                /* get traces */
                ArrayList<String> traces = passMap.get(keys.get(i));
                /* traverse traces */
                for(int k=0; k<traces.size(); k++) {
                    writer.append("\""+traces.get(k)+"\"");
                    if (k == traces.size()-1) {
                        writer.append("]");
                    } else {
                        writer.append(", ");
                    }
                }
                /* if not end */
                if (i != keys.size()-1) {
                    writer.append(",");
                }
                writer.append("\n");
            }
            writer.append("}");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}

