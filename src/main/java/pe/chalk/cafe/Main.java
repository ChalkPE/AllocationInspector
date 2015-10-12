package pe.chalk.cafe;

import org.json.JSONObject;
import pe.chalk.takoyaki.Takoyaki;
import pe.chalk.takoyaki.Target;
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

    public static boolean DELAY = true;

    public static final PrintStream realErr = System.err;
    public static final PrintStream fakeErr = new PrintStream(new OutputStream(){
        @Override
        public void write(int b) throws IOException{
            //DO NOTHING
        }
    });

    public static void main(String[] args) throws IOException, InterruptedException {
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

        Main.inspectors.forEach(inspector -> {
            Target target = Takoyaki.getInstance().getTarget(inspector.getClubId());
            Takoyaki.getInstance().getLogger().info("게시글을 검사합니다: 대상자 " + inspector.getAssignees().size() + "명: " + target.getName() + " (ID: " + target.getClubId() + ")");
        });

        //noinspection InfiniteLoopStatement
        while(true){
            Calendar calendar = Calendar.getInstance(Locale.KOREA);

            Main.inspectors.forEach(inspector -> inspector.inspect(calendar.getTime())); Thread.sleep(1000);
            //calendar.add(Calendar.DATE, -1); Main.inspectors.forEach(inspector -> inspector.inspect(calendar.getTime())); Thread.sleep(1000);
        }


    }

    public static boolean delay(long millis){
        if(!Main.DELAY) return false;

        try{
            Thread.sleep(millis);
            return true;
        }catch(InterruptedException e){
            e.printStackTrace();
            return false;
        }
    }
}
