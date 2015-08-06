/**
 *
 */
package org.javarosa.engine;

import org.javarosa.engine.models.Mockup;
import org.javarosa.engine.models.Mockup.MockupEditor;
import org.javarosa.engine.models.Session;
import org.javarosa.engine.xml.InvalidStructureException;
import org.javarosa.engine.xml.MockupParser;
import org.javarosa.engine.xml.serializer.MockupSerializer;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author ctsims
 *
 */
public class Harness {

    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Usage: java -jar Player");
        }
        if(args.length == 1) {
            XFormPlayer xfp = new XFormPlayer(System.in, System.out, null);
            //XFormPlayer xfp = new XFormPlayer(System.console(), System.out);
            try {
                xfp.start(args[0]);
                Session s = xfp.environment.getSessionRecording();
            } catch (FileNotFoundException e) {
                System.out.println("File not found at " + args[0]+ "!!!!");
            }
        } else {

            //Setup:
            try {
                if(args[0].equals("add")) {
                    Mockup m = getMockup(args[1]);

                    XFormPlayer xfp = new XFormPlayer(System.in, System.out, m);
                    xfp.start(args[2]);

                    Session s = xfp.environment.getSessionRecording();
                    MockupEditor e = m.getEditor();
                    e.addSession(s);
                    e.commit();

                    updateMockup(args[1], m);
                }

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }

    private static void updateMockup(String file, Mockup m) throws IOException {
        File dest = new File(file);
        File temp = File.createTempFile(dest.getName(), "tmp");

        FileOutputStream fos = new FileOutputStream(temp);
        new MockupSerializer(fos, m).serialize();
        try {
            fos.close();
        } catch(IOException e){

        }

        boolean atomicSuccess = temp.renameTo(dest);


        //*sigh*, ok, so on Windows, you can't do this atomically (read: Correctly),
        //so we have to do some terrible voodoo.

        if(!atomicSuccess) {
            File oldFile = new File(dest.getCanonicalFile() + ".bak");
            int count = 0;
            while(oldFile.exists()) {
                oldFile = new File(dest.getCanonicalFile() + ".bak." + count);
                count++;
            }

            if(!dest.renameTo(oldFile)) {
                throw new IOException("Can't save file at " + oldFile.getAbsolutePath());
            }

            if(!temp.renameTo(dest)) {
                if(!oldFile.renameTo(dest)) {
                    throw new IOException("Windows sucks and won't let us save the file! Your old file is safe, though, at: " + oldFile.getAbsolutePath());
                }
            } else {
                oldFile.deleteOnExit();
            }
        }
    }

    private static Mockup getMockup(String arg) {
        try {
            FileInputStream fis = new FileInputStream(arg);
            Mockup m = new MockupParser(fis).parse();
            try {
                fis.close();
            } catch(IOException e) {

            }
            return m;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found at " + arg + "!!!!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidStructureException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid Mockup file: " + e.getMessage());
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid Mockup file: " + e.getMessage());
        }
    }

}
