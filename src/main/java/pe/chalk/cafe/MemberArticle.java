package pe.chalk.cafe;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pe.chalk.takoyaki.Takoyaki;
import pe.chalk.takoyaki.model.Member;
import pe.chalk.takoyaki.model.SimpleArticle;
import pe.chalk.takoyaki.utils.TextFormat;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ChalkPE <chalkpe@gmail.com>
 * @since 2015-10-10
 */
public class MemberArticle extends SimpleArticle {
    public static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy.MM.dd. HH:mm", Locale.KOREA);
    public static final Pattern MENU_ID_PATTERN = Pattern.compile("&search\\.menuid=(\\d+)&");
    public static final Map<Integer, MemberArticle> cache = new HashMap<>();

    private String uploadDate;
    private String uploadTime;
    private Member writer;
    private int menuId = 0;

    public MemberArticle(int targetId, int id, String title, int commentCount, String uploadDate, Member writer){
        super(targetId, id, title, commentCount);
        MemberArticle.cache.put(this.getId(), this);

        this.uploadDate = uploadDate;
        this.writer = writer;
    }

    public static MemberArticle fromElement(Element element, int targetId, Member writer){
        int id = Integer.parseInt(element.select("span.m-tcol-c.list-count").first().text());
        if(MemberArticle.cache.containsKey(id)){
            return MemberArticle.cache.get(id);
        }

        String title = element.select("td.board-list > span a.m-tcol-c").first().text();
        String uploadDate = element.select("td.view-count.m-tcol-c").first().text();

        Elements commentElements = element.select("td.board-list > a.m-tcol-p > span.m-tcol-p.num > strong");
        int commentCount = commentElements.isEmpty() ? 0 : Integer.parseInt(commentElements.first().text());

        return new MemberArticle(targetId, id, title, commentCount, uploadDate, writer);
    }

    public String getUploadDate(){
        return this.uploadDate;
    }

    public String getUploadTime(){
        return this.getUploadTime(true);
    }

    public String getUploadTime(boolean update){
        if((this.uploadTime == null || this.uploadTime.equals("")) && update){
            this.update();
        }
        return this.uploadTime;
    }

    public String getUploadDateAndTime(){
        return this.getUploadDateAndTime(true);
    }

    public String getUploadDateAndTime(boolean update){
        return this.getUploadDate() + " " + this.getUploadTime(update);
    }

    public Date getDate(){
        try{
            return MemberArticle.FORMAT.parse(this.getUploadDateAndTime());
        }catch(ParseException e){
            e.printStackTrace();
        }
        return null;
    }

    public Member getWriter(){
        return this.writer;
    }

    public int getMenuId(){
        return this.getMenuId(true);
    }

    public int getMenuId(boolean update){
        if(update && this.menuId == 0) this.update();
        return this.menuId;
    }

    public void update(){
        Main.delay(500);
        Takoyaki.getInstance().getLogger().info("UPDATE: " + this.toString());

        try{
            Document document = Jsoup.parse(Main.staff.getPage(new URL("http://cafe.naver.com/ArticleRead.nhn?clubid=23683173&articleid=" + this.getId())).getWebResponse().getContentAsString());

            Matcher menuIdMatcher = MemberArticle.MENU_ID_PATTERN.matcher(document.select("div.tit-box div.fl a.m-tcol-c").first().attr("href"));
            if(menuIdMatcher.find()){
                this.menuId = Integer.parseInt(menuIdMatcher.group(1));
            }

            String uploadTime = document.select("div.tit-box div.fr td.m-tcol-c.date").first().text();
            if(uploadTime.length() > 5){
                uploadTime = uploadTime.substring(uploadTime.length() - 5);
            }
            this.uploadTime = uploadTime;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public String toString(){
        return TextFormat.GREEN + "[" + this.getId() + "] " + TextFormat.RESET
                + TextFormat.LIGHT_PURPLE + "[" + this.getTarget().getMenu(this.getMenuId(false)).getName() + "] " + TextFormat.RESET
                + this.getTitle()
                + TextFormat.DARK_AQUA + " by " + this.getWriter().toString() + TextFormat.RESET
                + TextFormat.GOLD + " at " + this.getUploadDateAndTime(false) + TextFormat.RESET;
    }
}
