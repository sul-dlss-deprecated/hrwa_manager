package edu.columbia.ldpd.hrwa.marc.translation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;


public class Translations {
    public static HashMap<String, String> COUNTRY = loadTranslation("country_code.txt",374);
    public static HashMap<String, String> GAC = loadTranslation("gac_code.txt",583);
    public static HashMap<String, String> LANGUAGE = loadTranslation("language_code.txt",517);

    private static HashMap<String, String> loadTranslation(String file, int lines){
        HashMap<String,String> result = new HashMap<String, String>(lines);
        InputStream is = Translations.class.getResourceAsStream(file);
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is,"UTF-8"));
            String line = null;
            while ((line = r.readLine()) != null){
                String [] parts = line.split("\\|");
                if (parts.length < 2) System.err.println(line);
                result.put(parts[0], parts[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}
