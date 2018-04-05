import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class FormCacheEligibilityTester {

    private static void categorizeAndPrintExpressions(String filename) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(new File(filename)));

        List<String> expressionStrings = new ArrayList<>();
        Pattern relevant = Pattern.compile("(?:relevant=)\"(.*?)(?:\")");
        Pattern calculate = Pattern.compile("(?:calculate=)\"(.*?)(?:\")");
        String line;
        while ((line = br.readLine()) != null) {
            Matcher m = relevant.matcher(line);
            while (m.find()) {
                expressionStrings.add(m.group());
            }
            m = calculate.matcher(line);
            while (m.find()) {
                expressionStrings.add(m.group());
            }
        }
        for (String s : expressionStrings) {
            System.out.println(s);
        }

    }

    public static void main(String[] args) {
        try {
            categorizeAndPrintExpressions(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}