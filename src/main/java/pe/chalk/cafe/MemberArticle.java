package pe.chalk.cafe;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pe.chalk.takoyaki.Takoyaki;
import pe.chalk.takoyaki.model.Member;
import pe.chalk.takoyaki.model.SimpleArticle;
import pe.chalk.takoyaki.utils.TextFormat;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ChalkPE <chalkpe@gmail.com>
 * @since 2015-10-10
 */
public class MemberArticle extends SimpleArticle {
    public static final Pattern MENU_ID_PATTERN = Pattern.compile("&search\\.menuid=(\\d+)&");

    private String uploadDate;
    private Member writer;
    private int menuId = 0;

    public MemberArticle(int targetId, int id, String title, int commentCount, String uploadDate, Member writer){
        super(targetId, id, title, commentCount);

        this.uploadDate = uploadDate;
        this.writer = writer;
    }

    public static MemberArticle fromElement(Element element, int targetId, Member writer){
        //Takoyaki.getInstance().getLogger().debug("creating MemberArticle.fromElement...");

        int id = Integer.parseInt(element.select("span.m-tcol-c.list-count").first().text());
        String title = element.select("td.board-list > span a.m-tcol-c").first().text();
        String uploadDate = element.select("td.view-count.m-tcol-c").first().text();

        Elements commentElements = element.select("td.board-list > a.m-tcol-p > span.m-tcol-p.num > strong");
        int commentCount = commentElements.isEmpty() ? 0 : Integer.parseInt(commentElements.first().text());

        return new MemberArticle(targetId, id, title, commentCount, uploadDate, writer);
    }

    public String getUploadDate(){
        return this.uploadDate;
    }

    public Member getWriter(){
        return this.writer;
    }

    public int getMenuId(){
        return this.getMenuId(true);
    }

    public int getMenuId(boolean update){
        if(update && this.menuId == 0){
            try{
                //Takoyaki.getInstance().getLogger().debug(this.toString() + ": getting menuId...");

                URL articleUrl = new URL("http://cafe.naver.com/ArticleRead.nhn?clubid=23683173&articleid=" + this.getId());
                String href = Jsoup.parse(Main.staff.getPage(articleUrl).getWebResponse().getContentAsString()).select("div.tit-box div.fl td.m-tcol-c a").attr("href");

                Matcher menuIdMatcher = MemberArticle.MENU_ID_PATTERN.matcher(href);
                if(menuIdMatcher.find()){
                    this.menuId = Integer.parseInt(menuIdMatcher.group(1));
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        return this.menuId;
    }

    @Override
    public String toString(){
        return TextFormat.GREEN + "[" + this.getId() + "] " + TextFormat.RESET
                + TextFormat.LIGHT_PURPLE + "[" + this.getTarget().getMenu(this.getMenuId(false)).getName() + "] " + TextFormat.RESET
                + this.getTitle()
                + TextFormat.DARK_AQUA + " by " + this.getWriter().toString() + TextFormat.RESET
                + TextFormat.GOLD + " at " + this.getUploadDate() + TextFormat.RESET;
    }
}
