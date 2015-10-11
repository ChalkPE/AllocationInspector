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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ChalkPE <chalkpe@gmail.com>
 * @since 2015-10-10
 */
public class AllocationInspector {
    public static final String MEMBER_RECENT_ARTICLES_URL = "http://cafe.naver.com/CafeMemberNetworkArticleList.nhn?clubid=%s&search.clubid=%s&search.writerid=%s";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd.", Locale.KOREA);
    public static final SimpleDateFormat KOREAN_DATE_FORMAT = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);

    private int clubId;
    private long allocatedArticles;
    private List<Member> assignees;

    public AllocationInspector(JSONObject properties){
        this.clubId = properties.getInt("clubId");

        Target target = Takoyaki.getInstance().getTarget(this.getClubId());
        Takoyaki.getInstance().getLogger().info("게시글을 검사합니다: " + target.getName() + " (ID: " + target.getClubId() + ")");

        this.allocatedArticles = properties.getLong("allocatedArticles");
        this.assignees = Takoyaki.<String>buildStream(properties.getJSONArray("assignees")).map(assignee -> {
            String[] a = assignee.split(":");
            return new Member(this.getTarget().getClubId(), a[0], a[1]);
        }).collect(Collectors.toList());
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

    public List<MemberArticle> getArticles(Member member, Date date){
        String today = AllocationInspector.DATE_FORMAT.format(date);

        try{
            return this.getRecentMemberArticles(member).select("tr[align=center]:not([class])").stream()
                    .map(element -> MemberArticle.fromElement(element, this.getClubId(), member))
                    .filter(article -> article.getUploadDate().equals(today))
                    .filter(article -> article.getMenuId() != 30)
                    .sorted((a, b) -> a.getId() - b.getId())
                    .collect(Collectors.toList());
        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public void inspect(Date date){
        Takoyaki.getInstance().getLogger().info(TextFormat.BOLD.toString() + TextFormat.BLUE + "[" + AllocationInspector.KOREAN_DATE_FORMAT.format(date) + " 게시글 할당제 달성 여부]");

        Comparator<Map.Entry<Member, List<MemberArticle>>> comparator = Comparator.comparing(entry -> -entry.getValue().size());
        comparator = comparator.thenComparing(entry -> entry.getValue().size() <= 0 ? Integer.MAX_VALUE : entry.getValue().get(entry.getValue().size() >= this.getAllocatedArticles() ? (int) this.getAllocatedArticles() - 1 : 0).getId());
        comparator = comparator.thenComparing(entry -> entry.getKey().toString());

        this.getAssignees().stream().collect(Collectors.toMap(assignee -> assignee, assignee -> this.getArticles(assignee, date))).entrySet().stream().sorted(comparator).forEach(entry -> {
            int count = entry.getValue().size();
            boolean done = count >= this.getAllocatedArticles();

            String last = count <= 0 ? "     " : (done ? entry.getValue().get((int) this.getAllocatedArticles() - 1) : entry.getValue().get(0)).getUploadDateAndTime().substring(12);
            TextFormat format = count <= 0 ? TextFormat.DARK_RED : (done ? (count == this.getAllocatedArticles() ? TextFormat.GREEN : TextFormat.AQUA) : (count >= this.getAllocatedArticles() / 2.0 ? TextFormat.YELLOW : TextFormat.RED));

            Takoyaki.getInstance().getLogger().info(String.format("%s%2d/%d %s %s %s", format, count, this.getAllocatedArticles(), done ? "SUCCESS" : "FAILURE", last, entry.getKey()));
        });
    }
}
