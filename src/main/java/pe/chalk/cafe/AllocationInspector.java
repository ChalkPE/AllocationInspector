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
    private int allocatedArticles;
    private List<Member> assignees;

    public AllocationInspector(JSONObject properties){
        this.clubId = properties.getInt("clubId");
        this.allocatedArticles = properties.getInt("allocatedArticles");
        this.assignees = Takoyaki.<String>buildStream(properties.getJSONArray("assignees")).map(assignee -> {
            String[] a = assignee.split(":");
            return new Member(this.getTarget().getClubId(), a[0], a[1]);
        }).collect(Collectors.toList());
    }

    public int getClubId(){
        return clubId;
    }

    public int getAllocatedArticles(){
        return this.allocatedArticles;
    }

    public List<Member> getAssignees(){
        return this.assignees;
    }

    public Target getTarget(){
        return Takoyaki.getInstance().getTarget(this.getClubId());
    }

    public Document getRecentMemberArticles(Member member) throws IOException {
        try{Thread.sleep(50);}catch(InterruptedException e){e.printStackTrace();}

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
        long time = System.currentTimeMillis();
        int totalAssignees = this.getAssignees().size();

        Takoyaki.getInstance().getLogger().info(TextFormat.BOLD.toString() + TextFormat.BLUE + "[" + AllocationInspector.KOREAN_DATE_FORMAT.format(date) + "]");
        List<Result> results = this.getAssignees().stream().map(assignee -> new Result(assignee, this.getArticles(assignee, date), this.getAllocatedArticles())).sorted().collect(Collectors.toList());

        long aliveAssignees     = results.stream().filter(Result::isAlive).count();
        long succeededAssignees = results.stream().filter(Result::isSucceeded).count();
        long totalArticles      = results.stream().mapToLong(Result::size).sum();

        double average             =      totalArticles * 1.0 / totalAssignees;
        double alivePercentage     =     aliveAssignees * 1.0 / totalAssignees;
        double succeededPercentage = succeededAssignees * 1.0 / totalAssignees;

        Map<Integer, Long> map = results.stream().collect(Collectors.groupingBy(Result::size, Collectors.counting()));
        double standardDeviation = Math.sqrt(map.entrySet().stream().mapToDouble(entry -> Math.pow(entry.getKey() - average, 2) * entry.getValue()).sum() / totalAssignees);

        //TODO: Print results with rank

        time = System.currentTimeMillis() - time;

        String delimiter = TextFormat.RESET.toString() + TextFormat.DARK_BLUE + "| " + TextFormat.BLUE;

        Takoyaki.getInstance().getLogger().info(String.format("%s참여자: %s%4d명 %s달성자: %s%4d명 %s총 게시글 수: %s%4d개 %s소요시간: %s%5.2f초 %s",
                delimiter, TextFormat.BOLD, aliveAssignees,
                delimiter, TextFormat.BOLD, succeededAssignees,
                delimiter, TextFormat.BOLD, totalArticles,
                delimiter, TextFormat.BOLD, time / 1000.0,
                delimiter));

        Takoyaki.getInstance().getLogger().info(String.format("%s참여율:　%s%4.1f%% %s달성률:　%s%4.1f%% %s1인당 평균:　%s%5.2f개 %s표준편차: %s%5.2f개 %s%n",
                delimiter, TextFormat.BOLD, alivePercentage * 100,
                delimiter, TextFormat.BOLD, succeededPercentage * 100,
                delimiter, TextFormat.BOLD, average,
                delimiter, TextFormat.BOLD, standardDeviation,
                delimiter));
    }

    @SuppressWarnings("unused")
    public class Counter {
        public long value;

        public Counter(){
            this(0L);
        }

        public Counter(long value){
            this.value = value;
        }

        public long getValue(){
            return this.value;
        }

        public Counter setValue(long value){
            this.value = value;

            return this;
        }

        public Counter add(long value){
            this.value += value;

            return this;
        }

        public Counter subtract(long value){
            this.value -= value;

            return this;
        }

        public Counter increase(){
            this.value++;

            return this;
        }

        public Counter decrease(){
            this.value--;

            return this;
        }
    }
}
