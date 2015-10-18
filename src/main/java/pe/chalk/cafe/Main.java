package pe.chalk.cafe;

import org.json.JSONObject;
import pe.chalk.takoyaki.Takoyaki;
import pe.chalk.takoyaki.Target;
import pe.chalk.takoyaki.utils.TextFormat;
import pe.chalk.test.Staff;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author ChalkPE <chalkpe@gmail.com>
 * @since 2015-10-10
 */
public class Main {
    public static Staff staff;
    public static Takoyaki takoyaki;
    public static List<AllocationInspector> inspectors;

    public static boolean DELAY = true;

    public static String html;
    public static Path htmlOutput;

    public static int days, midnightHour;

    public static void main(String[] args) throws IOException, InterruptedException {
        Main.takoyaki = new Takoyaki();

        try{
            Properties accountProperties = new Properties(); accountProperties.load(new FileInputStream("account.properties"));
            Takoyaki.getInstance().getLogger().info("네이버에 로그인합니다: " + accountProperties.getProperty("user.id"));

            Main.staff = new Staff(null, accountProperties);
            Main.staff.getOptions().setJavaScriptEnabled(false);

            Runtime.getRuntime().addShutdownHook(new Thread(Main.staff::close));
        }catch(IllegalStateException e){
            Takoyaki.getInstance().getLogger().error("네이버에 로그인할 수 없습니다!");
            return;
        }

        Path propertiesPath = Paths.get("AllocationInspector.json");
        if(Files.notExists(propertiesPath)){
            Takoyaki.getInstance().getLogger().error("프로퍼티 파일을 찾을 수 없습니다: " + propertiesPath);
            return;
        }

        JSONObject properties = new JSONObject(new String(Files.readAllBytes(propertiesPath), StandardCharsets.UTF_8));
        Main.days = properties.getInt("days");
        Main.midnightHour = properties.getInt("midnightHour");

        try{
            Main.html = new String(Files.readAllBytes(Paths.get(properties.getString("htmlInput"))), StandardCharsets.UTF_8);
            Main.htmlOutput = Paths.get(properties.getString("htmlOutput"));
        }catch(IOException e){
            e.printStackTrace();
        }

        Main.inspectors = Takoyaki.<JSONObject>buildStream(properties.getJSONArray("targets")).map(AllocationInspector::new).collect(Collectors.toList());
        Main.inspectors.forEach(inspector -> {
            Target target = Takoyaki.getInstance().getTarget(inspector.getClubId());
            Takoyaki.getInstance().getLogger().info("게시글을 검사합니다: 대상자 " + inspector.getAssignees().size() + "명: " + target.getName() + " (ID: " + target.getClubId() + ")");
        });

        new Thread(() -> {
            try{
                Thread.sleep(TimeUnit.HOURS.toMillis(3));

                MemberArticle.cache.clear();
                Takoyaki.getInstance().getLogger().notice("CACHE CLEARED!");
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }).start();

        //noinspection InfiniteLoopStatement
        while(true){
            try{
                final Calendar calendar = Calendar.getInstance(Locale.KOREA);
                calendar.add(Calendar.DATE, 1 - Main.days);

                Main.inspect(IntStream.range(0, Main.days).mapToObj(i -> {
                    Date date = calendar.getTime();
                    calendar.add(Calendar.DATE, 1);

                    return date;
                }));
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void inspect(Stream<Date> dates) throws InterruptedException {
        List<String> messages = dates.flatMap(date -> Main.inspectors.stream().map(inspector -> inspector.inspect(date))).collect(Collectors.toList());
        Collections.reverse(messages);

        String result = String.join(String.format("%n%n"), messages);
        AllocationInspector.cache.clear();

        Takoyaki.getInstance().getLogger().info(result);
        Main.html(result);
    }

    public static void html(String messages){
        try{
            Files.write(Main.htmlOutput, String.format(Main.html, TextFormat.replaceTo(TextFormat.Type.HTML, messages)).getBytes(StandardCharsets.UTF_8));
        }catch(Exception e){
            e.printStackTrace();
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
