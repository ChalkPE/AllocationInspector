package pe.chalk.cafe;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import pe.chalk.takoyaki.Takoyaki;
import pe.chalk.takoyaki.Target;
import pe.chalk.takoyaki.model.Member;
import pe.chalk.takoyaki.utils.TextFormat;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ChalkPE <chalkpe@gmail.com>
 * @since 2015-10-10
 */
public class AllocationInspector {
    public static final String MEMBER_RECENT_ARTICLES_URL = "http://cafe.naver.com/CafeMemberNetworkArticleList.nhn?clubid=%s&search.clubid=%s&search.writerid=%s";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd.", Locale.KOREA);

    private int clubId;
    private long allocatedArticles;
    private List<Member> assignees;

    public AllocationInspector(JSONObject properties){
        this.clubId = properties.getInt("clubId");

        Target target = Takoyaki.getInstance().getTarget(this.getClubId());
        Takoyaki.getInstance().getLogger().info("인스펙터를 생성합니다: " + target.getName() + " (ID: " + target.getClubId() + ")");

        this.allocatedArticles = properties.getLong("allocatedArticles");
        this.assignees = Takoyaki.<String>buildStream(properties.getJSONArray("assignees")).map(assignee -> {
            String[] a = assignee.split(":");
            return new Member(this.getTarget().getClubId(), a[0], a[1]);
        }).collect(Collectors.toList());

        //Takoyaki.getInstance().getLogger().debug("checking articles...");
        this.getAssignees().forEach(assignees -> {
            List<MemberArticle> articles = this.getArticlesOfToday(assignees);
            Collections.reverse(articles);

            boolean done = articles.size() >= this.getAllocatedArticles();

            Takoyaki.getInstance().getLogger().info(String.format("%s -> %s%d/%d %s", assignees.toString(), done ? TextFormat.GREEN : TextFormat.RED, articles.size(), this.getAllocatedArticles(), done ? "DONE" : "FAILED"));
            articles.forEach(article -> Takoyaki.getInstance().getLogger().info(" * " + article.toString()));
        });
    }

    public int getClubId(){
        return clubId;
    }

    public long getAllocatedArticles(){
        return this.allocatedArticles;
    }

    public List<Member> getAssignees(){
        return this.assignees;
    }

    public Target getTarget(){
        return Takoyaki.getInstance().getTarget(this.getClubId());
    }

    public Document getRecentMemberArticles(Member member) throws IOException {
        URL recentMemberArticlesUrl = new URL(String.format(AllocationInspector.MEMBER_RECENT_ARTICLES_URL, this.getClubId(), this.getClubId(), member.getId()));
        return Jsoup.parse(Main.staff.getPage(recentMemberArticlesUrl).getWebResponse().getContentAsString());
    }

    public List<MemberArticle> getArticlesOfToday(Member member){
        String today = AllocationInspector.DATE_FORMAT.format(new Date());
        try{
            return this.getRecentMemberArticles(member).select("tr[align=center]:not([class])").stream()
                    .map(element -> MemberArticle.fromElement(element, this.getClubId(), member))
                    .filter(article -> article.getUploadDate().equals(today))
                    .filter(article -> article.getMenuId() != 30)
                    .collect(Collectors.toList());

        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

}
