package pseminar.cdp.webserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

import static pseminar.cdp.webserver.Methods.unixTimeToDate;
import static pseminar.cdp.webserver.WebserverApplication.DEBUG_ENTROPY;

class Entropies {

    void calculateEntropyLudwig_alt_nur_wie_of_zeichen_drin_ist(File csvFile, File restoredFile, String filePath, long mtime) throws IOException {
        BufferedWriter bw = null;
        FileWriter fw = null;

        float divergence = 0;
        byte[] byteArray = Files.readAllBytes(restoredFile.toPath());
        //Frequency
        ArrayList<Double> freqList = new ArrayList<Double>();
        for (int b = 0; b < 128; b++) {
            double ctr = 0;
            for (double bit : byteArray) {
                if (bit == b) {
                    ctr++;
                }
            }
            freqList.add(ctr / byteArray.length);
        }
        //Entropy
        double ent = 0.0;
        String csvLine = "";
        for (double freq : freqList) {
            if (DEBUG_ENTROPY) System.out.print(freq + ";");
            csvLine = csvLine + freq + ';';
            if (freq > 0) {
                ent = ent + freq * (Math.log(freq) / Math.log(2));
            }
        }
        ent = -ent;

        if (DEBUG_ENTROPY) {
            System.out.println("Shannon entropy (min bits per byte-character):");
            System.out.println(ent);
            System.out.println("Min possible file size assuming max theoretical compression efficiency:");
            System.out.println(ent * byteArray.length + " in bits");
            System.out.println((ent * byteArray.length) / 8 + "in bytes");
        }

        //write calculated Entropy in CSV-File
        try {

            String data = filePath + ';' + unixTimeToDate(mtime, false) + ';' + csvLine + "\n";
            fw = new FileWriter(csvFile.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write(data);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        restoredFile.delete();
    }






    void calculateEntropyLudwig(File csvFile, File restoredFile, String filePath, long mtime) throws IOException {
        BufferedWriter bw = null;
        FileWriter fw = null;

        //float divergence = 0;
        //byte[] byteArray = Files.readAllBytes(restoredFile.toPath());
        //Frequency
        //ArrayList<Double> freqList = new ArrayList<Double>();
        //for (int b = 0; b < 128; b++) {
        //    double ctr = 0;
        //    for (double bit : byteArray) {
        //        if (bit == b) {
        //            ctr++;
        //        }
        //    }
        //    freqList.add(ctr / byteArray.length);
        //}
        //Entropy
        //double ent = 0.0;
        String csvLine = "";

        String commandexternalcall = "python /home/pseminar/entropie/entropie.py -s 32 -b -m local " + restoredFile.toPath();

        String s = null;

        try {

            // run the Unix "ps -ef" command
            // using the Runtime exec method:

            Process p = Runtime.getRuntime().exec( commandexternalcall );

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
                csvLine = s;
            }


            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

            //System.exit(0);
        }
        catch (IOException e) {
            System.out.println("exception happened - here's what I know: ");
            e.printStackTrace();
            System.exit(-1);
        }













        // for (double freq : freqList) {
        //    if (DEBUG_ENTROPY) System.out.print(freq + ";");
        //    csvLine = csvLine + freq + ';';
        //    if (freq > 0) {
        //        ent = ent + freq * (Math.log(freq) / Math.log(2));
        //    }
        //}
        //ent = -ent;

        // call external python tool to get entropy values

        if (DEBUG_ENTROPY) {
            System.out.println("XXX");
            //System.out.println(ent);
            //System.out.println("Min possible file size assuming max theoretical compression efficiency:");
            //System.out.println(ent * byteArray.length + " in bits");
            //System.out.println((ent * byteArray.length) / 8 + "in bytes");
        }

        //write calculated Entropy from python in CSV-File
        try {

            String data = filePath + ';' + unixTimeToDate(mtime, false) + ';' +  restoredFile.length()     + ';'      + csvLine + "\n";
            fw = new FileWriter(csvFile.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write(data);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        restoredFile.delete();
    }










}
