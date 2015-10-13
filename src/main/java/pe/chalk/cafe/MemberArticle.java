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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ChalkPE <chalkpe@gmail.com>
 * @since 2015-10-10
 */
public class MemberArticle extends SimpleArticle {
    public static final Pattern MENU_ID_PATTERN = Pattern.compile("&search\\.menuid=(\\d+)&");
    public static final Map<Integer, MemberArticle> cache = new HashMap<>();

    private String uploadDate;
    private String uploadDateAndTime = null;
    private Member writer;
    private int menuId = 0;

    public MemberArticle(int targetId, int id, String title, int commentCount, String uploadDate, Member writer){
        super(targetId, id, title, commentCount);
        MemberArticle.cache.put(this.getId(), this);

        this.uploadDate = uploadDate;
        this.writer = writer;

        Takoyaki.getInstance().getLogger().debug("NEW:   " + this.toString());
    }

    public static MemberArticle fromElement(Element element, int targetId, Member writer){
        int id = Integer.parseInt(element.select("span.m-tcol-c.list-count").first().text());
        if(MemberArticle.cache.containsKey(id)){
            MemberArticle article = MemberArticle.cache.get(id);
            Takoyaki.getInstance().getLogger().debug("CACHE: " + article.toString());

            return article;
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

    public String getUploadDateAndTime(){
        return this.getUploadDateAndTime(true);
    }

    public String getUploadDateAndTime(boolean update){
        if(this.uploadDateAndTime == null){
            if(!update) return this.getUploadDate();
            this.update();
        }
        return this.uploadDateAndTime;
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

        try{
            Document document = Jsoup.parse(Main.staff.getPage(new URL("http://cafe.naver.com/ArticleRead.nhn?clubid=23683173&articleid=" + this.getId())).getWebResponse().getContentAsString());

            Matcher menuIdMatcher = MemberArticle.MENU_ID_PATTERN.matcher(document.select("div.tit-box div.fl td.m-tcol-c a").attr("href"));
            if(menuIdMatcher.find()){
                this.menuId = Integer.parseInt(menuIdMatcher.group(1));
            }

            this.uploadDateAndTime = document.select("div.tit-box div.fr td.m-tcol-c.date").text();
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
