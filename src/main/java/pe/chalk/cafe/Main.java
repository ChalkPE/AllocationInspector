package pe.chalk.cafe;

import org.json.JSONObject;
import pe.chalk.takoyaki.Takoyaki;
import pe.chalk.test.Staff;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author ChalkPE <chalkpe@gmail.com>
 * @since 2015-10-10
 */
public class Main {
    public static Staff staff;
    public static Takoyaki takoyaki;
    public static List<AllocationInspector> inspectors;

    public static final PrintStream realErr = System.err;
    public static final PrintStream fakeErr = new PrintStream(new OutputStream(){
        @Override
        public void write(int b) throws IOException{
            //DO NOTHING
        }
    });

    public static void main(String[] args) throws IOException {
        Main.takoyaki = new Takoyaki();
        Main.inspectors = new ArrayList<>();
        try{
            Properties accountProperties = new Properties();
            accountProperties.load(new FileInputStream("account.properties"));
            Takoyaki.getInstance().getLogger().info("네이버에 로그인합니다: " + accountProperties.getProperty("user.id"));

            System.setErr(Main.fakeErr);
            Main.staff = new Staff(null, accountProperties);
            System.setErr(Main.realErr);
        }catch(IllegalStateException e){
            Takoyaki.getInstance().getLogger().critical("네이버에 로그인할 수 없습니다!");
            return;
        }
        Main.staff.getOptions().setJavaScriptEnabled(false);

        Path propertiesPath = Paths.get("AllocationInspector.json");
        if(Files.notExists(propertiesPath)){
            return;
        }

        JSONObject properties = new JSONObject(new String(Files.readAllBytes(propertiesPath), StandardCharsets.UTF_8));
        Main.inspectors.addAll(Takoyaki.<JSONObject>buildStream(properties.getJSONArray("targets")).map(AllocationInspector::new).collect(Collectors.toList()));

        Calendar calendar = Calendar.getInstance(Locale.KOREA);
        Date today = calendar.getTime();

        //calendar.add(Calendar.DATE, -1);
        //Date yesterday = calendar.getTime();

        //noinspection InfiniteLoopStatement
        while(true) Main.inspectors.forEach(inspector -> inspector.inspect(today));

        //Main.inspectors.forEach(inspector -> inspector.inspect(yesterday));
    }
}
